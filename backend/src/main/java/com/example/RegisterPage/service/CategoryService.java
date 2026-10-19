
package com.example.RegisterPage.service;

import com.example.RegisterPage.dto.CategoryDTO;
import com.example.RegisterPage.entity.Category;
import com.example.RegisterPage.repository.CategoryRepository;

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

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
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
}
