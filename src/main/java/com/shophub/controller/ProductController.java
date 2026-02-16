package com.shophub.controller;

import com.shophub.exception.BadRequestException;
import com.shophub.model.Product;
import com.shophub.service.ImageUploadService;
import com.shophub.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:3000")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ImageUploadService imageUploadService;

    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> createProduct(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") Double price,
            @RequestParam(value = "originalPrice", required = false) Double originalPrice,
            @RequestParam("category") String category,
            @RequestParam("stock") Integer stock,
            @RequestParam(value = "badge", required = false) String badge,
            @RequestParam(value = "rating", defaultValue = "4.5") Double rating,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "additionalImageFiles", required = false) MultipartFile[] additionalImageFiles,
            @RequestParam(value = "additionalImageUrls", required = false) List<String> additionalImageUrls
    ) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setOriginalPrice(originalPrice);
        product.setCategory(category);
        product.setStock(stock);
        product.setBadge(badge);
        product.setRating(rating);
        product.setInStock(stock > 0);

        // Handle main image
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrlPath = imageUploadService.uploadImage(imageFile);
            product.setImage(imageUrlPath);
        } else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            product.setImage(imageUrl);
        } else {
            product.setImage("https://via.placeholder.com/300x300?text=No+Image");
        }

        // Handle additional images
        List<String> additionalImages = new ArrayList<>();

        // Upload additional files
        if (additionalImageFiles != null) {
            for (MultipartFile file : additionalImageFiles) {
                if (file != null && !file.isEmpty()) {
                    String uploadedUrl = imageUploadService.uploadImage(file);
                    additionalImages.add(uploadedUrl);
                }
            }
        }

        // Add additional URLs
        if (additionalImageUrls != null) {
            for (String url : additionalImageUrls) {
                if (url != null && !url.trim().isEmpty()) {
                    additionalImages.add(url);
                }
            }
        }

        product.setAdditionalImages(additionalImages);

        Product savedProduct = productService.createProduct(product);
        return ResponseEntity.ok(savedProduct);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") Double price,
            @RequestParam(value = "originalPrice", required = false) Double originalPrice,
            @RequestParam("category") String category,
            @RequestParam("stock") Integer stock,
            @RequestParam(value = "badge", required = false) String badge,
            @RequestParam(value = "rating", defaultValue = "4.5") Double rating,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "keepExistingImage", defaultValue = "false") Boolean keepExistingImage,
            @RequestParam(value = "additionalImageFiles", required = false) MultipartFile[] additionalImageFiles,
            @RequestParam(value = "additionalImageUrls", required = false) List<String> additionalImageUrls,
            @RequestParam(value = "existingAdditionalImages", required = false) List<String> existingAdditionalImages
    ) {
        Product existingProduct = productService.getProductById(id);

        existingProduct.setName(name);
        existingProduct.setDescription(description);
        existingProduct.setPrice(price);
        existingProduct.setOriginalPrice(originalPrice);
        existingProduct.setCategory(category);
        existingProduct.setStock(stock);
        existingProduct.setBadge(badge);
        existingProduct.setRating(rating);
        existingProduct.setInStock(stock > 0);

        // Handle main image update
        if (imageFile != null && !imageFile.isEmpty()) {
            if (existingProduct.getImage() != null && existingProduct.getImage().startsWith("/uploads/")) {
                imageUploadService.deleteImage(existingProduct.getImage());
            }
            String imageUrlPath = imageUploadService.uploadImage(imageFile);
            existingProduct.setImage(imageUrlPath);
        } else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            if (existingProduct.getImage() != null && existingProduct.getImage().startsWith("/uploads/")) {
                imageUploadService.deleteImage(existingProduct.getImage());
            }
            existingProduct.setImage(imageUrl);
        } else if (!keepExistingImage) {
            existingProduct.setImage("https://via.placeholder.com/300x300?text=No+Image");
        }

        // Handle additional images
        List<String> updatedAdditionalImages = new ArrayList<>();

        // Keep existing images that weren't deleted
        if (existingAdditionalImages != null) {
            updatedAdditionalImages.addAll(existingAdditionalImages);
        }

        // Upload new files
        if (additionalImageFiles != null) {
            for (MultipartFile file : additionalImageFiles) {
                if (file != null && !file.isEmpty()) {
                    String uploadedUrl = imageUploadService.uploadImage(file);
                    updatedAdditionalImages.add(uploadedUrl);
                }
            }
        }

        // Add new URLs
        if (additionalImageUrls != null) {
            for (String url : additionalImageUrls) {
                if (url != null && !url.trim().isEmpty()) {
                    updatedAdditionalImages.add(url);
                }
            }
        }

        existingProduct.setAdditionalImages(updatedAdditionalImages);

        Product updatedProduct = productService.updateProduct(id, existingProduct);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        Product product = productService.getProductById(id);

        // Delete main image
        if (product.getImage() != null && product.getImage().startsWith("/uploads/")) {
            imageUploadService.deleteImage(product.getImage());
        }

        // Delete additional images
        if (product.getAdditionalImages() != null) {
            for (String imageUrl : product.getAdditionalImages()) {
                if (imageUrl.startsWith("/uploads/")) {
                    imageUploadService.deleteImage(imageUrl);
                }
            }
        }

        productService.deleteProduct(id);
        return ResponseEntity.ok("Product deleted successfully");
    }
}
