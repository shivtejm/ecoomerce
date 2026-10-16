import { Component, OnInit, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CatalogService } from '../../services/catalog.service';
import { PaymentService, OrderRecordDTO } from '../../services/payment.service';
import { CartService } from '../../services/cart.service';
import { Product } from '../../models/catalog.model';
import { AuthService } from '../../auth';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.css'
})
export class ProductDetailComponent implements OnInit {
  product: Product | null = null;
  isLoading = true;
  quantity = 1;
  addedToCart = false;
  loginRequired = false;

  // Razorpay payment state
  isProcessingPayment = false;
  paymentSuccessOrder: OrderRecordDTO | null = null;
  paymentErrorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private catalogService: CatalogService,
    private paymentService: PaymentService,
    public cartService: CartService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const productId = Number(idParam);
      this.loadProduct(productId);
    } else {
      this.isLoading = false;
      this.cdr.detectChanges();
    }
  }

  loadProduct(id: number): void {
    this.isLoading = true;
    this.cdr.detectChanges();
    this.catalogService.getProductById(id).subscribe({
      next: (product) => {
        this.product = product;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load product', err);
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  incrementQuantity(): void {
    if (this.product && this.quantity < this.product.stockQuantity) {
      this.quantity++;
      this.cdr.detectChanges();
    }
  }

  decrementQuantity(): void {
    if (this.quantity > 1) {
      this.quantity--;
      this.cdr.detectChanges();
    }
  }

  addToCart(): void {
    if (!this.authService.isLoggedIn()) {
      this.loginRequired = true;
      this.cdr.detectChanges();
      return;
    }

    if (this.product) {
      this.cartService.addToCart(this.product, this.quantity);
      this.addedToCart = true;
      this.cdr.detectChanges();
      setTimeout(() => {
        this.addedToCart = false;
        this.cdr.detectChanges();
      }, 3500);
    }
  }

  // =====================================
  // RAZORPAY CHECKOUT
  // =====================================
  buyNowWithRazorpay(): void {
    if (!this.authService.isLoggedIn()) {
      this.loginRequired = true;
      this.cdr.detectChanges();
      return;
    }

    if (!this.product) return;

    this.isProcessingPayment = true;
    this.paymentErrorMessage = '';
    this.cdr.detectChanges();

    this.paymentService.createOrder(this.product.id, this.quantity).subscribe({
      next: (orderData) => {
        const options = {
          key: orderData.keyId,
          amount: Math.round(orderData.amount * 100), // In paise
          currency: orderData.currency || 'INR',
          name: 'Mart Store',
          description: `Order: ${orderData.productName} (x${orderData.quantity})`,
          image: this.product?.imageUrl || '',
          order_id: orderData.razorpayOrderId,
          handler: (response: any) => {
            this.ngZone.run(() => {
              this.verifyPayment({
                razorpayOrderId: response.razorpay_order_id,
                razorpayPaymentId: response.razorpay_payment_id,
                razorpaySignature: response.razorpay_signature,
                productId: this.product!.id,
                quantity: this.quantity
              });
            });
          },
          prefill: {
            name: this.authService.currentUser()?.name || '',
            email: this.authService.currentUser()?.email || ''
          },
          theme: {
            color: '#1e3a8a'
          },
          modal: {
            ondismiss: () => {
              this.ngZone.run(() => {
                this.isProcessingPayment = false;
                this.cdr.detectChanges();
              });
            }
          }
        };

        const razorpay = new (window as any).Razorpay(options);
        razorpay.on('payment.failed', (response: any) => {
          this.ngZone.run(() => {
            this.isProcessingPayment = false;
            this.paymentErrorMessage = response.error?.description || 'Payment was unsuccessful or cancelled.';
            this.cdr.detectChanges();
          });
        });

        razorpay.open();
      },
      error: (err: any) => {
        this.ngZone.run(() => {
          this.isProcessingPayment = false;
          this.paymentErrorMessage = err.error?.message || 'Failed to initiate Razorpay order. Please try again.';
          this.cdr.detectChanges();
        });
      }
    });
  }

  verifyPayment(payload: {
    razorpayOrderId: string;
    razorpayPaymentId: string;
    razorpaySignature: string;
    productId: number;
    quantity: number;
  }): void {
    this.paymentService.verifyPayment(payload).subscribe({
      next: (res) => {
        this.ngZone.run(() => {
          this.isProcessingPayment = false;
          this.paymentSuccessOrder = res.order;
          if (this.product) {
            this.product.stockQuantity = Math.max(0, this.product.stockQuantity - this.quantity);
          }
          this.cdr.detectChanges();
        });
      },
      error: (err: any) => {
        this.ngZone.run(() => {
          this.isProcessingPayment = false;
          this.paymentErrorMessage = err.error?.message || 'Payment signature verification failed.';
          this.cdr.detectChanges();
        });
      }
    });
  }

  closePaymentSuccessModal(): void {
    this.paymentSuccessOrder = null;
    this.cdr.detectChanges();
  }

  goToLogin(): void {
    this.router.navigate(['/auth']);
  }

  goBack(): void {
    this.router.navigate(['/home']);
  }
}
