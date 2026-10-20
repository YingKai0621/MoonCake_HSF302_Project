package com.se201411.project.mooncake.entity;

import com.se201411.project.mooncake.enums.ProductType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "PRODUCT", schema = "dbo")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Nationalized
    @Column(name = "slug", nullable = false, length = 180)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 20)
    private ProductType productType;

    @Nationalized
    @Column(name = "product_name", nullable = false, length = 220)
    private String name;

    @Nationalized
    @Column(name = "short_description", nullable = false, length = 600)
    private String shortDescription;

    @Lob
    @Nationalized
    @Column(name = "detailed_description", columnDefinition = "NVARCHAR(MAX)")
    private String detailedDescription;

    @Nationalized
    @Column(name = "main_ingredients", length = 700)
    private String mainIngredients;

    @Nationalized
    @Column(name = "shelf_life_info", length = 350)
    private String shelfLifeInfo;

    @Nationalized
    @Column(name = "storage_guidance", length = 600)
    private String storageGuidance;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Product() {
    }

    public Long getId() { return id; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }
    public String getDetailedDescription() { return detailedDescription; }
    public void setDetailedDescription(String detailedDescription) { this.detailedDescription = detailedDescription; }
    public String getMainIngredients() { return mainIngredients; }
    public void setMainIngredients(String mainIngredients) { this.mainIngredients = mainIngredients; }
    public String getShelfLifeInfo() { return shelfLifeInfo; }
    public void setShelfLifeInfo(String shelfLifeInfo) { this.shelfLifeInfo = shelfLifeInfo; }
    public String getStorageGuidance() { return storageGuidance; }
    public void setStorageGuidance(String storageGuidance) { this.storageGuidance = storageGuidance; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
