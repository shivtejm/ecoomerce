import { Injectable, signal, computed } from '@angular/core';
import { Product } from '../models/catalog.model';

export interface CartItem {
  product: Product;
  quantity: number;
}

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private readonly storageKey = 'mart_user_cart';

  readonly items = signal<CartItem[]>(this.loadFromStorage());
  readonly isCartOpen = signal<boolean>(false);

  readonly totalCount = computed(() =>
    this.items().reduce((sum, item) => sum + item.quantity, 0)
  );

  readonly subtotal = computed(() =>
    this.items().reduce((sum, item) => sum + (Number(item.product.price) * item.quantity), 0)
  );

  constructor() {}

  private loadFromStorage(): CartItem[] {
    try {
      const data = localStorage.getItem(this.storageKey);
      if (data) {
        return JSON.parse(data);
      }
    } catch (e) {
      console.error('Failed to read cart from localStorage', e);
    }
    return [];
  }

  private saveToStorage(items: CartItem[]): void {
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(items));
    } catch (e) {
      console.error('Failed to save cart to localStorage', e);
    }
  }

  addToCart(product: Product, quantity = 1): void {
    const current = [...this.items()];
    const index = current.findIndex(i => i.product.id === product.id);

    if (index > -1) {
      const newQty = current[index].quantity + quantity;
      // Do not exceed available stock
      const maxAllowed = product.stockQuantity || 999;
      current[index] = {
        ...current[index],
        quantity: Math.min(newQty, maxAllowed)
      };
    } else {
      current.push({
        product,
        quantity: Math.min(quantity, product.stockQuantity || 999)
      });
    }

    this.items.set(current);
    this.saveToStorage(current);
  }

  updateQuantity(productId: number, newQuantity: number): void {
    if (newQuantity <= 0) {
      this.removeFromCart(productId);
      return;
    }

    const current = this.items().map(item => {
      if (item.product.id === productId) {
        const maxAllowed = item.product.stockQuantity || 999;
        return {
          ...item,
          quantity: Math.min(newQuantity, maxAllowed)
        };
      }
      return item;
    });

    this.items.set(current);
    this.saveToStorage(current);
  }

  removeFromCart(productId: number): void {
    const current = this.items().filter(item => item.product.id !== productId);
    this.items.set(current);
    this.saveToStorage(current);
  }

  clearCart(): void {
    this.items.set([]);
    localStorage.removeItem(this.storageKey);
  }

  openCart(): void {
    this.isCartOpen.set(true);
  }

  closeCart(): void {
    this.isCartOpen.set(false);
  }

  toggleCart(): void {
    this.isCartOpen.update(open => !open);
  }
}
