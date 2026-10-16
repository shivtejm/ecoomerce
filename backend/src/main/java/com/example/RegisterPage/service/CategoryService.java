package com.example.RegisterPage.service;

import com.example.RegisterPage.dto.CategoryDTO;
import com.example.RegisterPage.entity.Category;
import com.example.RegisterPage.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Builds and returns the hierarchical category tree starting from Top-level (Level 1)
     * down to Parent categories (Level 2) and Child categories (Level 3).
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> getCategoryTree() {
        List<Category> topLevelCategories = categoryRepository.findFullTree();
        return topLevelCategories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Recursive converter from Entity to DTO to ensure clean JSON output.
     */
    public CategoryDTO convertToDTO(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setSlug(category.getSlug());
        dto.setLevel(category.getLevel());
        dto.setParentId(category.getParent() != null ? category.getParent().getId() : null);

        if (category.getSubCategories() != null && !category.getSubCategories().isEmpty()) {
            List<CategoryDTO> children = category.getSubCategories().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            dto.setSubCategories(children);
        } else {
            dto.setSubCategories(new ArrayList<>());
        }

        return dto;
    }

    /**
     * Returns breadcrumb names for a category up to the root.
     * e.g., ["Electronics", "Computers", "Gaming Laptops"]
     */
    public List<String> getBreadcrumb(Category category) {
        List<String> breadcrumb = new ArrayList<>();
        Category current = category;
        while (current != null) {
            breadcrumb.add(current.getName());
            current = current.getParent();
        }
        Collections.reverse(breadcrumb);
        return breadcrumb;
    }
}
