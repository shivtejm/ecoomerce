package com.example.RegisterPage.repository;

import com.example.RegisterPage.entity.Category;
import com.example.RegisterPage.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Fetch products belonging directly to a child category
    List<Product> findByCategoryId(Long categoryId);

    // Fetch products belonging to any of the specified categories
    List<Product> findByCategoryIn(List<Category> categories);

    /**
     * Eagerly fetches ALL products with their category (+ parent chain) and createdBy user
     * in a single query to avoid N+1 lazy loading issues.
     */
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category c " +
           "LEFT JOIN FETCH c.parent cp " +
           "LEFT JOIN FETCH cp.parent " +
           "LEFT JOIN FETCH p.createdBy")
    List<Product> findAllWithCategoryAndCreator();

    /**
     * Eagerly fetches products by category IDs with their category chain and createdBy user.
     */
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category c " +
           "LEFT JOIN FETCH c.parent cp " +
           "LEFT JOIN FETCH cp.parent " +
           "LEFT JOIN FETCH p.createdBy " +
           "WHERE p.category IN :categories")
    List<Product> findByCategoryInWithCategoryAndCreator(@Param("categories") List<Category> categories);

    /**
     * Eagerly fetches a single product by ID with its category chain and createdBy user.
     */
    @Query("SELECT p FROM Product p " +
           "LEFT JOIN FETCH p.category c " +
           "LEFT JOIN FETCH c.parent cp " +
           "LEFT JOIN FETCH cp.parent " +
           "LEFT JOIN FETCH p.createdBy " +
           "WHERE p.id = :id")
    Optional<Product> findByIdWithCategoryAndCreator(@Param("id") Long id);
}
