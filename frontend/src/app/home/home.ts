import { Component, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../auth';
import { CartService, CartItem } from '../services/cart.service';
import { PaymentService, OrderRecordDTO } from '../services/payment.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    FormsModule,
    MatToolbarModule,
    MatButtonModule
  ],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home {

  globalSearchTerm: string = '';
  isProcessingCheckout = false;
  checkoutErrorMessage = '';
  checkoutSuccessData: {
    paymentId: string;
    orderId: string;
    totalPaid: number;
    itemCount: number;
    orders: OrderRecordDTO[];
  } | null = null;

  constructor(
    private router: Router,
    public authService: AuthService,
    public cartService: CartService,
    private paymentService: PaymentService,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  get isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  get userName(): string {
    return this.authService.currentUser()?.name || localStorage.getItem('userName') || '';
  }

  get cartCount(): number {
    return this.cartService.totalCount();
  }

  get isCartOpen(): boolean {
    return this.cartService.isCartOpen();
  }

  get cartItems(): CartItem[] {
    return this.cartService.items();
  }

  get cartSubtotal(): number {
    return this.cartService.subtotal();
  }

  openCart(): void {
    this.cartService.openCart();
    this.checkoutErrorMessage = '';
  }

  closeCart(): void {
    this.cartService.closeCart();
  }

  updateCartQuantity(productId: number, qty: number): void {
    this.cartService.updateQuantity(productId, qty);
  }

  removeCartItem(productId: number): void {
    this.cartService.removeFromCart(productId);
  }

  clearCart(): void {
    this.cartService.clearCart();
  }

  goToDashboard() {
    this.router.navigate(['/home/dashboard']);
  }

  goToAdmin() {
    this.router.navigate(['/home/admin']);
  }

  goToStore() {
    this.router.navigate(['/home/store']);
  }

  onGlobalSearch() {
    const term = this.globalSearchTerm.trim();
    if (term) {
      this.router.navigate(['/home/store'], {
        queryParams: { search: term }
      });
    } else {
      this.router.navigate(['/home/store'], {
        queryParams: {}
      });
    }
  }

  clearGlobalSearch() {
    this.globalSearchTerm = '';
    this.router.navigate(['/home/store'], {
      queryParams: {}
    });
  }

  goToProfile() {
    if (this.isLoggedIn) {
      this.router.navigate(['/home/profile']);
    } else {
      this.router.navigate(['/auth']);
    }
  }

  goToLogin() {
    this.router.navigate(['/auth']);
  }

  // =====================================
  // RAZORPAY CART CHECKOUT
  // =====================================
  checkoutCartWithRazorpay(): void {
    if (!this.isLoggedIn) {
      this.closeCart();
      this.goToLogin();
      return;
    }

    if (this.cartItems.length === 0) return;

    this.isProcessingCheckout = true;
    this.checkoutErrorMessage = '';
    this.cdr.detectChanges();

    const itemsPayload = this.cartItems.map(item => ({
      productId: item.product.id,
      quantity: item.quantity
    }));

    this.paymentService.createCartOrder(itemsPayload).subscribe({
      next: (orderData) => {
        const options = {
          key: orderData.keyId,
          amount: Math.round(orderData.amount * 100), // In paise
          currency: orderData.currency || 'INR',
          name: 'Mart Store',
          description: `Cart Checkout (${orderData.quantity} item kinds)`,
          image: this.cartItems[0]?.product.imageUrl || '',
          order_id: orderData.razorpayOrderId,
          handler: (response: any) => {
            this.ngZone.run(() => {
              this.verifyCartPayment({
                razorpayOrderId: response.razorpay_order_id,
                razorpayPaymentId: response.razorpay_payment_id,
                razorpaySignature: response.razorpay_signature,
                items: itemsPayload
              }, orderData.amount);
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
                this.isProcessingCheckout = false;
                this.cdr.detectChanges();
              });
            }
          }
        };

        const razorpay = new (window as any).Razorpay(options);
        razorpay.on('payment.failed', (response: any) => {
          this.ngZone.run(() => {
            this.isProcessingCheckout = false;
            this.checkoutErrorMessage = response.error?.description || 'Payment was unsuccessful or cancelled.';
            this.cdr.detectChanges();
          });
        });

        razorpay.open();
      },
      error: (err: any) => {
        this.ngZone.run(() => {
          this.isProcessingCheckout = false;
          this.checkoutErrorMessage = err.error?.message || 'Failed to create Razorpay cart order. Please try again.';
          this.cdr.detectChanges();
        });
      }
    });
  }

  verifyCartPayment(payload: {
    razorpayOrderId: string;
    razorpayPaymentId: string;
    razorpaySignature: string;
    items: { productId: number; quantity: number }[];
  }, totalPaid: number): void {
    this.paymentService.verifyCartPayment(payload).subscribe({
      next: (res) => {
        this.ngZone.run(() => {
          this.isProcessingCheckout = false;
          const count = this.cartCount;
          this.cartService.clearCart();
          this.closeCart();

          this.checkoutSuccessData = {
            paymentId: payload.razorpayPaymentId,
            orderId: payload.razorpayOrderId,
            totalPaid: totalPaid,
            itemCount: count,
            orders: res.orders
          };
          this.cdr.detectChanges();
        });
      },
      error: (err: any) => {
        this.ngZone.run(() => {
          this.isProcessingCheckout = false;
          this.checkoutErrorMessage = err.error?.message || 'Payment signature verification failed.';
          this.cdr.detectChanges();
        });
      }
    });
  }

  closeCheckoutSuccessModal(): void {
    this.checkoutSuccessData = null;
    this.cdr.detectChanges();
    this.router.navigate(['/home/store']);
  }
}