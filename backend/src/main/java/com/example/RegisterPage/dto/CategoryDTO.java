package com.example.RegisterPage.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Category with nested subcategories.
 * Avoids any cyclic references and provides a clean JSON representation.
 */
public class CategoryDTO {

    private Long id;
    private String name;
    private String slug;
    private Integer level;
    private Long parentId;
    private List<CategoryDTO> subCategories = new ArrayList<>();

    public CategoryDTO() {
    }

    public CategoryDTO(Long id, String name, String slug, Integer level, Long parentId) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.level = level;
        this.parentId = parentId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public List<CategoryDTO> getSubCategories() {
        return subCategories;
    }

    public void setSubCategories(List<CategoryDTO> subCategories) {
        this.subCategories = subCategories;
    }
}
