package com.shophub.controller;

import com.shophub.model.Category;
import com.shophub.model.Product;
import com.shophub.repository.CategoryRepository;
import com.shophub.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "http://localhost:3000")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private final String UPLOAD_DIR = "uploads/categories/";

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
    public ResponseEntity<Category> createCategory(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam(value = "icon", required = false) MultipartFile iconFile,
            @RequestParam(value = "iconUrl", required = false) String iconUrl,
            @RequestParam("color") String color) {

        try {
            Category category = new Category();
            category.setName(name);
            category.setDescription(description);
            category.setColor(color);

            if (iconFile != null && !iconFile.isEmpty()) {
                String iconPath = saveIcon(iconFile);
                category.setIcon(iconPath);
            } else if (iconUrl != null && !iconUrl.trim().isEmpty()) {
                category.setIcon(iconUrl);
            }

            Category savedCategory = categoryRepository.save(category);
            return ResponseEntity.ok(savedCategory);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam(value = "icon", required = false) MultipartFile iconFile,
            @RequestParam(value = "iconUrl", required = false) String iconUrl,
            @RequestParam(value = "keepExistingIcon", required = false) Boolean keepExistingIcon,
            @RequestParam("color") String color) {

        return categoryRepository.findById(id)
                .map(category -> {
                    try {
                        category.setName(name);
                        category.setDescription(description);
                        category.setColor(color);

                        if (keepExistingIcon == null || !keepExistingIcon) {
                            if (iconFile != null && !iconFile.isEmpty()) {
                                String iconPath = saveIcon(iconFile);
                                category.setIcon(iconPath);
                            } else if (iconUrl != null && !iconUrl.trim().isEmpty()) {
                                category.setIcon(iconUrl);
                            }
                        }

                        Category updated = categoryRepository.save(category);
                        return ResponseEntity.ok(updated);
                    } catch (Exception e) {
                        return ResponseEntity.badRequest().<Category>build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        return categoryRepository.findById(id)
                .map(category -> {
                    categoryRepository.delete(category);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private String saveIcon(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/" + UPLOAD_DIR + uniqueFilename;
    }
}
