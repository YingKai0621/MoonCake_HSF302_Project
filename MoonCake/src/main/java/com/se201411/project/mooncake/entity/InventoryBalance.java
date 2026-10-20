package com.se201411.project.mooncake.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Entity
@Table(name = "INVENTORY_BALANCE", schema = "dbo")
public class InventoryBalance {

    @Id
    @Column(name = "variant_id")
    private Long variantId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold = 5;

    @Version
    @Column(name = "stock_version", nullable = false)
    private long stockVersion;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public InventoryBalance() {
    }

    public Long getVariantId() { return variantId; }
    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }
    public int getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(int value) { this.availableQuantity = value; }
    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int value) { this.lowStockThreshold = value; }
    public long getStockVersion() { return stockVersion; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
