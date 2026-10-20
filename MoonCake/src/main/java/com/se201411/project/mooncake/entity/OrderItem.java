package com.se201411.project.mooncake.entity;

import com.se201411.project.mooncake.enums.DiscountGroup;
import com.se201411.project.mooncake.enums.InventorySource;
import com.se201411.project.mooncake.enums.OrderItemKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "ORDER_ITEM", schema = "dbo")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private SalesOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "configuration_id")
    private BoxConfiguration configuration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volume_discount_tier_id")
    private VolumeDiscountTier volumeDiscountTier;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_kind", nullable = false, length = 12)
    private OrderItemKind itemKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_source_snapshot", nullable = false, length = 16)
    private InventorySource inventorySourceSnapshot;

    @Nationalized
    @Column(name = "product_name_snapshot", nullable = false, length = 220)
    private String productNameSnapshot;

    @Nationalized
    @Column(name = "sku_snapshot", length = 80)
    private String skuSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_group_snapshot", length = 12)
    private DiscountGroup discountGroupSnapshot;

    @Nationalized
    @Column(name = "box_name_snapshot", length = 220)
    private String boxNameSnapshot;

    @Nationalized
    @Column(name = "box_sku_snapshot", length = 80)
    private String boxSkuSnapshot;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_base_price_snapshot", nullable = false, precision = 18, scale = 0)
    private BigDecimal unitBasePriceSnapshot;

    @Column(name = "discount_pct_snapshot", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentSnapshot = BigDecimal.ZERO;

    @Column(name = "unit_discount_snapshot", nullable = false, precision = 18, scale = 0)
    private BigDecimal unitDiscountSnapshot = BigDecimal.ZERO;

    @Column(name = "unit_final_price_snapshot", nullable = false, precision = 18, scale = 0)
    private BigDecimal unitFinalPriceSnapshot;

    @Column(name = "line_total", nullable = false, precision = 18, scale = 0)
    private BigDecimal lineTotal;

    public OrderItem() {
    }

    public Long getId() { return id; }
    public SalesOrder getOrder() { return order; }
    public void setOrder(SalesOrder order) { this.order = order; }
    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }
    public BoxConfiguration getConfiguration() { return configuration; }
    public void setConfiguration(BoxConfiguration configuration) { this.configuration = configuration; }
    public VolumeDiscountTier getVolumeDiscountTier() { return volumeDiscountTier; }
    public void setVolumeDiscountTier(VolumeDiscountTier value) { this.volumeDiscountTier = value; }
    public OrderItemKind getItemKind() { return itemKind; }
    public void setItemKind(OrderItemKind itemKind) { this.itemKind = itemKind; }
    public InventorySource getInventorySourceSnapshot() { return inventorySourceSnapshot; }
    public void setInventorySourceSnapshot(InventorySource value) { this.inventorySourceSnapshot = value; }
    public String getProductNameSnapshot() { return productNameSnapshot; }
    public void setProductNameSnapshot(String value) { this.productNameSnapshot = value; }
    public String getSkuSnapshot() { return skuSnapshot; }
    public void setSkuSnapshot(String skuSnapshot) { this.skuSnapshot = skuSnapshot; }
    public DiscountGroup getDiscountGroupSnapshot() { return discountGroupSnapshot; }
    public void setDiscountGroupSnapshot(DiscountGroup value) { this.discountGroupSnapshot = value; }
    public String getBoxNameSnapshot() { return boxNameSnapshot; }
    public void setBoxNameSnapshot(String value) { this.boxNameSnapshot = value; }
    public String getBoxSkuSnapshot() { return boxSkuSnapshot; }
    public void setBoxSkuSnapshot(String value) { this.boxSkuSnapshot = value; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitBasePriceSnapshot() { return unitBasePriceSnapshot; }
    public void setUnitBasePriceSnapshot(BigDecimal value) { this.unitBasePriceSnapshot = value; }
    public BigDecimal getDiscountPercentSnapshot() { return discountPercentSnapshot; }
    public void setDiscountPercentSnapshot(BigDecimal value) { this.discountPercentSnapshot = value; }
    public BigDecimal getUnitDiscountSnapshot() { return unitDiscountSnapshot; }
    public void setUnitDiscountSnapshot(BigDecimal value) { this.unitDiscountSnapshot = value; }
    public BigDecimal getUnitFinalPriceSnapshot() { return unitFinalPriceSnapshot; }
    public void setUnitFinalPriceSnapshot(BigDecimal value) { this.unitFinalPriceSnapshot = value; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
