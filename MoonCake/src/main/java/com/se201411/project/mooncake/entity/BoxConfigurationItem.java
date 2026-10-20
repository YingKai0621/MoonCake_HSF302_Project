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

@Entity
@Table(name = "BOX_CONFIGURATION_ITEM", schema = "dbo")
public class BoxConfigurationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "configuration_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "configuration_id", nullable = false)
    private BoxConfiguration configuration;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mooncake_variant_id", nullable = false)
    private ProductVariant mooncakeVariant;

    @Column(name = "quantity_per_box", nullable = false)
    private int quantityPerBox;

    public BoxConfigurationItem() {
    }

    public Long getId() { return id; }
    public BoxConfiguration getConfiguration() { return configuration; }
    public void setConfiguration(BoxConfiguration configuration) { this.configuration = configuration; }
    public ProductVariant getMooncakeVariant() { return mooncakeVariant; }
    public void setMooncakeVariant(ProductVariant mooncakeVariant) { this.mooncakeVariant = mooncakeVariant; }
    public int getQuantityPerBox() { return quantityPerBox; }
    public void setQuantityPerBox(int quantityPerBox) { this.quantityPerBox = quantityPerBox; }
}
