package com.se201411.project.mooncake.entity;

import com.se201411.project.mooncake.enums.ComponentRole;
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
@Table(name = "ORDER_ITEM_COMPONENT", schema = "dbo")
public class OrderItemComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_component_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_variant_id", nullable = false)
    private ProductVariant componentVariant;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_role", nullable = false, length = 8)
    private ComponentRole componentRole;

    @Nationalized
    @Column(name = "component_name_snapshot", nullable = false, length = 220)
    private String componentNameSnapshot;

    @Column(name = "quantity_per_set", nullable = false)
    private int quantityPerSet;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 18, scale = 0)
    private BigDecimal unitPriceSnapshot;

    public OrderItemComponent() {
    }

    public Long getId() { return id; }
    public OrderItem getOrderItem() { return orderItem; }
    public void setOrderItem(OrderItem orderItem) { this.orderItem = orderItem; }
    public ProductVariant getComponentVariant() { return componentVariant; }
    public void setComponentVariant(ProductVariant value) { this.componentVariant = value; }
    public ComponentRole getComponentRole() { return componentRole; }
    public void setComponentRole(ComponentRole componentRole) { this.componentRole = componentRole; }
    public String getComponentNameSnapshot() { return componentNameSnapshot; }
    public void setComponentNameSnapshot(String value) { this.componentNameSnapshot = value; }
    public int getQuantityPerSet() { return quantityPerSet; }
    public void setQuantityPerSet(int quantityPerSet) { this.quantityPerSet = quantityPerSet; }
    public BigDecimal getUnitPriceSnapshot() { return unitPriceSnapshot; }
    public void setUnitPriceSnapshot(BigDecimal value) { this.unitPriceSnapshot = value; }
}
