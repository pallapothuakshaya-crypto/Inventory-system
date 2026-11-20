package com.ims.inventory.dao;

import java.util.List;
import java.util.Optional;

import com.ims.inventory.model.Category;

public interface CategoryDAO {
    void addCategory(Category category);
    boolean updateCategory(Category category);
    boolean deleteCategory(long id);
    Optional<Category> getCategoryById(long id);
    List<Category> getAllCategories();
    boolean isCategoryInUse(long id);
}