package com.se201411.project.mooncake.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

@Entity
@Table(name = "PRODUCT_VARIANT", schema = "dbo")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Nationalized
    @Column(name = "sku", nullable = false, length = 80)
    private String sku;

    @Nationalized
    @Column(name = "variant_name", nullable = false, length = 200)
    private String name;

    @Column(name = "weight_g")
    private Integer weightGrams;

    @Nationalized
    @Column(name = "filling_label", length = 180)
    private String fillingLabel;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 0)
    private BigDecimal unitPrice;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public ProductVariant() {
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getWeightGrams() { return weightGrams; }
    public void setWeightGrams(Integer weightGrams) { this.weightGrams = weightGrams; }
    public String getFillingLabel() { return fillingLabel; }
    public void setFillingLabel(String fillingLabel) { this.fillingLabel = fillingLabel; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
