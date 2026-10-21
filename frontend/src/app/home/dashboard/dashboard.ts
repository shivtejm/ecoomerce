import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../auth';
import { CatalogService } from '../../services/catalog.service';
import { Category, Product } from '../../models/catalog.model';

interface CategoryTab {
  id: number | null;
  name: string;
  rawCategory?: Category;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit, OnDestroy {

  userName = '';
  currentSlide = 0;

  images = [
    'assets/images/image1.png',
    'assets/images/image2.png',
    'assets/images/image3.png'
  ];

  private autoSlideTimer: ReturnType<typeof setInterval> | undefined;

  // Catalog State
  categoryTree: Category[] = [];
  categoryTabs: CategoryTab[] = [];
  selectedTabId: number | null = null;
  selectedParentCategory: Category | null = null;
  selectedChildCategory: Category | null = null;

  products: Product[] = [];
  filteredProducts: Product[] = [];
  searchQuery: string = '';

  isLoadingCategories = false;
  isLoadingProducts = false;
  cartCount = 0;

  // Alerts & Notifications
  toastMessage: string | null = null;
  loginAlertMessage: string | null = null;



  constructor(
    public authService: AuthService,
    private catalogService: CatalogService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    const user = this.authService.currentUser();
    this.userName = user?.name || localStorage.getItem('userName') || '';

    this.autoSlideTimer = setInterval(() => {
      this.nextSlide();
    }, 4500);

    this.loadCatalogData();
  }

  ngOnDestroy(): void {
    if (this.autoSlideTimer) {
      clearInterval(this.autoSlideTimer);
    }
  }

  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  get isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  // Carousel
  nextSlide(): void {
    this.currentSlide = (this.currentSlide + 1) % this.images.length;
    this.cdr.detectChanges();
  }

  previousSlide(): void {
    this.currentSlide = (this.currentSlide - 1 + this.images.length) % this.images.length;
    this.cdr.detectChanges();
  }

  goToSlide(index: number): void {
    this.currentSlide = index;
    this.cdr.detectChanges();
  }

  // Category and Product Data
  loadCatalogData(): void {
    this.isLoadingCategories = true;
    this.catalogService.getCategoryTree().subscribe({
      next: (tree) => {
        this.categoryTree = tree;
        this.buildCategoryTabs(tree);
        this.isLoadingCategories = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading category tree', err);
        this.isLoadingCategories = false;
        this.cdr.detectChanges();
      }
    });

    this.loadProducts();
  }

  buildCategoryTabs(tree: Category[]): void {
    const tabs: CategoryTab[] = [
      { id: null, name: 'All Products' }
    ];

    for (const cat of tree) {
      tabs.push({
        id: cat.id,
        name: cat.name,
        rawCategory: cat
      });
    }

    this.categoryTabs = tabs;
  }

  loadProducts(categoryId?: number): void {
    this.isLoadingProducts = true;
    this.cdr.detectChanges();
    this.catalogService.getProducts(categoryId).subscribe({
      next: (products) => {
        this.products = products;
        this.applyFilter();
        this.isLoadingProducts = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading products', err);
        this.isLoadingProducts = false;
        this.cdr.detectChanges();
      }
    });
  }

  selectCategoryTab(tab: CategoryTab): void {
    this.selectedTabId = tab.id;
    this.selectedParentCategory = null;
    this.selectedChildCategory = null;

    if (tab.id === null) {
      this.loadProducts();
    } else if (tab.rawCategory) {
      this.loadProducts(tab.id);
    } else {
      // For auxiliary tabs, search in existing products
      this.searchQuery = tab.name;
      this.applyFilter();
    }
  }

  get currentTopCategory(): Category | undefined {
    return this.categoryTree.find(c => c.id === this.selectedTabId);
  }

  selectParentSubCategory(parent: Category): void {
    if (this.selectedParentCategory?.id === parent.id) {
      this.selectedParentCategory = null;
      this.selectedChildCategory = null;
      if (this.selectedTabId) {
        this.loadProducts(this.selectedTabId);
      }
      return;
    }

    this.selectedParentCategory = parent;
    this.selectedChildCategory = null;
    this.loadProducts(parent.id);
  }

  selectChildSubCategory(child: Category): void {
    if (this.selectedChildCategory?.id === child.id) {
      this.selectedChildCategory = null;
      if (this.selectedParentCategory) {
        this.loadProducts(this.selectedParentCategory.id);
      }
      return;
    }

    this.selectedChildCategory = child;
    this.loadProducts(child.id);
  }

  onSearchChange(): void {
    this.applyFilter();
  }

  applyFilter(): void {
    if (!this.searchQuery.trim()) {
      this.filteredProducts = [...this.products];
    } else {
      const q = this.searchQuery.toLowerCase();
      this.filteredProducts = this.products.filter(p =>
        p.name.toLowerCase().includes(q) ||
        p.description?.toLowerCase().includes(q) ||
        p.categoryName?.toLowerCase().includes(q)
      );
    }
  }

  viewProductDetails(productId: number): void {
    this.router.navigate(['/home/product', productId]);
  }

  // Purchase / Add to Cart handler
  handleAddToCart(product: Product, event: Event): void {
    event.stopPropagation();

    // Anyone can browse, but only authenticated users can purchase
    if (!this.isLoggedIn) {
      this.loginAlertMessage = `Please log in to purchase or add "${product.name}" to your cart.`;
      return;
    }

    this.cartCount++;
    this.showToast(`Added "${product.name}" to your cart!`);
  }

  dismissLoginAlert(): void {
    this.loginAlertMessage = null;
  }

  goToLogin(): void {
    this.router.navigate(['/auth']);
  }

  showToast(msg: string): void {
    this.toastMessage = msg;
    setTimeout(() => {
      if (this.toastMessage === msg) {
        this.toastMessage = null;
      }
    }, 3000);
  }
}