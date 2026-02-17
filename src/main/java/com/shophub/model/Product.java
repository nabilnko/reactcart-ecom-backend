package com.shophub.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 1000)
    private String description;

    private Double price;
    private Double originalPrice;

    // Keep string category for backward compatibility
    private String category;

    // NEW: Category relationship
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category categoryEntity;

    private Integer stock;
    private String badge;
    private Double rating = 4.5;
    private Boolean inStock = true;

    @Column(length = 500)
    private String image;

    private String imagePublicId;

    // Additional Images
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "product_images",
            joinColumns = @JoinColumn(name = "product_id")
    )
    @Column(name = "image_url", length = 500)
    @JsonProperty("additionalImages")
    private List<String> additionalImages = new ArrayList<>();

        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(
            name = "product_image_public_ids",
            joinColumns = @JoinColumn(name = "product_id")
        )
        @Column(name = "public_id", length = 255)
        private List<String> additionalImagePublicIds = new ArrayList<>();

    // Helper method
    @Transient
    public List<String> getAllImages() {
        List<String> allImages = new ArrayList<>();
        if (image != null && !image.isEmpty()) {
            allImages.add(image);
        }
        if (additionalImages != null) {
            allImages.addAll(additionalImages);
        }
        return allImages;
    }

    // NEW: Helper to get category name from either source
    @Transient
    public String getCategoryName() {
        if (categoryEntity != null) {
            return categoryEntity.getName();
        }
        return category;
    }
}
