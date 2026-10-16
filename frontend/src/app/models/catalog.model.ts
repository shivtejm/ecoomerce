/**
 * Category data model representing the 3-tier hierarchy:
 * Level 1: Top-level category (e.g., Electronics)
 * Level 2: Parent category (e.g., Computers & Laptops)
 * Level 3: Child category (e.g., Gaming Laptops)
 */
export interface Category {
  id: number;
  name: string;
  slug: string;
  level: number;
  parentId: number | null;
  subCategories: Category[];
}

/**
 * Product data model with category and creator ownership info.
 */
export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  imageUrl: string;
  categoryId: number;
  categoryName: string;
  breadcrumb?: string[];

  // Ownership tracking
  createdById?: number;
  createdByEmail?: string;
  createdByName?: string;
  canModify?: boolean; // True ONLY if current logged-in admin is the creator
}

export interface AdminUser {
  id: number;
  name: string;
  email: string;
  role: string;
}
