package com.se201411.project.mooncake;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@ContextConfiguration(initializers = MoonCakeApplicationIT.TestDatabaseGuard.class)
class CatalogSeedIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void v3CreatesCompleteCatalogWithValidPresetCapacities() {
        Integer categoryCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM dbo.CATEGORY
            WHERE slug IN (N'banh-trung-thu', N'hop-qua-trung-thu')
            """, Integer.class);

        Map<String, Object> productCounts = jdbcTemplate.queryForMap("""
            SELECT
                SUM(CASE WHEN product.product_type = 'MOONCAKE' THEN 1 ELSE 0 END) AS mooncakes,
                SUM(CASE WHEN product.product_type = 'EMPTY_BOX' THEN 1 ELSE 0 END) AS empty_boxes,
                SUM(CASE WHEN product.product_type = 'GIFT_SET' THEN 1 ELSE 0 END) AS gift_sets
            FROM dbo.PRODUCT AS product
            JOIN dbo.PRODUCT_VARIANT AS variant ON variant.product_id = product.product_id
            WHERE variant.sku IN (
                N'MC-KSPM-080', N'MC-PBMHA-080', N'MC-CBTN-080', N'MC-TCGQ-080',
                N'MC-CBBB-080', N'MC-LSTT-080', N'MC-TXHN-120', N'MC-HSMC-120',
                N'MC-SCDHDC-120', N'MC-LXGQXO-150', N'MC-THRV-150', N'MC-SDHK-150',
                N'BOX-KIM-PHUC-4', N'BOX-LAM-PHUC-6',
                N'GIFT-KIM-PHUC-4', N'GIFT-LAM-PHUC-6'
            )
            """);

        Integer zeroStockBalanceCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM dbo.INVENTORY_BALANCE AS balance
            JOIN dbo.PRODUCT_VARIANT AS variant ON variant.variant_id = balance.variant_id
            WHERE variant.sku IN (
                N'MC-KSPM-080', N'MC-PBMHA-080', N'MC-CBTN-080', N'MC-TCGQ-080',
                N'MC-CBBB-080', N'MC-LSTT-080', N'MC-TXHN-120', N'MC-HSMC-120',
                N'MC-SCDHDC-120', N'MC-LXGQXO-150', N'MC-THRV-150', N'MC-SDHK-150',
                N'BOX-KIM-PHUC-4', N'BOX-LAM-PHUC-6',
                N'GIFT-KIM-PHUC-4', N'GIFT-LAM-PHUC-6'
            )
              AND balance.available_quantity = 0
            """, Integer.class);

        Integer readyPresetCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM dbo.BOX_CONFIGURATION AS configuration
            JOIN dbo.PRODUCT_VARIANT AS gift_variant
                ON gift_variant.variant_id = configuration.preset_variant_id
            WHERE gift_variant.sku IN (N'GIFT-KIM-PHUC-4', N'GIFT-LAM-PHUC-6')
              AND configuration.configuration_type = 'PRESET'
              AND configuration.configuration_status = 'READY'
              AND configuration.cart_id IS NULL
            """, Integer.class);

        Integer invalidCapacityCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM (
                SELECT configuration.configuration_id
                FROM dbo.BOX_CONFIGURATION AS configuration
                JOIN dbo.PRODUCT_VARIANT AS gift_variant
                    ON gift_variant.variant_id = configuration.preset_variant_id
                JOIN dbo.BOX AS box ON box.box_id = configuration.box_id
                JOIN dbo.BOX_CONFIGURATION_ITEM AS item
                    ON item.configuration_id = configuration.configuration_id
                WHERE gift_variant.sku IN (N'GIFT-KIM-PHUC-4', N'GIFT-LAM-PHUC-6')
                GROUP BY configuration.configuration_id, box.capacity_cakes
                HAVING SUM(item.quantity_per_box) <> box.capacity_cakes
            ) AS invalid_presets
            """, Integer.class);

        BigDecimal kimPhucPrice = jdbcTemplate.queryForObject("""
            SELECT unit_price
            FROM dbo.PRODUCT_VARIANT
            WHERE sku = N'GIFT-KIM-PHUC-4'
            """, BigDecimal.class);
        BigDecimal lamPhucPrice = jdbcTemplate.queryForObject("""
            SELECT unit_price
            FROM dbo.PRODUCT_VARIANT
            WHERE sku = N'GIFT-LAM-PHUC-6'
            """, BigDecimal.class);

        assertThat(categoryCount).isEqualTo(2);
        assertThat(((Number) productCounts.get("mooncakes")).intValue()).isEqualTo(12);
        assertThat(((Number) productCounts.get("empty_boxes")).intValue()).isEqualTo(2);
        assertThat(((Number) productCounts.get("gift_sets")).intValue()).isEqualTo(2);
        assertThat(zeroStockBalanceCount).isEqualTo(16);
        assertThat(readyPresetCount).isEqualTo(2);
        assertThat(invalidCapacityCount).isZero();
        assertThat(kimPhucPrice).isEqualByComparingTo("590000");
        assertThat(lamPhucPrice).isEqualByComparingTo("930000");
    }
}
