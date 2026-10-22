package com.se201411.project.mooncake.admin;

import com.se201411.project.mooncake.product.service.impl.PresetConfigurationAdminServiceImpl;
import com.se201411.project.mooncake.product.repository.*;
import com.se201411.project.mooncake.inventory.service.InventoryAdminService;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.common.validation.AdminFormValidation;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.entity.*;
import com.se201411.project.mooncake.enums.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class PresetAdminServiceTest {
    PresetAdminRepository repo = mock(PresetAdminRepository.class);
    ProductAdminRepository products = mock(ProductAdminRepository.class);
    InventoryAdminService inventory = mock(InventoryAdminService.class);
    PresetConfigurationAdminServiceImpl service = new PresetConfigurationAdminServiceImpl(repo, products, inventory, mock(AdminAccessService.class), mock(AdminFormValidation.class), Clock.systemUTC());
    BoxConfiguration config;

    @BeforeEach
    void setup() {
        config = new BoxConfiguration();
        ReflectionTestUtils.setField(config, "id", 1L);
        config.setType(ConfigurationType.PRESET);
        config.setStatus(ConfigurationStatus.DRAFT);
        var gift = variant(10, ProductType.GIFT_SET);
        config.setPresetVariant(gift);
        var box = new Box();
        ReflectionTestUtils.setField(box, "id", 2L);
        box.setCapacityCakes(4);
        box.setActive(true);
        box.setEmptyBoxVariant(variant(20, ProductType.EMPTY_BOX));
        config.setBox(box);
        when(repo.lock(1)).thenReturn(config);
    }

    @Test
    void readyCompositionCannotBeChanged() {
        config.setStatus(ConfigurationStatus.READY);
        assertThatThrownBy(() -> service.setComponent(1, 30, 4)).isInstanceOf(BusinessRuleViolationException.class);
        verify(repo, never()).saveItem(any());
    }

    @Test
    void stockedOrHistoricallyReferencedDraftCannotBeChanged() {
        when(inventory.hasStockOrHistory(10)).thenReturn(true);
        assertThatThrownBy(() -> service.setComponent(1, 30, 4)).isInstanceOf(BusinessRuleViolationException.class);
        when(inventory.hasStockOrHistory(10)).thenReturn(false);
        when(repo.historicallyReferenced(1, 10)).thenReturn(true);
        assertThatThrownBy(() -> service.setComponent(1, 30, 4)).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void activationRequiresExactCapacity() {
        var item = new BoxConfigurationItem();
        item.setMooncakeVariant(variant(30, ProductType.MOONCAKE));
        item.setQuantityPerBox(3);
        when(repo.items(1)).thenReturn(List.of(item));
        assertThatThrownBy(() -> service.activate(1)).isInstanceOf(BusinessRuleViolationException.class);
        assertThat(config.getStatus()).isEqualTo(ConfigurationStatus.DRAFT);
        item.setQuantityPerBox(4);
        service.activate(1);
        assertThat(config.getStatus()).isEqualTo(ConfigurationStatus.READY);
    }

    @Test
    void rejectsEmptyBoxAsCake() {
        when(products.variant(30)).thenReturn(variant(30, ProductType.EMPTY_BOX));
        assertThatThrownBy(() -> service.setComponent(1, 30, 4)).isInstanceOf(BusinessRuleViolationException.class);
        verify(repo, never()).saveItem(any());
    }

    private ProductVariant variant(long id, ProductType type) {
        var v = new ProductVariant();
        ReflectionTestUtils.setField(v, "id", id);
        v.setActive(false);
        var p = new Product();
        p.setProductType(type);
        v.setProduct(p);
        return v;
    }
}
