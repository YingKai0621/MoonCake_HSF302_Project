package com.se201411.project.mooncake.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "BOX", schema = "dbo")
public class Box {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "box_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empty_box_variant_id", nullable = false)
    private ProductVariant emptyBoxVariant;

    @Column(name = "capacity_cakes", nullable = false)
    private int capacityCakes;

    @Nationalized
    @Column(name = "material", length = 180)
    private String material;

    @Nationalized
    @Column(name = "box_notes", length = 500)
    private String notes;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Box() {
    }

    public Long getId() { return id; }
    public ProductVariant getEmptyBoxVariant() { return emptyBoxVariant; }
    public void setEmptyBoxVariant(ProductVariant emptyBoxVariant) { this.emptyBoxVariant = emptyBoxVariant; }
    public int getCapacityCakes() { return capacityCakes; }
    public void setCapacityCakes(int capacityCakes) { this.capacityCakes = capacityCakes; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
