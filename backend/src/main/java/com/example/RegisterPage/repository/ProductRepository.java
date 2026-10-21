package com.example.RegisterPage.repository;

import com.example.RegisterPage.entity.Category;
import com.example.RegisterPage.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Fetch products belonging to any of the specified categories
    List<Product> findByCategoryIn(List<Category> categories);
}
