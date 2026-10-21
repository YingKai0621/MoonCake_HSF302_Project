package com.se201411.project.mooncake.cart.service;

import com.se201411.project.mooncake.cart.repository.CartItemRepository;
import com.se201411.project.mooncake.cart.repository.CartRepository;
import com.se201411.project.mooncake.cart.service.impl.CartServiceImpl;
import com.se201411.project.mooncake.common.exception.ConcurrentOperationException;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartServiceImpl(cartRepository, cartItemRepository);
    }

    @Test
    void failedClaimStopsBeforeReadingCartLines() {
        ShopperIdentity customer = ShopperIdentity.forCustomer(9L);
        when(cartRepository.claimForCheckout(4L, 9L)).thenReturn(0);

        assertThatThrownBy(() -> cartService.claimForCheckout(4L, customer))
            .isInstanceOf(ConcurrentOperationException.class)
            .hasMessageContaining("đã được thanh toán");

        verify(cartItemRepository, never()).findVariantItemsByCartId(4L);
        verify(cartItemRepository, never()).findCustomItemsByCartId(4L);
    }
}
