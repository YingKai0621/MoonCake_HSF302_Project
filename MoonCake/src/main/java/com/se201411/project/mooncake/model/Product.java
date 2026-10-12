package com.se201411.project.mooncake.model;

/**
 * Model class representing a MoonCake product for catalog presentation.
 */
public class Product {
    private String id;
    private String name;
    private long price;
    private String formattedPrice;
    private String imageUrl;
    private String description;
    private String category; // "gift-boxes" or "mooncakes"
    private Integer boxCount; // e.g. 4, 6 for gift boxes
    private Integer weight;   // e.g. 150, 200 for individual mooncakes
    private String filling;   // e.g. "thap-cam", "lava", "hat-sen", "tra-xanh", "dau-xanh"
    private String detailUrl;

    public Product() {
    }

    public Product(String id, String name, long price, String formattedPrice, String imageUrl,
                   String description, String category, Integer boxCount, Integer weight,
                   String filling, String detailUrl) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.formattedPrice = formattedPrice;
        this.imageUrl = imageUrl;
        this.description = description;
        this.category = category;
        this.boxCount = boxCount;
        this.weight = weight;
        this.filling = filling;
        this.detailUrl = detailUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public String getFormattedPrice() {
        return formattedPrice;
    }

    public void setFormattedPrice(String formattedPrice) {
        this.formattedPrice = formattedPrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getBoxCount() {
        return boxCount;
    }

    public void setBoxCount(Integer boxCount) {
        this.boxCount = boxCount;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public String getFilling() {
        return filling;
    }

    public void setFilling(String filling) {
        this.filling = filling;
    }

    public String getDetailUrl() {
        return detailUrl;
    }

    public void setDetailUrl(String detailUrl) {
        this.detailUrl = detailUrl;
    }
}
