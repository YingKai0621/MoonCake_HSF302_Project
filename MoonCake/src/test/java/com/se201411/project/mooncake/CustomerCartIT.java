package com.se201411.project.mooncake;

import com.se201411.project.mooncake.cart.dto.CustomerCartView;
import com.se201411.project.mooncake.cart.service.CustomerCartService;
import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.security.dto.CustomerRegistrationForm;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.repository.AccountRepository;
import com.se201411.project.mooncake.security.repository.CustomerRepository;
import com.se201411.project.mooncake.security.service.AuthenticationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@ContextConfiguration(initializers = MoonCakeApplicationIT.TestDatabaseGuard.class)
@Transactional
class CustomerCartIT {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerCartService customerCartService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void repeatedAddUsesOneCartLineAndDoesNotChangeInventory() {
        Customer customer = registerCustomer();
        ShopperIdentity identity = ShopperIdentity.forCustomer(customer.getId());
        long variantId = findVariantId("MC-KSPM-080");
        int inventoryBefore = findInventoryQuantity(variantId);

        customerCartService.addVariant(identity, variantId, 2);
        customerCartService.addVariant(identity, variantId, 3);
        entityManager.flush();

        CustomerCartView cart = customerCartService.getCurrentCart(identity);
        long cartLineCount = countCartLines(customer.getId(), variantId);
        int inventoryAfter = findInventoryQuantity(variantId);

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().getFirst().quantity()).isEqualTo(5);
        assertThat(cart.totalQuantity()).isEqualTo(5);
        assertThat(cartLineCount).isEqualTo(1L);
        assertThat(inventoryAfter).isEqualTo(inventoryBefore);
    }

    private Customer registerCustomer() {
        String email = "cart-" + UUID.randomUUID() + "@example.com";
        CustomerRegistrationForm form = new CustomerRegistrationForm();
        form.setEmail(email);
        form.setPassword("password123");
        form.setConfirmPassword("password123");
        form.setFullName("Khách kiểm thử giỏ hàng");
        form.setPhone("0901234567");
        authenticationService.registerCustomer(form);

        Account account = accountRepository.findByEmail(email).orElseThrow();
        return customerRepository.findByAccount_Id(account.getId()).orElseThrow();
    }

    private long findVariantId(String sku) {
        Number result = (Number) entityManager.createNativeQuery("""
            SELECT variant_id
            FROM dbo.PRODUCT_VARIANT
            WHERE sku = :sku
            """)
            .setParameter("sku", sku)
            .getSingleResult();
        return result.longValue();
    }

    private int findInventoryQuantity(long variantId) {
        Number result = (Number) entityManager.createNativeQuery("""
            SELECT available_quantity
            FROM dbo.INVENTORY_BALANCE
            WHERE variant_id = :variantId
            """)
            .setParameter("variantId", variantId)
            .getSingleResult();
        return result.intValue();
    }

    private long countCartLines(long customerId, long variantId) {
        Number result = (Number) entityManager.createNativeQuery("""
            SELECT COUNT(*)
            FROM dbo.CART_ITEM AS item
            JOIN dbo.CART AS cart ON cart.cart_id = item.cart_id
            WHERE cart.customer_id = :customerId
              AND cart.cart_status = 'ACTIVE'
              AND item.variant_id = :variantId
            """)
            .setParameter("customerId", customerId)
            .setParameter("variantId", variantId)
            .getSingleResult();
        return result.longValue();
    }
}
