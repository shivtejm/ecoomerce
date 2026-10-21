import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../auth';
import { CatalogService } from '../../services/catalog.service';
import { Category, Product } from '../../models/catalog.model';

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
    // Automatically select the deepest category chosen (Child > Subcategory > Top Category)
    const effectiveCategoryId = this.formProduct.categoryId || this.selectedParentId || this.selectedTopId;

    if (!this.formProduct.name || !this.formProduct.price || !effectiveCategoryId) {
      this.errorMessage = 'Product Name, Price, and Category are required.';
      return;
    }

    this.formProduct.categoryId = effectiveCategoryId;

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

  // ==========================================
  // CATEGORY HIERARCHY MANAGEMENT
  // ==========================================
  showCategoryManager = false;
  showQuickAddModal = false;
  showQuickEditModal = false;
  categoryActionError = '';

  newCatName = '';
  newCatParentId: number | null = null;
  newCatParentName = '';

  editCatId: number | null = null;
  editCatName = '';

  openCategoryManagerModal(): void {
    this.categoryActionError = '';
    this.showCategoryManager = true;
  }

  closeCategoryManagerModal(): void {
    this.showCategoryManager = false;
  }

  quickAddCategory(parentId: number | null, parentName?: string): void {
    this.newCatParentId = parentId;
    this.newCatParentName = parentName || (parentId ? this.findCategoryById(parentId)?.name || 'Parent' : 'Root (Level 1)');
    this.newCatName = '';
    this.categoryActionError = '';
    this.showQuickAddModal = true;
  }

  closeQuickAddModal(): void {
    this.showQuickAddModal = false;
  }

  quickEditCategory(catId: number | null): void {
    if (!catId) return;
    const cat = this.findCategoryById(catId);
    if (!cat) return;
    this.editCatId = cat.id;
    this.editCatName = cat.name;
    this.categoryActionError = '';
    this.showQuickEditModal = true;
  }

  closeQuickEditModal(): void {
    this.showQuickEditModal = false;
  }

  submitCreateCategory(): void {
    if (!this.newCatName.trim()) {
      this.categoryActionError = 'Category name is required';
      return;
    }

    this.categoryActionError = '';
    this.catalogService.createCategory({
      name: this.newCatName.trim(),
      parentId: this.newCatParentId
    }).subscribe({
      next: (created) => {
        this.showToast(`Category "${created.name}" created successfully!`);
        this.showQuickAddModal = false;
        const parentId = this.newCatParentId;
        this.loadCategoryTreeWithCallback(() => {
          if (!parentId) {
            // New top category created: select it
            this.selectedTopId = created.id;
            this.onTopCategoryChange();
          } else if (parentId === this.selectedTopId) {
            // New subcategory created: select it
            this.selectedParentId = created.id;
            this.onParentCategoryChange();
          } else if (parentId === this.selectedParentId) {
            // New child category created: select it
            this.formProduct.categoryId = created.id;
          }
        });
      },
      error: (err) => {
        this.categoryActionError = err.error?.message || 'Failed to create category';
        this.cdr.detectChanges();
      }
    });
  }

  submitUpdateCategory(): void {
    if (!this.editCatId || !this.editCatName.trim()) {
      this.categoryActionError = 'Category name is required';
      return;
    }

    this.categoryActionError = '';
    this.catalogService.updateCategory(this.editCatId, {
      name: this.editCatName.trim()
    }).subscribe({
      next: (updated) => {
        this.showToast(`Category renamed to "${updated.name}"`);
        this.showQuickEditModal = false;
        this.loadCategoryTree();
      },
      error: (err) => {
        this.categoryActionError = err.error?.message || 'Failed to update category';
        this.cdr.detectChanges();
      }
    });
  }

  deleteCategory(cat: Category): void {
    if (!confirm(`Are you sure you want to delete category "${cat.name}" and any subcategories?`)) {
      return;
    }

    this.catalogService.deleteCategory(cat.id).subscribe({
      next: () => {
        this.showToast(`Category "${cat.name}" deleted successfully.`);
        if (this.selectedTopId === cat.id) this.selectedTopId = null;
        if (this.selectedParentId === cat.id) this.selectedParentId = null;
        if (this.formProduct.categoryId === cat.id) this.formProduct.categoryId = undefined;
        this.loadCategoryTree();
      },
      error: (err) => {
        alert(err.error?.message || 'Cannot delete category because products are assigned to it.');
      }
    });
  }

  findCategoryById(id: number): Category | null {
    const search = (list: Category[]): Category | null => {
      for (const item of list) {
        if (item.id === id) return item;
        if (item.subCategories && item.subCategories.length > 0) {
          const found = search(item.subCategories);
          if (found) return found;
        }
      }
      return null;
    };
    return search(this.categoryTree);
  }

  loadCategoryTreeWithCallback(cb?: () => void): void {
    this.catalogService.getCategoryTree().subscribe({
      next: (tree) => {
        this.categoryTree = tree;
        // Refresh available parents / children
        if (this.selectedTopId) {
          const top = this.categoryTree.find(c => c.id === this.selectedTopId);
          this.availableParents = top ? top.subCategories : [];
        }
        if (this.selectedParentId) {
          const parent = this.availableParents.find(c => c.id === this.selectedParentId);
          this.availableChildren = parent ? parent.subCategories : [];
        }
        if (cb) cb();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load category tree', err);
        this.cdr.detectChanges();
      }
    });
  }
}
