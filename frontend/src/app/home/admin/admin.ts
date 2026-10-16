import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../auth';
import { CatalogService } from '../../services/catalog.service';
import { Category, Product, AdminUser } from '../../models/catalog.model';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './admin.html',
  styleUrl: './admin.css'
})
export class AdminComponent implements OnInit {

  activeTab: 'products' | 'users' = 'products';
  currentAdminEmail = '';

  // Products & Categories
  products: Product[] = [];
  categoryTree: Category[] = [];
  availableParents: Category[] = [];
  availableChildren: Category[] = [];

  // Form State
  isEditing = false;
  editingProductId: number | null = null;
  selectedTopId: number | null = null;
  selectedParentId: number | null = null;

  formProduct: Partial<Product> = {
    name: '',
    description: '',
    price: 0,
    stockQuantity: 10,
    imageUrl: '',
    categoryId: undefined
  };

  // User Management
  users: AdminUser[] = [];
  isLoadingUsers = false;

  // UI state
  isLoading = false;
  isSaving = false;
  toastMessage: string | null = null;
  errorMessage: string | null = null;

  constructor(
    public authService: AuthService,
    private catalogService: CatalogService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const user = this.authService.currentUser();
    this.currentAdminEmail = user?.email || localStorage.getItem('userEmail') || '';
    this.loadCategoryTree();
    this.loadProducts();
  }

  loadCategoryTree(): void {
    this.catalogService.getCategoryTree().subscribe({
      next: (tree) => {
        this.categoryTree = tree;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load category tree', err);
        this.cdr.detectChanges();
      }
    });
  }

  loadProducts(): void {
    this.isLoading = true;
    this.cdr.detectChanges();
    this.catalogService.getProducts().subscribe({
      next: (products) => {
        this.products = products;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load products', err);
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  loadUsers(): void {
    this.isLoadingUsers = true;
    this.cdr.detectChanges();
    this.catalogService.getAllUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.isLoadingUsers = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load users', err);
        this.isLoadingUsers = false;
        this.cdr.detectChanges();
      }
    });
  }

  switchTab(tab: 'products' | 'users'): void {
    this.activeTab = tab;
    if (tab === 'users') {
      this.loadUsers();
    } else {
      this.loadProducts();
    }
  }

  // Cascading Category selection
  onTopCategoryChange(): void {
    this.selectedParentId = null;
    this.formProduct.categoryId = undefined;
    this.availableChildren = [];

    const top = this.categoryTree.find(c => c.id === this.selectedTopId);
    this.availableParents = top ? top.subCategories : [];
  }

  onParentCategoryChange(): void {
    this.formProduct.categoryId = undefined;
    const parent = this.availableParents.find(c => c.id === this.selectedParentId);
    this.availableChildren = parent ? parent.subCategories : [];
  }

  // Edit Product
  editProduct(product: Product): void {
    if (!product.canModify) {
      this.showToast('You can only edit products that you created!');
      return;
    }

    this.isEditing = true;
    this.editingProductId = product.id;
    this.errorMessage = null;

    this.formProduct = {
      name: product.name,
      description: product.description,
      price: product.price,
      stockQuantity: product.stockQuantity,
      imageUrl: product.imageUrl,
      categoryId: product.categoryId
    };

    // Auto-select cascading categories for editing product
    this.autoSelectCategoriesForEdit(product.categoryId);

    // Scroll smoothly to form
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  autoSelectCategoriesForEdit(categoryId: number): void {
    for (const top of this.categoryTree) {
      for (const parent of top.subCategories) {
        for (const child of parent.subCategories) {
          if (child.id === categoryId) {
            this.selectedTopId = top.id;
            this.availableParents = top.subCategories;
            this.selectedParentId = parent.id;
            this.availableChildren = parent.subCategories;
            this.formProduct.categoryId = child.id;
            return;
          }
        }
      }
    }
  }

  cancelEdit(): void {
    this.isEditing = false;
    this.editingProductId = null;
    this.errorMessage = null;
    this.resetForm();
  }

  resetForm(): void {
    this.formProduct = {
      name: '',
      description: '',
      price: 0,
      stockQuantity: 10,
      imageUrl: '',
      categoryId: undefined
    };
    this.selectedTopId = null;
    this.selectedParentId = null;
    this.availableParents = [];
    this.availableChildren = [];
  }

  // Save (Create or Update) Product
  saveProduct(): void {
    if (!this.formProduct.name || !this.formProduct.price || !this.formProduct.categoryId) {
      this.errorMessage = 'Product Name, Price, and Category are required.';
      return;
    }

    this.isSaving = true;
    this.errorMessage = null;

    if (!this.formProduct.imageUrl) {
      this.formProduct.imageUrl = 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&auto=format&fit=crop&q=80';
    }

    if (this.isEditing && this.editingProductId) {
      // Update existing product
      this.catalogService.updateProduct(this.editingProductId, this.formProduct).subscribe({
        next: (updated) => {
          this.isSaving = false;
          this.isEditing = false;
          this.editingProductId = null;
          this.resetForm();
          this.showToast(`Product "${updated.name}" updated successfully!`);
          this.loadProducts();
        },
        error: (err) => {
          this.isSaving = false;
          this.errorMessage = err.error?.message || 'Failed to update product.';
          this.cdr.detectChanges();
        }
      });
    } else {
      // Create new product
      this.catalogService.addProduct(this.formProduct).subscribe({
        next: (created) => {
          this.isSaving = false;
          this.resetForm();
          this.showToast(`Product "${created.name}" created and added to catalog!`);
          this.loadProducts();
        },
        error: (err) => {
          this.isSaving = false;
          this.errorMessage = err.error?.message || 'Failed to add product.';
          this.cdr.detectChanges();
        }
      });
    }
  }

  // Delete Product
  deleteProduct(product: Product): void {
    if (!product.canModify) {
      this.showToast('You can only delete products that you created!');
      return;
    }

    if (!confirm(`Are you sure you want to delete "${product.name}"? This action cannot be undone.`)) {
      return;
    }

    this.catalogService.deleteProduct(product.id).subscribe({
      next: () => {
        this.showToast(`Product "${product.name}" deleted.`);
        this.loadProducts();
      },
      error: (err) => {
        this.showToast(err.error?.message || 'Failed to delete product.');
        this.cdr.detectChanges();
      }
    });
  }

  // Multi-Admin Role Promotion / Demotion
  toggleUserRole(user: AdminUser): void {
    const newRole = user.role === 'ROLE_ADMIN' ? 'ROLE_USER' : 'ROLE_ADMIN';
    const actionName = newRole === 'ROLE_ADMIN' ? 'Promote to Admin' : 'Revoke Admin';

    if (!confirm(`Are you sure you want to ${actionName} for "${user.email}"?`)) {
      return;
    }

    this.catalogService.updateUserRole(user.id, newRole).subscribe({
      next: () => {
        this.showToast(`User ${user.email} is now ${newRole}!`);
        this.loadUsers();
      },
      error: (err) => {
        this.showToast(err.error?.message || 'Failed to update user role.');
        this.cdr.detectChanges();
      }
    });
  }

  showToast(msg: string): void {
    this.toastMessage = msg;
    this.cdr.detectChanges();
    setTimeout(() => {
      if (this.toastMessage === msg) {
        this.toastMessage = null;
        this.cdr.detectChanges();
      }
    }, 3500);
  }
}
