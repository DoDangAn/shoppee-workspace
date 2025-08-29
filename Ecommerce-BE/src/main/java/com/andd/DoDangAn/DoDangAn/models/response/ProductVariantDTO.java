package com.andd.DoDangAn.DoDangAn.models.response;

import com.andd.DoDangAn.DoDangAn.models.ProductVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantDTO {
    private String id;
    private String productName;
    private String description;
    private Integer quantity;
    private Double price;
    private Double newPrice;
    private LocalDateTime date;
    private Integer viewCount;
    private String imageUrl;
    private String videoUrl;
    private String videoPublicIds;
    private String productId;
    private String categoryId;
    private String categoryName;
    private String countryId;
    private String countryName;

    // --- Compatibility getters for existing frontend expecting different property names ---
    // FE expects 'name' instead of 'productName'
    public String getName() { return productName; }
    // FE expects 'image' instead of 'imageUrl'
    public String getImage() {
        if (imageUrl == null) return null;
        // If already an absolute URL or starts with /uploads assume ok
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }
        String path = imageUrl.startsWith("/uploads/") ? imageUrl : "/uploads/" + imageUrl;
        // Provide absolute URL so Angular (different origin) loads correct backend resource
        return "http://localhost:8080" + path;
    }
    // FE filtering uses 'category'; map to categoryName
    public String getCategory() { return categoryName; }

    public static ProductVariantDTO fromEntity(ProductVariant pv) {
        return ProductVariantDTO.builder()
                .id(pv.getId())
                .productName(pv.getProductName())
                .description(pv.getDescription())
                .quantity(pv.getQuantity())
                .price(pv.getPrice())
                .newPrice(pv.getNewPrice())
                .date(pv.getDate())
                .viewCount(pv.getViewCount())
                .imageUrl(pv.getImageUrl())
                .videoUrl(pv.getVideoUrl())
                .videoPublicIds(pv.getVideoPublicIds())
                .productId(pv.getProduct() != null ? pv.getProduct().getId() : null)
                .categoryId(pv.getCategory() != null ? pv.getCategory().getCategoryID() : null)
                .categoryName(pv.getCategory() != null ? pv.getCategory().getCategoryName() : null)
                .countryId(pv.getCountry() != null ? pv.getCountry().getCountryId() : null)
                .countryName(pv.getCountry() != null ? pv.getCountry().getCountryName() : null)
                .build();
    }
}
