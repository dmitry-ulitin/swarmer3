package com.swarmer.finance.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.swarmer.finance.dto.CategoryDto;
import com.swarmer.finance.security.UserPrincipal;
import com.swarmer.finance.services.CategoryService;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllCategories(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(categoryService.getAllCategories(principal.getUserDto().id()));
    }

    @PostMapping
    public ResponseEntity<CategoryDto> saveCategory(
            @RequestBody CategoryDto category,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(categoryService.saveCategory(category, principal.getUserDto().id()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryDto category,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!id.equals(category.id())) {
            throw new IllegalArgumentException("Category id doesn't match");
        }
        return ResponseEntity.ok(categoryService.saveCategory(category, principal.getUserDto().id()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        categoryService.deleteCategory(id, principal.getUserDto().id());
        return ResponseEntity.ok().build();
    }
}