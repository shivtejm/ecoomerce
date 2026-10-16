package com.example.RegisterPage.service;

import com.example.RegisterPage.dto.CreateOrderRequest;
import com.example.RegisterPage.dto.OrderRecordDTO;
import com.example.RegisterPage.dto.OrderResponse;
import com.example.RegisterPage.dto.PaymentVerificationRequest;
import com.example.RegisterPage.entity.OrderRecord;
import com.example.RegisterPage.entity.Product;
import com.example.RegisterPage.entity.User;
import com.example.RegisterPage.repository.OrderRecordRepository;
import com.example.RegisterPage.repository.ProductRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    private final ProductRepository productRepository;
    private final OrderRecordRepository orderRecordRepository;

    public PaymentService(ProductRepository productRepository,
                          OrderRecordRepository orderRecordRepository) {
        this.productRepository = productRepository;
        this.orderRecordRepository = orderRecordRepository;
    }

    @Transactional
    public OrderResponse createRazorpayOrder(CreateOrderRequest request, User user) throws Exception {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + request.getProductId()));

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new RuntimeException("Requested quantity exceeds available stock (" + product.getStockQuantity() + " remaining)");
        }

        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        // Razorpay expects the amount in the smallest currency sub-unit (paise for INR: 1 INR = 100 paise)
        long amountInPaise = totalAmount.multiply(BigDecimal.valueOf(100)).longValue();

        RazorpayClient client = new RazorpayClient(keyId, keySecret);
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

        Order razorpayOrder = client.orders.create(orderRequest);
        String razorpayOrderId = razorpayOrder.get("id");

        // Save pre-order record
        OrderRecord orderRecord = new OrderRecord(
                razorpayOrderId,
                totalAmount,
                "INR",
                "CREATED",
                user,
                product,
                request.getQuantity()
        );
        orderRecordRepository.save(orderRecord);

        log.info("Created Razorpay Order: {} for user: {} with amount: ₹{}", razorpayOrderId, user.getEmail(), totalAmount);

        return new OrderResponse(
                razorpayOrderId,
                totalAmount,
                "INR",
                keyId,
                product.getName(),
                request.getQuantity()
        );
    }

    @Transactional
    public OrderRecordDTO verifyAndCompletePayment(PaymentVerificationRequest request, User user) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean isSignatureValid = Utils.verifyPaymentSignature(options, keySecret);

            if (!isSignatureValid) {
                log.error("Invalid payment signature for order: {}", request.getRazorpayOrderId());
                throw new RuntimeException("Payment signature verification failed");
            }

            OrderRecord orderRecord = orderRecordRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                    .orElseGet(() -> {
                        Product p = productRepository.findById(request.getProductId()).orElseThrow();
                        BigDecimal total = p.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
                        return new OrderRecord(request.getRazorpayOrderId(), total, "INR", "PAID", user, p, request.getQuantity());
                    });

            orderRecord.setRazorpayPaymentId(request.getRazorpayPaymentId());
            orderRecord.setStatus("PAID");
            orderRecordRepository.save(orderRecord);

            // Deduct stock quantity
            Product product = orderRecord.getProduct();
            if (product != null) {
                int newStock = Math.max(0, product.getStockQuantity() - request.getQuantity());
                product.setStockQuantity(newStock);
                productRepository.save(product);
            }

            log.info("Payment SUCCESS verified for Order: {}, Payment ID: {}", request.getRazorpayOrderId(), request.getRazorpayPaymentId());

            return new OrderRecordDTO(orderRecord);
        } catch (Exception e) {
            log.error("Failed to verify payment: {}", e.getMessage(), e);
            throw new RuntimeException("Payment verification error: " + e.getMessage());
        }
    }

    @Transactional
    public OrderResponse createCartRazorpayOrder(com.example.RegisterPage.dto.CreateCartOrderRequest request, User user) throws Exception {
        BigDecimal cartTotal = BigDecimal.ZERO;
        StringBuilder productNames = new StringBuilder();

        for (com.example.RegisterPage.dto.CartItemDTO item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found with ID: " + item.getProductId()));

            if (product.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock for \"" + product.getName() + "\" (" + product.getStockQuantity() + " remaining)");
            }

            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            cartTotal = cartTotal.add(itemTotal);

            if (productNames.length() > 0) productNames.append(", ");
            productNames.append(product.getName()).append(" (x").append(item.getQuantity()).append(")");
        }

        long amountInPaise = cartTotal.multiply(BigDecimal.valueOf(100)).longValue();

        RazorpayClient client = new RazorpayClient(keyId, keySecret);
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "cart_" + System.currentTimeMillis());

        Order razorpayOrder = client.orders.create(orderRequest);
        String razorpayOrderId = razorpayOrder.get("id");

        // Save pre-order records for each cart item
        for (com.example.RegisterPage.dto.CartItemDTO item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId()).orElseThrow();
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

            OrderRecord record = new OrderRecord(
                    razorpayOrderId,
                    itemTotal,
                    "INR",
                    "CREATED",
                    user,
                    product,
                    item.getQuantity()
            );
            orderRecordRepository.save(record);
        }

        log.info("Created Cart Razorpay Order: {} for user: {} with amount: ₹{}", razorpayOrderId, user.getEmail(), cartTotal);

        return new OrderResponse(
                razorpayOrderId,
                cartTotal,
                "INR",
                keyId,
                productNames.toString(),
                request.getItems().size()
        );
    }

    @Transactional
    public List<OrderRecordDTO> verifyAndCompleteCartPayment(com.example.RegisterPage.dto.VerifyCartPaymentRequest request, User user) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean isSignatureValid = Utils.verifyPaymentSignature(options, keySecret);

            if (!isSignatureValid) {
                log.error("Invalid payment signature for cart order: {}", request.getRazorpayOrderId());
                throw new RuntimeException("Payment signature verification failed");
            }

            List<OrderRecord> existingRecords = orderRecordRepository.findByUserOrderByCreatedAtDesc(user)
                    .stream()
                    .filter(o -> request.getRazorpayOrderId().equals(o.getRazorpayOrderId()))
                    .collect(Collectors.toList());

            List<OrderRecordDTO> completedOrders = new java.util.ArrayList<>();

            for (com.example.RegisterPage.dto.CartItemDTO item : request.getItems()) {
                Product product = productRepository.findById(item.getProductId()).orElseThrow();

                // Deduct stock
                int newStock = Math.max(0, product.getStockQuantity() - item.getQuantity());
                product.setStockQuantity(newStock);
                productRepository.save(product);

                // Find matching pre-order or create
                OrderRecord record = existingRecords.stream()
                        .filter(r -> r.getProduct().getId().equals(item.getProductId()))
                        .findFirst()
                        .orElseGet(() -> {
                            BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                            return new OrderRecord(request.getRazorpayOrderId(), total, "INR", "PAID", user, product, item.getQuantity());
                        });

                record.setRazorpayPaymentId(request.getRazorpayPaymentId());
                record.setStatus("PAID");
                orderRecordRepository.save(record);

                completedOrders.add(new OrderRecordDTO(record));
            }

            log.info("Cart Payment SUCCESS verified for Order: {}, Payment ID: {}, Items: {}",
                    request.getRazorpayOrderId(), request.getRazorpayPaymentId(), completedOrders.size());

            return completedOrders;
        } catch (Exception e) {
            log.error("Failed to verify cart payment: {}", e.getMessage(), e);
            throw new RuntimeException("Cart payment verification error: " + e.getMessage());
        }
    }

    public List<OrderRecordDTO> getUserOrders(User user) {
        return orderRecordRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(OrderRecordDTO::new)
                .collect(Collectors.toList());
    }
}
