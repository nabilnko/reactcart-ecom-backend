package com.shophub.service;

import com.shophub.exception.BadRequestException;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.Product;
import com.shophub.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProductService {

    private static final String PRODUCT_IMAGE_FOLDER = "products";

    private final ProductRepository productRepository;
    private final CloudinaryService cloudinaryService;

    public ProductService(ProductRepository productRepository, CloudinaryService cloudinaryService) {
        this.productRepository = productRepository;
        this.cloudinaryService = cloudinaryService;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public List<Product> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword);
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public Product createProductWithImages(
            Product product,
            MultipartFile imageFile,
            String imageUrl,
            MultipartFile[] additionalImageFiles,
            List<String> additionalImageUrls
    ) {
        applyMainImage(product, imageFile, imageUrl, false);
        applyAdditionalImages(product, additionalImageFiles, additionalImageUrls, null);
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product productDetails) {
        Product product = getProductById(id);

        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setOriginalPrice(productDetails.getOriginalPrice());
        product.setCategory(productDetails.getCategory());
        product.setStock(productDetails.getStock());
        product.setImage(productDetails.getImage());
        product.setImagePublicId(productDetails.getImagePublicId());
        product.setAdditionalImages(productDetails.getAdditionalImages());
        product.setAdditionalImagePublicIds(productDetails.getAdditionalImagePublicIds());
        product.setBadge(productDetails.getBadge());
        product.setInStock(productDetails.getStock() > 0);

        return productRepository.save(product);
    }

    public Product updateProductWithImages(
            Long id,
            Product product,
            MultipartFile imageFile,
            String imageUrl,
            Boolean keepExistingImage,
            MultipartFile[] additionalImageFiles,
            List<String> additionalImageUrls,
            List<String> existingAdditionalImages
    ) {
        Product existingProduct = getProductById(id);

        existingProduct.setName(product.getName());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setOriginalPrice(product.getOriginalPrice());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setStock(product.getStock());
        existingProduct.setBadge(product.getBadge());
        existingProduct.setRating(product.getRating());
        existingProduct.setInStock(product.getStock() > 0);

        applyMainImage(existingProduct, imageFile, imageUrl, Boolean.TRUE.equals(keepExistingImage));
        applyAdditionalImages(existingProduct, additionalImageFiles, additionalImageUrls, existingAdditionalImages);

        return productRepository.save(existingProduct);
    }

    public void deleteProduct(Long id) {
        Product product = getProductById(id);

        if (product.getImagePublicId() != null && !product.getImagePublicId().isBlank()) {
            try {
                cloudinaryService.deleteFile(product.getImagePublicId());
            } catch (IOException e) {
                throw new BadRequestException("Failed to delete product main image");
            }
        }

        if (product.getAdditionalImagePublicIds() != null) {
            for (String publicId : product.getAdditionalImagePublicIds()) {
                if (publicId == null || publicId.isBlank()) {
                    continue;
                }
                try {
                    cloudinaryService.deleteFile(publicId);
                } catch (IOException e) {
                    throw new BadRequestException("Failed to delete product additional images");
                }
            }
        }

        productRepository.delete(product);
    }

    public List<Product> getSaleProducts() {
        return productRepository.findByBadge("SALE");
    }

    private void applyMainImage(Product product, MultipartFile imageFile, String imageUrl, boolean keepExistingImage) {
        if (imageFile != null && !imageFile.isEmpty()) {
            deleteMainImageIfExists(product);
            Map uploadResult;
            try {
                uploadResult = cloudinaryService.uploadFile(imageFile, PRODUCT_IMAGE_FOLDER);
            } catch (IOException e) {
                throw new BadRequestException("Failed to upload product image");
            }
            product.setImage(uploadResult.get("secure_url").toString());
            product.setImagePublicId(uploadResult.get("public_id").toString());
            return;
        }

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            deleteMainImageIfExists(product);
            product.setImage(imageUrl);
            product.setImagePublicId(null);
            return;
        }

        if (!keepExistingImage) {
            deleteMainImageIfExists(product);
            product.setImage("https://via.placeholder.com/300x300?text=No+Image");
            product.setImagePublicId(null);
        }
    }

    private void deleteMainImageIfExists(Product product) {
        if (product.getImagePublicId() == null || product.getImagePublicId().isBlank()) {
            return;
        }

        try {
            cloudinaryService.deleteFile(product.getImagePublicId());
        } catch (IOException e) {
            throw new BadRequestException("Failed to delete existing product image");
        }

        product.setImagePublicId(null);
    }

    private void applyAdditionalImages(
            Product product,
            MultipartFile[] additionalImageFiles,
            List<String> additionalImageUrls,
            List<String> existingAdditionalImages
    ) {
        List<String> currentImages = product.getAdditionalImages() != null ? product.getAdditionalImages() : List.of();
        List<String> currentPublicIds = product.getAdditionalImagePublicIds() != null ? product.getAdditionalImagePublicIds() : List.of();

        Map<String, String> urlToPublicId = new HashMap<>();
        int alignedSize = Math.min(currentImages.size(), currentPublicIds.size());
        for (int i = 0; i < alignedSize; i++) {
            String url = currentImages.get(i);
            String publicId = currentPublicIds.get(i);
            if (url == null || url.isBlank()) {
                continue;
            }
            if (publicId == null || publicId.isBlank()) {
                continue;
            }
            urlToPublicId.putIfAbsent(url, publicId);
        }

        List<String> keptImages;
        if (existingAdditionalImages == null) {
            keptImages = new ArrayList<>(currentImages);
        } else {
            keptImages = new ArrayList<>(existingAdditionalImages);
        }

        List<String> updatedAdditionalImages = new ArrayList<>();
        List<String> updatedAdditionalPublicIds = new ArrayList<>();
        for (String url : keptImages) {
            if (url == null || url.isBlank()) {
                continue;
            }
            updatedAdditionalImages.add(url);
            updatedAdditionalPublicIds.add(urlToPublicId.get(url));
        }

        if (additionalImageFiles != null) {
            for (MultipartFile file : additionalImageFiles) {
                if (file == null || file.isEmpty()) {
                    continue;
                }

                Map uploadResult;
                try {
                    uploadResult = cloudinaryService.uploadFile(file, PRODUCT_IMAGE_FOLDER);
                } catch (IOException e) {
                    throw new BadRequestException("Failed to upload additional product image");
                }

                updatedAdditionalImages.add(uploadResult.get("secure_url").toString());
                updatedAdditionalPublicIds.add(uploadResult.get("public_id").toString());
            }
        }

        if (additionalImageUrls != null) {
            for (String url : additionalImageUrls) {
                if (url == null || url.trim().isEmpty()) {
                    continue;
                }
                updatedAdditionalImages.add(url);
                updatedAdditionalPublicIds.add(null);
            }
        }

        // Delete removed additional images (Cloudinary public IDs no longer referenced)
        if (product.getAdditionalImagePublicIds() != null) {
            for (String oldPublicId : product.getAdditionalImagePublicIds()) {
                if (oldPublicId == null || oldPublicId.isBlank()) {
                    continue;
                }

                if (!updatedAdditionalPublicIds.contains(oldPublicId)) {
                    try {
                        cloudinaryService.deleteFile(oldPublicId);
                    } catch (IOException e) {
                        throw new BadRequestException("Failed to delete removed additional image");
                    }
                }
            }
        }

        product.setAdditionalImages(updatedAdditionalImages);
        product.setAdditionalImagePublicIds(updatedAdditionalPublicIds);
    }
}
