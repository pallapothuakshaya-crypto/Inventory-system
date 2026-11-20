package com.ims.inventory.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ims.inventory.dao.CategoryDAO;
import com.ims.inventory.dao.DuplicateEntryException;
import com.ims.inventory.model.Category;

@Service
public class CategoryService {

    private final CategoryDAO categoryDAO;
    private final HistoryService historyService;

    public CategoryService(CategoryDAO categoryDAO, HistoryService historyService) {
        this.categoryDAO = categoryDAO;
        this.historyService = historyService;
    }

    public Category addCategory(Category category) {
        return addCategory(category, "Admin");
    }

    public Category addCategory(Category category, String username) {
        validate(category);
        try {
            categoryDAO.addCategory(category);
            historyService.log("CREATE", "CATEGORY", category.getCategoryName(), "Created category", username);
        } catch (DuplicateEntryException e) {
            throw new BusinessRuleException(
                "A category named '" + category.getCategoryName() + "' already exists");
        }
        return category;
    }

    public List<Category> getAllCategories() {
        return categoryDAO.getAllCategories();
    }

    public Category getCategory(long id) {
        return categoryDAO.getCategoryById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category " + id + " not found"));
    }

    public Category updateCategory(long id, Category category) {
        return updateCategory(id, category, "Admin");
    }

    public Category updateCategory(long id, Category category, String username) {
        validate(category);
        category.setCategoryId(id);
        boolean updated;
        try {
            updated = categoryDAO.updateCategory(category);
        } catch (DuplicateEntryException e) {
            throw new BusinessRuleException(
                "A category named '" + category.getCategoryName() + "' already exists");
        }
        if (!updated) {
            throw new ResourceNotFoundException("Category " + id + " not found");
        }
        historyService.log("UPDATE", "CATEGORY", category.getCategoryName(), "Updated category details", username);
        return category;
    }

    public void deleteCategory(long id) {
        deleteCategory(id, "Admin");
    }

    public void deleteCategory(long id, String username) {
        Category cat = getCategory(id);   // throws "not found" if it doesn't exist
        if (categoryDAO.isCategoryInUse(id)) {
            throw new BusinessRuleException(
                "Cannot delete: products are still using this category");
        }
        categoryDAO.deleteCategory(id);
        historyService.log("DELETE", "CATEGORY", cat.getCategoryName(), "Deleted category #" + id, username);
    }

    private void validate(Category category) {
        if (category == null || category.getCategoryName() == null
                || category.getCategoryName().isBlank()) {
            throw new IllegalArgumentException("Category name is required");
        }
        category.setCategoryName(category.getCategoryName().trim());
    }
}