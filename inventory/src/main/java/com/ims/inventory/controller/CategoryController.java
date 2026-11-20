package com.ims.inventory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ims.inventory.model.Category;
import com.ims.inventory.service.CategoryService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    private String getUsername(HttpServletRequest request) {
        if (request != null) {
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("username") != null) {
                return (String) session.getAttribute("username");
            }
        }
        return "Admin";
    }

    @PostMapping
    public ResponseEntity<Category> add(@RequestBody Category category, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                            .body(categoryService.addCategory(category, getUsername(request)));
    }

    @GetMapping
    public List<Category> getAll() {
        return categoryService.getAllCategories();
    }

    @GetMapping("/{id}")
    public Category getOne(@PathVariable long id) {
        return categoryService.getCategory(id);
    }

    @PutMapping("/{id}")
    public Category update(@PathVariable long id, @RequestBody Category category, HttpServletRequest request) {
        return categoryService.updateCategory(id, category, getUsername(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id, HttpServletRequest request) {
        categoryService.deleteCategory(id, getUsername(request));
        return ResponseEntity.noContent().build();
    }
}