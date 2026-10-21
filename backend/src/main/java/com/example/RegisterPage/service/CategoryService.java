
package com.example.RegisterPage.service;

import com.example.RegisterPage.dto.CategoryDTO;
import com.example.RegisterPage.entity.Category;
import com.example.RegisterPage.entity.Product;
import com.example.RegisterPage.repository.CategoryRepository;
import com.example.RegisterPage.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    /**
     * Fetch all categories and build the hierarchy:
     * Level 1 -> Level 2 -> Level 3
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> getCategoryTree() {

        // Fetch all categories without recursively fetching children.
        List<Category> categories = categoryRepository.findAll();

        // Store one DTO per category ID.
        Map<Long, CategoryDTO> dtoMap = new HashMap<>();

        for (Category category : categories) {
            CategoryDTO dto = new CategoryDTO();

            dto.setId(category.getId());
            dto.setName(category.getName());
            dto.setSlug(category.getSlug());
            dto.setLevel(category.getLevel());

            dto.setParentId(
                    category.getParent() != null
                            ? category.getParent().getId()
                            : null
            );

            // Every category starts with an empty child list.
            dto.setSubCategories(new ArrayList<>());

            dtoMap.put(category.getId(), dto);
        }

        // Attach each category to its parent.
        List<CategoryDTO> roots = new ArrayList<>();

        for (Category category : categories) {

            CategoryDTO currentDTO =
                    dtoMap.get(category.getId());

            if (category.getParent() == null) {

                // No parent means this is a root category.
                roots.add(currentDTO);

            } else {

                Long parentId = category.getParent().getId();

                CategoryDTO parentDTO = dtoMap.get(parentId);

                if (parentDTO != null) {
                    parentDTO.getSubCategories().add(currentDTO);
                }
            }
        }

        // Return only root categories with nested children.
        return roots;
    }

    /**
     * Returns breadcrumb names from root to the selected category.
     */
    @Transactional(readOnly = true)
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

    /**
     * Creates a new category and automatically assigns the level based on parentId.
     */
    @Transactional
    public CategoryDTO createCategory(CategoryDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Category name is required");
        }

        String name = dto.getName().trim();
        String slug = dto.getSlug();
        if (slug == null || slug.trim().isEmpty()) {
            slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        } else {
            slug = slug.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        }

        Category parent = null;
        int level = 1;

        if (dto.getParentId() != null) {
            parent = categoryRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found with id: " + dto.getParentId()));
            level = parent.getLevel() + 1;
        }

        Category category = new Category(name, slug, level, parent);
        Category saved = categoryRepository.save(category);

        CategoryDTO result = new CategoryDTO();
        result.setId(saved.getId());
        result.setName(saved.getName());
        result.setSlug(saved.getSlug());
        result.setLevel(saved.getLevel());
        result.setParentId(saved.getParent() != null ? saved.getParent().getId() : null);
        result.setSubCategories(new ArrayList<>());
        return result;
    }

    /**
     * Updates category name and/or slug.
     */
    @Transactional
    public CategoryDTO updateCategory(Long id, CategoryDTO dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));

        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            category.setName(dto.getName().trim());
            String slug = dto.getSlug();
            if (slug == null || slug.trim().isEmpty()) {
                slug = dto.getName().trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
            }
            category.setSlug(slug);
        }

        Category updated = categoryRepository.save(category);

        CategoryDTO result = new CategoryDTO();
        result.setId(updated.getId());
        result.setName(updated.getName());
        result.setSlug(updated.getSlug());
        result.setLevel(updated.getLevel());
        result.setParentId(updated.getParent() != null ? updated.getParent().getId() : null);
        return result;
    }

    /**
     * Deletes a category and verifies it is safe to delete.
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));

        List<Category> allDescendants = new ArrayList<>();
        collectAllDescendants(category, allDescendants);

        List<Product> products = productRepository.findByCategoryIn(allDescendants);
        if (!products.isEmpty()) {
            throw new IllegalStateException("Cannot delete '" + category.getName() + "' because "
                    + products.size() + " product(s) belong to it or its subcategories. Please reassign or delete those products first.");
        }

        categoryRepository.delete(category);
    }

    private void collectAllDescendants(Category category, List<Category> list) {
        list.add(category);
        if (category.getSubCategories() != null) {
            for (Category child : category.getSubCategories()) {
                collectAllDescendants(child, list);
            }
        }
    }
}
