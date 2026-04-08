package com.finance.tracker.controller;

import com.finance.tracker.dto.ApiResponse;
import com.finance.tracker.model.Category;
import com.finance.tracker.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Manage expense categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all categories")
    public ResponseEntity<ApiResponse<List<Category>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<ApiResponse<Category>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a custom category")
    public ResponseEntity<ApiResponse<Category>> create(@RequestBody CategoryRequest request) {
        Category category = categoryService.create(request.getName(), request.getIcon(), request.getColorHex());
        return ResponseEntity.ok(ApiResponse.success("Category created", category));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category")
    public ResponseEntity<ApiResponse<Category>> update(
            @PathVariable Long id,
            @RequestBody CategoryRequest request) {
        Category category = categoryService.update(id, request.getName(), request.getIcon(), request.getColorHex());
        return ResponseEntity.ok(ApiResponse.success("Category updated", category));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a custom category")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted", null));
    }

    @Data
    public static class CategoryRequest {
        private String name;
        private String icon;
        private String colorHex;
    }
}
