package com.se201411.project.mooncake.entity;

import com.se201411.project.mooncake.enums.ConfigurationStatus;
import com.se201411.project.mooncake.enums.ConfigurationType;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "BOX_CONFIGURATION", schema = "dbo")
public class BoxConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "configuration_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "box_id", nullable = false)
    private Box box;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preset_variant_id")
    private ProductVariant presetVariant;

    @Enumerated(EnumType.STRING)
    @Column(name = "configuration_type", nullable = false, length = 12)
    private ConfigurationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "configuration_status", nullable = false, length = 16)
    private ConfigurationStatus status = ConfigurationStatus.DRAFT;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public BoxConfiguration() {
    }

    public Long getId() { return id; }
    public Box getBox() { return box; }
    public void setBox(Box box) { this.box = box; }
    public Cart getCart() { return cart; }
    public void setCart(Cart cart) { this.cart = cart; }
    public ProductVariant getPresetVariant() { return presetVariant; }
    public void setPresetVariant(ProductVariant presetVariant) { this.presetVariant = presetVariant; }
    public ConfigurationType getType() { return type; }
    public void setType(ConfigurationType type) { this.type = type; }
    public ConfigurationStatus getStatus() { return status; }
    public void setStatus(ConfigurationStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
