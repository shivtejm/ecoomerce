package com.example.RegisterPage.service;

import com.example.RegisterPage.dto.ProductDTO;
import com.example.RegisterPage.entity.Category;
import com.example.RegisterPage.entity.Product;
import com.example.RegisterPage.entity.User;
import com.example.RegisterPage.repository.CategoryRepository;
import com.example.RegisterPage.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          CategoryService categoryService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts(User currentUser) {
        return productRepository.findAllWithCategoryAndCreator().stream()
                .map(p -> convertToDTO(p, currentUser))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(Long categoryId, User currentUser) {
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            return new ArrayList<>();
        }

        Category category = categoryOpt.get();
        List<Category> allTargetCategories = new ArrayList<>();
        collectAllDescendants(category, allTargetCategories);

        return productRepository.findByCategoryInWithCategoryAndCreator(allTargetCategories).stream()
                .map(p -> convertToDTO(p, currentUser))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id, User currentUser) {
        return productRepository.findByIdWithCategoryAndCreator(id)
                .map(p -> convertToDTO(p, currentUser))
                .orElse(null);
    }

    public static final String DEFAULT_PRODUCT_IMAGE = "https://placehold.co/400x400?text=No+Image";
    private static final String OLD_WATCH_IMAGE_FRAGMENT = "photo-1523275335684-37898b6baf30";

    private String sanitizeImageUrl(String url) {
        if (url == null || url.trim().isEmpty() || url.contains(OLD_WATCH_IMAGE_FRAGMENT)) {
            return DEFAULT_PRODUCT_IMAGE;
        }
        return url.trim();
    }

    @Transactional
    public ProductDTO createProduct(ProductDTO dto, User creator) {
        Category category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId()).orElse(null);
        }

        Product product = new Product(
                dto.getName(),
                dto.getDescription(),
                dto.getPrice(),
                dto.getStockQuantity() != null ? dto.getStockQuantity() : 0,
                sanitizeImageUrl(dto.getImageUrl()),
                category,
                creator
        );

        Product saved = productRepository.save(product);
        return convertToDTO(saved, creator);
    }

    /**
     * Updates product ONLY if the requesting admin is the creator of this product.
     */
    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO dto, User adminUser) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));

        // Enforce ownership: only the creator admin can modify
        if (product.getCreatedBy() != null && !product.getCreatedBy().getId().equals(adminUser.getId())) {
            throw new SecurityException("Permission denied: You can only modify products that you created.");
        }

        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getPrice() != null) product.setPrice(dto.getPrice());
        if (dto.getStockQuantity() != null) product.setStockQuantity(dto.getStockQuantity());
        if (dto.getImageUrl() != null) product.setImageUrl(dto.getImageUrl());

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId()).orElse(null);
            if (category != null) {
                product.setCategory(category);
            }
        }

        // If product didn't have an owner before, associate with current updater
        if (product.getCreatedBy() == null) {
            product.setCreatedBy(adminUser);
        }

        Product updated = productRepository.save(product);
        return convertToDTO(updated, adminUser);
    }

    /**
     * Deletes product ONLY if the requesting admin is the creator of this product.
     */
    @Transactional
    public void deleteProduct(Long id, User adminUser) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));

        // Enforce ownership: only the creator admin can delete
        if (product.getCreatedBy() != null && !product.getCreatedBy().getId().equals(adminUser.getId())) {
            throw new SecurityException("Permission denied: You can only delete products that you created.");
        }

        productRepository.delete(product);
    }

    /**
     * Direct Buy / Instant Purchase: Decrements the product stock when purchased.
     */
    @Transactional
    public ProductDTO purchaseProduct(Long id, int quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));

        if (product.getStockQuantity() < quantity) {
            throw new IllegalStateException("Only " + product.getStockQuantity() + " units available in stock.");
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);
        Product saved = productRepository.save(product);
        return convertToDTO(saved, null);
    }

    private void collectAllDescendants(Category category, List<Category> list) {
        list.add(category);
        if (category.getSubCategories() != null) {
            for (Category child : category.getSubCategories()) {
                collectAllDescendants(child, list);
            }
        }
    }

    public ProductDTO convertToDTO(Product product, User currentUser) {
        List<String> breadcrumb = product.getCategory() != null
                ? categoryService.getBreadcrumb(product.getCategory())
                : new ArrayList<>();

        ProductDTO dto = new ProductDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getImageUrl(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : "",
                breadcrumb
        );

        if (product.getCreatedBy() != null) {
            dto.setCreatedById(product.getCreatedBy().getId());
            dto.setCreatedByEmail(product.getCreatedBy().getEmail());
            dto.setCreatedByName(product.getCreatedBy().getName());

            if (currentUser != null && currentUser.getId() != null) {
                dto.setCanModify(product.getCreatedBy().getId().equals(currentUser.getId()));
            } else {
                dto.setCanModify(false);
            }
        } else {
            // Default seed products: editable by any admin
            dto.setCreatedByName("System / Demo");
            dto.setCanModify(currentUser != null && "ROLE_ADMIN".equals(currentUser.getRole()));
        }

        return dto;
    }
}
