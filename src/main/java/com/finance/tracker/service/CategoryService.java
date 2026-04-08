package com.finance.tracker.service;

import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.model.Category;
import com.finance.tracker.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    public Category getByName(String name) {
        return categoryRepository.findByNameIgnoreCase(name)
            .orElse(null);
    }

    @Transactional
    public Category create(String name, String icon, String colorHex) {
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("Category already exists: " + name);
        }
        return categoryRepository.save(
            Category.builder().name(name).icon(icon).colorHex(colorHex).isDefault(false).build()
        );
    }

    @Transactional
    public Category update(Long id, String name, String icon, String colorHex) {
        Category category = getById(id);
        if (name != null) category.setName(name);
        if (icon != null) category.setIcon(icon);
        if (colorHex != null) category.setColorHex(colorHex);
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = getById(id);
        if (category.isDefault()) {
            throw new BadRequestException("Cannot delete a default system category");
        }
        categoryRepository.delete(category);
    }
}
