package com.se201411.project.mooncake.product;

import java.util.List;
import com.se201411.project.mooncake.order.service.OrderManagementService;
import com.se201411.project.mooncake.cart.service.CartService;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.validation.AdminFormValidation;
import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.enums.ConfigurationStatus;
import com.se201411.project.mooncake.inventory.service.InventoryAdminService;
import com.se201411.project.mooncake.product.repository.PresetAdminRepository;
import com.se201411.project.mooncake.product.repository.ProductAdminRepository;
import com.se201411.project.mooncake.product.service.impl.PresetConfigurationAdminServiceImpl;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PresetUnlockRulesTest {
    private final PresetAdminRepository repo = mock(PresetAdminRepository.class);
    private final InventoryAdminService inventory = mock(InventoryAdminService.class);
    private final OrderManagementService orders = mock(OrderManagementService.class);
    private final CartService carts = mock(CartService.class);
    private PresetConfigurationAdminServiceImpl service;
    private BoxConfiguration recipe;
    private ProductVariant gift;

    @BeforeEach
    void setUp() {
        service = new PresetConfigurationAdminServiceImpl(
            repo,
            mock(ProductAdminRepository.class),
            inventory,
            mock(AdminAccessService.class),
            mock(AdminFormValidation.class),
            orders,
            carts,
            Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneOffset.UTC)
        );
        gift = new ProductVariant();
        ReflectionTestUtils.setField(gift, "id", 21L);
        gift.setActive(false);
        recipe = new BoxConfiguration();
        ReflectionTestUtils.setField(recipe, "id", 7L);
        recipe.setPresetVariant(gift);
        recipe.setStatus(ConfigurationStatus.READY);
        when(repo.lock(7L)).thenReturn(recipe);
    }

    @Test
    void lockedRecipeWithoutStockCartsOrOrdersReopensAsDraft() {
        service.unlock(7L);

        assertThat(recipe.getStatus()).isEqualTo(ConfigurationStatus.DRAFT);
        verify(repo).save(recipe);
    }

    @Test
    void packedGiftBoxStaysLocked() {
        when(inventory.hasStockOrHistory(21L)).thenReturn(true);

        assertThatThrownBy(() -> service.unlock(7L))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("đã đóng hộp");
        assertThat(recipe.getStatus()).isEqualTo(ConfigurationStatus.READY);
        verify(repo, never()).save(recipe);
    }

    @Test
    void giftBoxOnSaleOrInCartOrOrderedStaysLocked() {
        gift.setActive(true);
        assertThatThrownBy(() -> service.unlock(7L)).hasMessageContaining("đang mở bán");

        gift.setActive(false);
        when(carts.countItemsWithConfigurations(List.of(7L))).thenReturn(1L);
        assertThatThrownBy(() -> service.unlock(7L)).hasMessageContaining("giỏ hàng");

        when(carts.countItemsWithConfigurations(List.of(7L))).thenReturn(0L);
        when(orders.countItemsWithVariants(List.of(21L))).thenReturn(1L);
        assertThatThrownBy(() -> service.unlock(7L)).hasMessageContaining("đơn hàng");
        assertThat(service.canUnlock(7L)).isFalse();
    }
}
