import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface OrderResponse {
  razorpayOrderId: string;
  amount: number;
  currency: string;
  keyId: string;
  productName: string;
  quantity: number;
}

export interface PaymentVerificationRequest {
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
  productId: number;
  quantity: number;
}

export interface OrderRecordDTO {
  id: number;
  razorpayOrderId: string;
  razorpayPaymentId: string;
  amount: number;
  currency: string;
  status: string;
  productId: number;
  productName: string;
  productImageUrl: string;
  quantity: number;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private apiUrl = '/api/payment';

  constructor(private http: HttpClient) {}

  createOrder(productId: number, quantity: number): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(
      `${this.apiUrl}/create-order`,
      { productId, quantity },
      { withCredentials: true }
    );
  }

  verifyPayment(payload: PaymentVerificationRequest): Observable<{ message: string; order: OrderRecordDTO }> {
    return this.http.post<{ message: string; order: OrderRecordDTO }>(
      `${this.apiUrl}/verify-payment`,
      payload,
      { withCredentials: true }
    );
  }

  createCartOrder(items: { productId: number; quantity: number }[]): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(
      `${this.apiUrl}/create-cart-order`,
      { items },
      { withCredentials: true }
    );
  }

  verifyCartPayment(payload: {
    razorpayOrderId: string;
    razorpayPaymentId: string;
    razorpaySignature: string;
    items: { productId: number; quantity: number }[];
  }): Observable<{ message: string; orders: OrderRecordDTO[] }> {
    return this.http.post<{ message: string; orders: OrderRecordDTO[] }>(
      `${this.apiUrl}/verify-cart-payment`,
      payload,
      { withCredentials: true }
    );
  }

  getMyOrders(): Observable<OrderRecordDTO[]> {
    return this.http.get<OrderRecordDTO[]>(
      `${this.apiUrl}/my-orders`,
      { withCredentials: true }
    );
  }
}
