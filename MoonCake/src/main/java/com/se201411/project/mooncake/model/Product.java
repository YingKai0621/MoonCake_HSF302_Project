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
    private String productType;             // e.g. "Bánh nướng" or "Hộp quà Trung Thu"
    private String packageSpecification;    // e.g. "Hộp 4 bánh cao cấp kèm Trà Sen Tây Hồ"
    private String detailedDescription;     // Extended factual description for the Full Description section
    private String mainIngredients;         // Verified main ingredients, null if unverified
    private String shelfLifeInformation;    // Verified shelf life, null if unverified
    private String storageInstructions;     // Verified storage guidance, null if unverified

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

    public Product(String id, String name, long price, String formattedPrice, String imageUrl,
                   String description, String category, Integer boxCount, Integer weight,
                   String filling, String detailUrl, String productType, String packageSpecification,
                   String detailedDescription, String mainIngredients, String shelfLifeInformation,
                   String storageInstructions) {
        this(id, name, price, formattedPrice, imageUrl, description, category, boxCount, weight, filling, detailUrl);
        this.productType = productType;
        this.packageSpecification = packageSpecification;
        this.detailedDescription = detailedDescription;
        this.mainIngredients = mainIngredients;
        this.shelfLifeInformation = shelfLifeInformation;
        this.storageInstructions = storageInstructions;
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

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getPackageSpecification() {
        return packageSpecification;
    }

    public void setPackageSpecification(String packageSpecification) {
        this.packageSpecification = packageSpecification;
    }

    public String getDetailedDescription() {
        return detailedDescription;
    }

    public void setDetailedDescription(String detailedDescription) {
        this.detailedDescription = detailedDescription;
    }

    public String getMainIngredients() {
        return mainIngredients;
    }

    public void setMainIngredients(String mainIngredients) {
        this.mainIngredients = mainIngredients;
    }

    public String getShelfLifeInformation() {
        return shelfLifeInformation;
    }

    public void setShelfLifeInformation(String shelfLifeInformation) {
        this.shelfLifeInformation = shelfLifeInformation;
    }

    public String getStorageInstructions() {
        return storageInstructions;
    }

    public void setStorageInstructions(String storageInstructions) {
        this.storageInstructions = storageInstructions;
    }

    /**
     * Maps internal filling code to friendly Vietnamese display name.
     */
    public String getFillingDisplayName() {
        if (filling == null) return null;
        switch (filling) {
            case "thap-cam":
                return "Thập cẩm sò điệp XO";
            case "lava":
                return "Lava kim sa trứng muối";
            case "hat-sen":
                return "Hạt sen táo đỏ";
            case "tra-xanh":
                return "Trà xanh macca";
            case "dau-xanh":
                return "Đậu xanh hoàng kim";
            default:
                return filling;
        }
    }
}
