package com.shophub.controller;

import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.Category;
import com.shophub.model.Product;
import com.shophub.repository.CategoryRepository;
import com.shophub.repository.ProductRepository;
import com.shophub.service.CategoryIconStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryIconStorageService categoryIconStorageService;

    @GetMapping
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        return categoryRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/products")
    public List<Product> getProductsByCategory(@PathVariable Long id) {
        List<Product> products = productRepository.findByCategoryId(id);

        if (products.isEmpty()) {
            Category category = categoryRepository.findById(id).orElse(null);
            if (category != null) {
                products = productRepository.findByCategory(category.getName());
            }
        }

        return products;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category> createCategory(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam(value = "icon", required = false) MultipartFile iconFile,
            @RequestParam(value = "iconUrl", required = false) String iconUrl,
            @RequestParam("color") String color) {

        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setColor(color);

        if (iconFile != null && !iconFile.isEmpty()) {
            String iconPath = categoryIconStorageService.saveIcon(iconFile);
            category.setIcon(iconPath);
        } else if (iconUrl != null && !iconUrl.trim().isEmpty()) {
            category.setIcon(iconUrl);
        }

        Category savedCategory = categoryRepository.save(category);
        return ResponseEntity.ok(savedCategory);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category> updateCategory(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam(value = "icon", required = false) MultipartFile iconFile,
            @RequestParam(value = "iconUrl", required = false) String iconUrl,
            @RequestParam(value = "keepExistingIcon", required = false) Boolean keepExistingIcon,
            @RequestParam("color") String color) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        category.setName(name);
        category.setDescription(description);
        category.setColor(color);

        if (keepExistingIcon == null || !keepExistingIcon) {
            if (iconFile != null && !iconFile.isEmpty()) {
                String iconPath = categoryIconStorageService.saveIcon(iconFile);
                category.setIcon(iconPath);
            } else if (iconUrl != null && !iconUrl.trim().isEmpty()) {
                category.setIcon(iconUrl);
            }
        }

        Category updated = categoryRepository.save(category);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        return categoryRepository.findById(id)
                .map(category -> {
                    categoryRepository.delete(category);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

}
