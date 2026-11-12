package com.se201411.project.mooncake.product;

import com.se201411.project.mooncake.cart.dto.CartItemView;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.dto.CatalogVariantView;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StockDisplayTest {

    @Test
    void variantKnowsWhenItIsSoldOutOrRunningLow() {
        assertThat(variant(0).soldOut()).isTrue();
        assertThat(variant(3).lowStock()).isTrue();
        assertThat(variant(50).soldOut()).isFalse();
        assertThat(variant(50).lowStock()).isFalse();
    }

    @Test
    void cartLineWarnsWhenStockIsGoneOrTooLow() {
        assertThat(cartLine(2, 0).stockWarning()).contains("hết hàng");
        assertThat(cartLine(5, 2).stockWarning()).contains("Vượt quá số lượng");
        assertThat(cartLine(2, 10).stockWarning()).isNull();
    }

    private CatalogVariantView variant(int stock) {
        return new CatalogVariantView(1L, "SKU", "Bánh 80g", 80, "Kim sa", BigDecimal.valueOf(95_000),
            "95.000đ", "/images/cake.png", "Bánh", null, stock);
    }

    private CartItemView cartLine(int quantity, int stock) {
        BigDecimal price = BigDecimal.valueOf(95_000);
        return new CartItemView(1L, 1L, "banh", "Bánh", "Bánh 80g", "SKU", ProductType.MOONCAKE,
            "/images/cake.png", "Bánh", quantity, price, "95.000đ", price, "95.000đ", true, stock);
    }
}
