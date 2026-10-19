# Application API Endpoints Documentation

**Base URL**: `http://localhost:8080`  
**Authentication**: JWT Token stored in HTTP-only cookie (`JWT`) or passed via Authorization header.  
**Security Config**: `SecurityConfig.java`

---

## Table of Contents
1. [Authentication & Account Endpoints](#1-authentication--account-endpoints)
2. [Product Catalog & Management Endpoints](#2-product-catalog--management-endpoints)
3. [Category Hierarchy Endpoints](#3-category-hierarchy-endpoints)
4. [Payment & Order Endpoints (Razorpay)](#4-payment--order-endpoints-razorpay)
5. [Admin User Management Endpoints](#5-admin-user-management-endpoints)

---

## 1. Authentication & Account Endpoints
**Controller Source**: `com.example.RegisterPage.controller.AuthController`  
**Base Path**: `/`

### 1.1 Register User
- **Method**: `POST`
- **URL**: `/register`
- **Access**: Public
- **Request Body** (`application/json`):
  ```json
  {
    "name": "John Doe",
    "email": "john@example.com",
    "password": "Password123"
  }
  ```
- **Responses**:
  - `201 Created`: `{"message": "Registration successful"}`
  - `409 Conflict`: `{"message": "Email is already registered"}`
  - `400 Bad Request`: Validation failure on request body

---

### 1.2 Login
- **Method**: `POST`
- **URL**: `/login`
- **Access**: Public
- **Request Body** (`application/json`):
  ```json
  {
    "email": "john@example.com",
    "password": "Password123"
  }
  ```
- **Responses**:
  - `200 OK`: Sets `Set-Cookie: JWT=<token>; HttpOnly; Path=/; Max-Age=3600; SameSite=Lax`
    ```json
    {
      "message": "Login successful",
      "email": "john@example.com",
      "name": "John Doe",
      "role": "ROLE_USER",
      "token": "<jwt-token>"
    }
    ```
  - `401 Unauthorized`: `{"message": "Invalid email or password"}`

---

### 1.3 Logout
- **Method**: `POST`
- **URL**: `/logout`
- **Access**: Public
- **Responses**:
  - `200 OK`: Clears cookie `Set-Cookie: JWT=; Max-Age=0`
    ```json
    {"message": "Logged out successfully"}
    ```

---

### 1.4 Get Current User Profile
- **Method**: `GET`
- **URL**: `/profile`
- **Access**: Authenticated (Requires valid JWT)
- **Responses**:
  - `200 OK`:
    ```json
    {
      "name": "John Doe",
      "email": "john@example.com",
      "role": "ROLE_USER"
    }
    ```
  - `401 Unauthorized`: `{"message": "Not authenticated"}`

---

### 1.5 Forgot Password
- **Method**: `POST`
- **URL**: `/forgot-password`
- **Access**: Public
- **Headers**: `Origin` (optional, defaults to `http://localhost:4200` for reset link)
- **Request Body** (`application/json`):
  ```json
  {
    "email": "john@example.com"
  }
  ```
- **Responses**:
  - `200 OK`: `{"message": "If an account exists with that email, a password reset link has been sent."}`

---

### 1.6 Validate Password Reset Token
- **Method**: `GET`
- **URL**: `/validate-reset-token`
- **Access**: Public
- **Query Parameters**:
  - `token` (String, required): The reset token received via email
- **Responses**:
  - `200 OK`: `{"valid": true, "message": "Token is valid"}`
  - `400 Bad Request`: `{"valid": false, "message": "Reset link is invalid or has expired"}`

---

### 1.7 Reset Password
- **Method**: `POST`
- **URL**: `/reset-password`
- **Access**: Public
- **Request Body** (`application/json`):
  ```json
  {
    "token": "<valid-reset-token>",
    "newPassword": "NewSecurePassword123"
  }
  ```
- **Responses**:
  - `200 OK`: `{"message": "Password has been reset successfully. You can now log in."}`
  - `400 Bad Request`: `{"message": "<error-reason>"}`

---

## 2. Product Catalog & Management Endpoints
**Controller Source**: `com.example.RegisterPage.controller.ProductController`  
**Base Path**: `/api/products`

### 2.1 Get All Products / Filter by Category
- **Method**: `GET`
- **URL**: `/api/products`
- **Access**: Public (Ownership evaluated if authenticated)
- **Query Parameters**:
  - `categoryId` (Long, optional): Category ID to filter products
- **Responses**:
  - `200 OK`: Array of `ProductDTO`
    ```json
    [
      {
        "id": 1,
        "name": "Smartphone",
        "description": "Latest model",
        "price": 699.99,
        "stock": 50,
        "imageUrl": "https://...",
        "categoryId": 2,
        "categoryName": "Electronics",
        "canEdit": false
      }
    ]
    ```

---

### 2.2 Get Products by Category Path
- **Method**: `GET`
- **URL**: `/api/products/category/{categoryId}`
- **Access**: Public
- **Path Variables**:
  - `categoryId` (Long, required): Category ID
- **Responses**:
  - `200 OK`: Array of `ProductDTO`

---

### 2.3 Get Product By ID
- **Method**: `GET`
- **URL**: `/api/products/{id}`
- **Access**: Public
- **Path Variables**:
  - `id` (Long, required): Product ID
- **Responses**:
  - `200 OK`: Single `ProductDTO`
  - `404 Not Found`: If product does not exist

---

### 2.4 Create Product
- **Method**: `POST`
- **URL**: `/api/products`
- **Access**: Authenticated Admin (`ROLE_ADMIN`)
- **Request Body** (`application/json`):
  ```json
  {
    "name": "New Product",
    "description": "Product description",
    "price": 99.99,
    "stock": 25,
    "imageUrl": "https://...",
    "categoryId": 1
  }
  ```
- **Responses**:
  - `201 Created`: Created `ProductDTO` object
  - `403 Forbidden`: `{"message": "Access denied: Only administrators can add products"}`

---

### 2.5 Update Product
- **Method**: `PUT`
- **URL**: `/api/products/{id}`
- **Access**: Authenticated Admin (Creator only)
- **Path Variables**:
  - `id` (Long, required): Product ID
- **Request Body** (`application/json`): `ProductDTO`
- **Responses**:
  - `200 OK`: Updated `ProductDTO`
  - `403 Forbidden`: `{"message": "You can only edit products you created"}` or admin privileges missing
  - `404 Not Found`: If product does not exist

---

### 2.6 Delete Product
- **Method**: `DELETE`
- **URL**: `/api/products/{id}`
- **Access**: Authenticated Admin (Creator only)
- **Path Variables**:
  - `id` (Long, required): Product ID
- **Responses**:
  - `200 OK`: `{"message": "Product deleted successfully"}`
  - `403 Forbidden`: If not creator or not admin
  - `404 Not Found`: If product does not exist

---

## 3. Category Hierarchy Endpoints
**Controller Source**: `com.example.RegisterPage.controller.CategoryController`  
**Base Path**: `/api/categories`

### 3.1 Get Category Tree
- **Method**: `GET`
- **URL**: `/api/categories/tree`
- **Access**: Public
- **Description**: Returns 3-tier hierarchical category structure (Top-level Categories &rarr; Parent Categories &rarr; Child Categories).
- **Responses**:
  - `200 OK`: Array of `CategoryDTO` (with nested `children`)
    ```json
    [
      {
        "id": 1,
        "name": "Electronics",
        "level": "TOP",
        "children": [
          {
            "id": 2,
            "name": "Computers",
            "level": "PARENT",
            "children": [
              {
                "id": 3,
                "name": "Laptops",
                "level": "CHILD",
                "children": []
              }
            ]
          }
        ]
      }
    ]
    ```

---

## 4. Payment & Order Endpoints (Razorpay)
**Controller Source**: `com.example.RegisterPage.controller.PaymentController`  
**Base Path**: `/api/payment`

### 4.1 Create Razorpay Order (Single Item)
- **Method**: `POST`
- **URL**: `/api/payment/create-order`
- **Access**: Authenticated User
- **Request Body** (`application/json`):
  ```json
  {
    "productId": 1,
    "quantity": 2,
    "deliveryAddress": "123 Main St, City, Country"
  }
  ```
- **Responses**:
  - `200 OK`: `OrderResponse` (Razorpay order ID, amount in paise, currency, keyId)
  - `401 Unauthorized`: `{"message": "You must be logged in to make a purchase"}`
  - `400 Bad Request`: Stock issues or Razorpay error

---

### 4.2 Verify Payment (Single Item)
- **Method**: `POST`
- **URL**: `/api/payment/verify-payment`
- **Access**: Authenticated User
- **Request Body** (`application/json`):
  ```json
  {
    "razorpayOrderId": "order_xyz123",
    "razorpayPaymentId": "pay_abc456",
    "razorpaySignature": "<signature_hash>",
    "productId": 1,
    "quantity": 2,
    "deliveryAddress": "123 Main St, City, Country"
  }
  ```
- **Responses**:
  - `200 OK`:
    ```json
    {
      "message": "Payment verified and order placed successfully",
      "order": { ... }
    }
    ```
  - `400 Bad Request`: `{"message": "Payment verification failed"}`

---

### 4.3 Create Cart Razorpay Order (Multi-Item)
- **Method**: `POST`
- **URL**: `/api/payment/create-cart-order`
- **Access**: Authenticated User
- **Request Body** (`application/json`):
  ```json
  {
    "deliveryAddress": "123 Main St, City, Country",
    "items": [
      { "productId": 1, "quantity": 1 },
      { "productId": 2, "quantity": 2 }
    ]
  }
  ```
- **Responses**:
  - `200 OK`: `OrderResponse` containing combined Razorpay order details
  - `400 Bad Request`: `{"message": "Failed to create cart order"}`

---

### 4.4 Verify Cart Payment (Multi-Item)
- **Method**: `POST`
- **URL**: `/api/payment/verify-cart-payment`
- **Access**: Authenticated User
- **Request Body** (`application/json`):
  ```json
  {
    "razorpayOrderId": "order_xyz123",
    "razorpayPaymentId": "pay_abc456",
    "razorpaySignature": "<signature_hash>",
    "deliveryAddress": "123 Main St, City, Country",
    "items": [
      { "productId": 1, "quantity": 1 },
      { "productId": 2, "quantity": 2 }
    ]
  }
  ```
- **Responses**:
  - `200 OK`:
    ```json
    {
      "message": "Cart payment verified and orders placed successfully",
      "orders": [ ... ]
    }
    ```
  - `400 Bad Request`: Verification failure message

---

### 4.5 Get My Orders
- **Method**: `GET`
- **URL**: `/api/payment/my-orders`
- **Access**: Authenticated User
- **Responses**:
  - `200 OK`: Array of `OrderRecordDTO` items representing user's purchase history
  - `401 Unauthorized`: `{"message": "You must be logged in to view your orders"}`

---

## 5. Admin User Management Endpoints
**Controller Source**: `com.example.RegisterPage.controller.AdminController`  
**Base Path**: `/api/admin`

### 5.1 List All Users
- **Method**: `GET`
- **URL**: `/api/admin/users`
- **Access**: Authenticated Admin (`ROLE_ADMIN`)
- **Responses**:
  - `200 OK`:
    ```json
    [
      {
        "id": 1,
        "name": "Admin User",
        "email": "admin@example.com",
        "role": "ROLE_ADMIN"
      },
      {
        "id": 2,
        "name": "Standard User",
        "email": "user@example.com",
        "role": "ROLE_USER"
      }
    ]
    ```
  - `403 Forbidden`: `{"message": "Access denied: Administrator privileges required"}`

---

### 5.2 Update User Role
- **Method**: `PUT`
- **URL**: `/api/admin/users/{id}/role`
- **Access**: Authenticated Admin (`ROLE_ADMIN`)
- **Path Variables**:
  - `id` (Long, required): Target User ID
- **Query Parameters**:
  - `newRole` (String, required): e.g., `ROLE_ADMIN` or `ROLE_USER`
- **Responses**:
  - `200 OK`:
    ```json
    {
      "message": "User user@example.com role updated to ROLE_ADMIN",
      "role": "ROLE_ADMIN"
    }
    ```
  - `403 Forbidden`: Admin privileges required
  - `404 Not Found`: If target user ID does not exist
