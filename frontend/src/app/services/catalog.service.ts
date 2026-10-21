import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Category, Product } from '../models/catalog.model';

@Injectable({
  providedIn: 'root'
})
export class CatalogService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  /**
   * Fetches the 3-level hierarchical category tree:
   * Top-level -> Parent -> Child
   */
  getCategoryTree(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.apiUrl}/categories/tree`);
  }

  /**
   * Fetches products.
   */
  getProducts(categoryId?: number): Observable<Product[]> {
    let params = new HttpParams();
    if (categoryId != null) {
      params = params.set('categoryId', categoryId.toString());
    }
    return this.http.get<Product[]>(`${this.apiUrl}/products`, {
      params,
      withCredentials: true
    });
  }

  /**
   * Fetches single product details for the Product Page.
   */
  getProductById(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/products/${id}`, {
      withCredentials: true
    });
  }

  /**
   * Admin-only: Creates a new product.
   */
  addProduct(product: Partial<Product>): Observable<Product> {
    return this.http.post<Product>(`${this.apiUrl}/products`, product, {
      withCredentials: true
    });
  }

  /**
   * Admin-only: Updates a product (Only creator admin allowed).
   */
  updateProduct(id: number, product: Partial<Product>): Observable<Product> {
    return this.http.put<Product>(`${this.apiUrl}/products/${id}`, product, {
      withCredentials: true
    });
  }

  /**
   * Admin-only: Deletes a product (Only creator admin allowed).
   */
  deleteProduct(id: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.apiUrl}/products/${id}`, {
      withCredentials: true
    });
  }

  /**
   * Creates a new category or subcategory.
   */
  createCategory(category: { name: string; slug?: string; parentId?: number | null }): Observable<Category> {
    return this.http.post<Category>(`${this.apiUrl}/categories`, category, {
      withCredentials: true
    });
  }

  /**
   * Updates an existing category (e.g. rename).
   */
  updateCategory(id: number, category: { name: string; slug?: string }): Observable<Category> {
    return this.http.put<Category>(`${this.apiUrl}/categories/${id}`, category, {
      withCredentials: true
    });
  }

  /**
   * Deletes a category.
   */
  deleteCategory(id: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.apiUrl}/categories/${id}`, {
      withCredentials: true
    });
  }
}
