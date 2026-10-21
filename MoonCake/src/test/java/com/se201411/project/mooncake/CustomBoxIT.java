package com.se201411.project.mooncake;

import com.se201411.project.mooncake.cart.dto.CustomCakeQuantity;
import com.se201411.project.mooncake.cart.dto.SaveCustomBoxCommand;
import com.se201411.project.mooncake.cart.service.CustomBoxService;
import com.se201411.project.mooncake.cart.service.CustomerCartService;
import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.enums.AccountStatus;
import com.se201411.project.mooncake.security.auth.MoonCakeUserPrincipal;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@ContextConfiguration(initializers = MoonCakeApplicationIT.TestDatabaseGuard.class)
@Transactional
class CustomBoxIT {

    @Autowired
    private CustomBoxService customBoxService;

    @Autowired
    private CustomerCartService customerCartService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rendersCustomBoxBuilderForCustomer() throws Exception {
        MoonCakeUserPrincipal customer = new MoonCakeUserPrincipal(
            999L,
            999L,
            "custom-box-page@mooncake.test",
            "test-hash",
            AccountRole.CUSTOMER,
            AccountStatus.ACTIVE
        );

        mockMvc.perform(get("/custom-boxes").with(user(customer)))
            .andExpect(status().isOk())
            .andExpect(view().name("custom-box"))
            .andExpect(content().string(
                org.hamcrest.Matchers.containsString("Tự chọn hộp quà")
            ));
    }

    @Test
    void invalidRequestWithoutCakeSelectionsRendersValidationMessage() throws Exception {
        long customerId = createCustomer();
        Long accountId = jdbcTemplate.queryForObject(
            "SELECT account_id FROM dbo.CUSTOMER WHERE customer_id = ?",
            Long.class,
            customerId
        );
        MoonCakeUserPrincipal customer = new MoonCakeUserPrincipal(
            accountId,
            customerId,
            "custom-box-it@mooncake.test",
            "test-hash",
            AccountRole.CUSTOMER,
            AccountStatus.ACTIVE
        );
        long boxId = requireLong("""
            SELECT box.box_id
            FROM dbo.BOX AS box
            JOIN dbo.PRODUCT_VARIANT AS variant
                ON variant.variant_id = box.empty_box_variant_id
            WHERE variant.sku = N'BOX-KIM-PHUC-4'
            """);

        mockMvc.perform(
            post("/custom-boxes")
                .with(user(customer))
                .with(csrf())
                .param("boxId", Long.toString(boxId))
                .param("boxQuantity", "1")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("custom-box"))
            .andExpect(content().string(
                org.hamcrest.Matchers.containsString("phải chọn đúng 4 bánh")
            ));
    }

    @Test
    void managesCustomBoxCartLifecycleWithoutChangingInventory() {
        long customerId = createCustomer();
        ShopperIdentity customer = ShopperIdentity.forCustomer(customerId);
        long boxId = requireLong("""
            SELECT box.box_id
            FROM dbo.BOX AS box
            JOIN dbo.PRODUCT_VARIANT AS variant
                ON variant.variant_id = box.empty_box_variant_id
            WHERE variant.sku = N'BOX-KIM-PHUC-4'
            """);
        long firstCakeId = requireLong("""
            SELECT variant_id
            FROM dbo.PRODUCT_VARIANT
            WHERE sku = N'MC-LXGQXO-150'
            """);
        long secondCakeId = requireLong("""
            SELECT variant_id
            FROM dbo.PRODUCT_VARIANT
            WHERE sku = N'MC-HSMC-120'
            """);

        Integer inventoryBefore = jdbcTemplate.queryForObject("""
            SELECT SUM(balance.available_quantity)
            FROM dbo.INVENTORY_BALANCE AS balance
            WHERE balance.variant_id IN (?, ?, (
                SELECT empty_box_variant_id FROM dbo.BOX WHERE box_id = ?
            ))
            """, Integer.class, firstCakeId, secondCakeId, boxId);

        SaveCustomBoxCommand command = new SaveCustomBoxCommand(
            boxId,
            2,
            List.of(
                new CustomCakeQuantity(firstCakeId, 2),
                new CustomCakeQuantity(secondCakeId, 2)
            )
        );
        long configurationId = customBoxService.createAndAddToCart(
            customer,
            command
        );
        entityManager.flush();

        Integer readyConfigurationCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM dbo.BOX_CONFIGURATION
            WHERE configuration_id = ?
              AND configuration_type = 'CUSTOM'
              AND configuration_status = 'READY'
              AND cart_id IS NOT NULL
              AND preset_variant_id IS NULL
            """, Integer.class, configurationId);
        Integer componentCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM dbo.BOX_CONFIGURATION_ITEM
            WHERE configuration_id = ?
            """, Integer.class, configurationId);
        Integer cakeCount = jdbcTemplate.queryForObject("""
            SELECT SUM(quantity_per_box)
            FROM dbo.BOX_CONFIGURATION_ITEM
            WHERE configuration_id = ?
            """, Integer.class, configurationId);
        Integer cartLineQuantity = jdbcTemplate.queryForObject("""
            SELECT quantity
            FROM dbo.CART_ITEM
            WHERE configuration_id = ?
              AND variant_id IS NULL
            """, Integer.class, configurationId);
        Integer inventoryAfter = jdbcTemplate.queryForObject("""
            SELECT SUM(balance.available_quantity)
            FROM dbo.INVENTORY_BALANCE AS balance
            WHERE balance.variant_id IN (?, ?, (
                SELECT empty_box_variant_id FROM dbo.BOX WHERE box_id = ?
            ))
            """, Integer.class, firstCakeId, secondCakeId, boxId);

        assertThat(readyConfigurationCount).isEqualTo(1);
        assertThat(componentCount).isEqualTo(2);
        assertThat(cakeCount).isEqualTo(4);
        assertThat(cartLineQuantity).isEqualTo(2);
        assertThat(inventoryAfter).isEqualTo(inventoryBefore);

        SaveCustomBoxCommand updateCommand = new SaveCustomBoxCommand(
            boxId,
            3,
            List.of(new CustomCakeQuantity(firstCakeId, 4))
        );
        customBoxService.updateConfiguration(customer, configurationId, updateCommand);
        entityManager.flush();

        Integer updatedComponentCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM dbo.BOX_CONFIGURATION_ITEM
            WHERE configuration_id = ?
            """, Integer.class, configurationId);
        Integer updatedCakeCount = jdbcTemplate.queryForObject("""
            SELECT SUM(quantity_per_box)
            FROM dbo.BOX_CONFIGURATION_ITEM
            WHERE configuration_id = ?
            """, Integer.class, configurationId);
        Long cartItemId = jdbcTemplate.queryForObject("""
            SELECT cart_item_id
            FROM dbo.CART_ITEM
            WHERE configuration_id = ?
              AND quantity = 3
            """, Long.class, configurationId);

        assertThat(updatedComponentCount).isEqualTo(1);
        assertThat(updatedCakeCount).isEqualTo(4);
        assertThat(cartItemId).isNotNull();

        customerCartService.removeItem(customer, cartItemId);
        entityManager.flush();

        Integer remainingConfigurationCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM dbo.BOX_CONFIGURATION
            WHERE configuration_id = ?
            """, Integer.class, configurationId);
        Integer inventoryAfterRemoval = jdbcTemplate.queryForObject("""
            SELECT SUM(balance.available_quantity)
            FROM dbo.INVENTORY_BALANCE AS balance
            WHERE balance.variant_id IN (?, ?, (
                SELECT empty_box_variant_id FROM dbo.BOX WHERE box_id = ?
            ))
            """, Integer.class, firstCakeId, secondCakeId, boxId);

        assertThat(remainingConfigurationCount).isZero();
        assertThat(inventoryAfterRemoval).isEqualTo(inventoryBefore);
    }

    private long createCustomer() {
        Long accountId = jdbcTemplate.queryForObject("""
            INSERT INTO dbo.ACCOUNT (email, password_hash, role, status, created_at)
            OUTPUT INSERTED.account_id
            VALUES (?, ?, 'CUSTOMER', 'ACTIVE', SYSUTCDATETIME())
            """, Long.class, "custom-box-it@mooncake.test", "test-hash");

        Long customerId = jdbcTemplate.queryForObject("""
            INSERT INTO dbo.CUSTOMER (account_id, full_name, phone, email, created_at)
            OUTPUT INSERTED.customer_id
            VALUES (?, N'Custom Box Integration Test', '0900000001', ?, SYSUTCDATETIME())
            """, Long.class, accountId, "custom-box-it@mooncake.test");
        return customerId;
    }

    private long requireLong(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        if (value == null) {
            throw new IllegalStateException("Expected SQL query to return one ID");
        }
        return value;
    }
}
