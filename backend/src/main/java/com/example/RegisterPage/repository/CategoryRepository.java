package com.example.RegisterPage.repository;

import com.example.RegisterPage.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Fetch all top-level categories (level 1, parent is null)
    List<Category> findByParentIsNull();

    // Fetch children of a specific category
    List<Category> findByParentId(Long parentId);

    // Fetch by level
    List<Category> findByLevel(Integer level);

    /**
     * Eagerly fetches the full category tree (3 levels) in a single query
     * to avoid N+1 lazy loading of subcategories.
     */
    @Query("SELECT DISTINCT c FROM Category c " +
           "LEFT JOIN FETCH c.subCategories sc " +
           "LEFT JOIN FETCH sc.subCategories " +
           "WHERE c.parent IS NULL")
    List<Category> findFullTree();
}
