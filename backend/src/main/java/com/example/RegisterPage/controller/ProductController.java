package com.example.RegisterPage.controller;

import com.example.RegisterPage.dto.ProductDTO;
import com.example.RegisterPage.entity.User;
import com.example.RegisterPage.repository.UserRepository;
import com.example.RegisterPage.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final UserRepository userRepository;

    public ProductController(ProductService productService, UserRepository userRepository) {
        this.productService = productService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }

    /**
     * GET /api/products
     * Publicly accessible. If user is logged in, their ownership permissions are evaluated.
     */
    @GetMapping
    public ResponseEntity<List<ProductDTO>> getProducts(@RequestParam(required = false) Long categoryId,
                                                        Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (categoryId != null) {
            return ResponseEntity.ok(productService.getProductsByCategory(categoryId, currentUser));
        }
        return ResponseEntity.ok(productService.getAllProducts(currentUser));
    }

    /**
     * GET /api/products/category/{categoryId}
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductDTO>> getProductsByCategory(@PathVariable Long categoryId,
                                                                  Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId, currentUser));
    }

    /**
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id,
                                                     Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        ProductDTO product = productService.getProductById(id, currentUser);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    /**
     * POST /api/products
     * Only Admin can add products. The product's creator is automatically recorded.
     */
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody ProductDTO dto, Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null || !"ROLE_ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access denied: Only administrators can add products"));
        }

        ProductDTO created = productService.createProduct(dto, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/products/{id}
     * Only the specific admin who created the product can modify it!
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id,
                                           @RequestBody ProductDTO dto,
                                           Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null || !"ROLE_ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access denied: Only administrators can edit products"));
        }

        try {
            ProductDTO updated = productService.updateProduct(id, dto, user);
            return ResponseEntity.ok(updated);
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", se.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/products/{id}
     * Only the specific admin who created the product can delete it!
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id,
                                           Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null || !"ROLE_ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access denied: Only administrators can delete products"));
        }

        try {
            productService.deleteProduct(id, user);
            return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", se.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.notFound().build();
        }
    }
}
