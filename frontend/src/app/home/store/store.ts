import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CatalogService } from '../../services/catalog.service';
import { CartService } from '../../services/cart.service';
import { Category, Product } from '../../models/catalog.model';

@Component({
  selector: 'app-store',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './store.html',
  styleUrl: './store.css'
})
export class StoreComponent implements OnInit {
  categoryTree: Category[] = [];
  products: Product[] = [];
  filteredProducts: Product[] = [];

  selectedTopCategory: Category | null = null;
  selectedParentCategory: Category | null = null;
  selectedChildCategory: Category | null = null;

  searchTerm: string = '';
  isLoadingCategories = false;
  isLoadingProducts = false;
  errorMessage: string | null = null;
  toastMessage: string | null = null;

  get cartCount(): number {
    return this.cartService.totalCount();
  }

  constructor(
    private catalogService: CatalogService,
    public cartService: CartService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadCategories();
    this.loadProducts();

    // Read search query parameter from toolbar search
    this.route.queryParams.subscribe(params => {
      if (params['search']) {
        this.searchTerm = params['search'];
        this.applySearchFilter();
      }
    });
  }

  loadCategories(): void {
    this.isLoadingCategories = true;
    this.catalogService.getCategoryTree().subscribe({
      next: (tree) => {
        this.categoryTree = tree || [];
        this.isLoadingCategories = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load categories', err);
        this.isLoadingCategories = false;
        this.cdr.detectChanges();
      }
    });
  }

  loadProducts(categoryId?: number): void {
    this.isLoadingProducts = true;
    this.errorMessage = null;
    this.cdr.detectChanges();

    this.catalogService.getProducts(categoryId).subscribe({
      next: (products) => {
        this.products = products || [];
        this.applySearchFilter();
        this.isLoadingProducts = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load products', err);
        this.errorMessage = 'Unable to connect to product service. Please verify your connection or try again.';
        this.isLoadingProducts = false;
        this.cdr.detectChanges();
      }
    });
  }

  selectTopCategory(top: Category | null): void {
    if (this.selectedTopCategory?.id === top?.id && top !== null) {
      // Toggle off to show all
      this.clearCategoryFilter();
      return;
    }

    this.selectedTopCategory = top;
    this.selectedParentCategory = null;
    this.selectedChildCategory = null;

    if (top) {
      this.loadProducts(top.id);
    } else {
      this.loadProducts();
    }
  }

  selectParentCategory(parent: Category): void {
    if (this.selectedParentCategory?.id === parent.id) {
      // Revert back to top category level
      this.selectedParentCategory = null;
      this.selectedChildCategory = null;
      if (this.selectedTopCategory) {
        this.loadProducts(this.selectedTopCategory.id);
      }
      return;
    }

    this.selectedParentCategory = parent;
    this.selectedChildCategory = null;
    this.loadProducts(parent.id);
  }

  selectChildCategory(child: Category): void {
    if (this.selectedChildCategory?.id === child.id) {
      // Revert back to parent level
      this.selectedChildCategory = null;
      if (this.selectedParentCategory) {
        this.loadProducts(this.selectedParentCategory.id);
      }
      return;
    }

    this.selectedChildCategory = child;
    this.loadProducts(child.id);
  }

  clearCategoryFilter(): void {
    this.selectedTopCategory = null;
    this.selectedParentCategory = null;
    this.selectedChildCategory = null;
    this.searchTerm = '';
    this.router.navigate([], { relativeTo: this.route, queryParams: {} });
    this.loadProducts();
  }

  onSearchChange(): void {
    this.applySearchFilter();
  }

  private applySearchFilter(): void {
    if (!this.searchTerm.trim()) {
      this.filteredProducts = [...this.products];
    } else {
      const term = this.searchTerm.toLowerCase();
      this.filteredProducts = this.products.filter(p =>
        p.name.toLowerCase().includes(term) ||
        p.description?.toLowerCase().includes(term) ||
        p.categoryName?.toLowerCase().includes(term)
      );
    }
    this.cdr.detectChanges();
  }

  viewProduct(productId: number): void {
    this.router.navigate(['/home/product', productId]);
  }

  addToCart(product: Product, event: Event): void {
    event.stopPropagation();
    this.cartService.addToCart(product);
    this.showToast(`Added "${product.name}" to cart!`);
    this.cdr.detectChanges();
  }

  openCart(): void {
    this.cartService.openCart();
  }

  showToast(message: string): void {
    this.toastMessage = message;
    this.cdr.detectChanges();
    setTimeout(() => {
      if (this.toastMessage === message) {
        this.toastMessage = null;
        this.cdr.detectChanges();
      }
    }, 3000);
  }
}
