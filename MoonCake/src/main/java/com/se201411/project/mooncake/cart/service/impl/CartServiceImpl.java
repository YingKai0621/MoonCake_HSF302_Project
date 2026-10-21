package com.se201411.project.mooncake.cart.service.impl;

import com.se201411.project.mooncake.cart.dto.CartLineSnapshot;
import com.se201411.project.mooncake.cart.dto.CartSnapshot;
import com.se201411.project.mooncake.cart.dto.CheckoutCartSnapshot;
import com.se201411.project.mooncake.cart.repository.CartItemRepository;
import com.se201411.project.mooncake.cart.repository.CartRepository;
import com.se201411.project.mooncake.cart.service.CartService;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ConcurrentOperationException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Cart;
import com.se201411.project.mooncake.entity.CartItem;
import com.se201411.project.mooncake.enums.CartStatus;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public CartServiceImpl(
        CartRepository cartRepository,
        CartItemRepository cartItemRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @Override
    public CheckoutCartSnapshot requireCurrentActiveCart(
        ShopperIdentity customerIdentity
    ) {
        requireIdentity(customerIdentity);
        Optional<Cart> cartResult = cartRepository.findByCustomerIdAndStatus(
            customerIdentity.customerId(),
            CartStatus.ACTIVE
        );
        if (cartResult.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy giỏ hàng đang hoạt động");
        }
        Cart cart = cartResult.get();

        return buildCheckoutSnapshot(cart.getId(), customerIdentity);
    }

    @Override
    public CartSnapshot requireActiveCart(
        long cartId,
        ShopperIdentity customerIdentity
    ) {
        requireCartId(cartId);
        requireIdentity(customerIdentity);
        requireOwnedActiveCart(cartId, customerIdentity.customerId());

        List<CartLineSnapshot> lines = loadLines(cartId);
        requireNotEmpty(lines);
        return new CartSnapshot(cartId, customerIdentity, lines);
    }

    @Override
    @Transactional
    public CheckoutCartSnapshot claimForCheckout(
        long cartId,
        ShopperIdentity customerIdentity
    ) {
        requireCartId(cartId);
        requireIdentity(customerIdentity);

        int updatedRows = cartRepository.claimForCheckout(
            cartId,
            customerIdentity.customerId()
        );
        if (updatedRows != 1) {
            throw new ConcurrentOperationException(
                "Giỏ hàng đã được thanh toán hoặc không còn hoạt động"
            );
        }

        return buildCheckoutSnapshot(cartId, customerIdentity);
    }

    private CheckoutCartSnapshot buildCheckoutSnapshot(
        long cartId,
        ShopperIdentity customerIdentity
    ) {
        List<CartLineSnapshot> lines = loadLines(cartId);
        requireNotEmpty(lines);
        return new CheckoutCartSnapshot(cartId, customerIdentity, lines);
    }

    private List<CartLineSnapshot> loadLines(long cartId) {
        List<CartLineSnapshot> lines = new ArrayList<>();

        List<CartItem> variantItems = cartItemRepository.findVariantItemsByCartId(cartId);
        for (CartItem item : variantItems) {
            lines.add(new CartLineSnapshot(
                item.getVariant().getId(),
                null,
                item.getQuantity()
            ));
        }

        List<CartItem> customItems = cartItemRepository.findCustomItemsByCartId(cartId);
        for (CartItem item : customItems) {
            lines.add(new CartLineSnapshot(
                null,
                item.getConfigurationId(),
                item.getQuantity()
            ));
        }

        return List.copyOf(lines);
    }

    private void requireOwnedActiveCart(long cartId, long customerId) {
        if (cartRepository.findOwnedCart(cartId, customerId, CartStatus.ACTIVE).isEmpty()) {
            throw new ResourceNotFoundException(
                "Không tìm thấy giỏ hàng đang hoạt động của bạn"
            );
        }
    }

    private void requireNotEmpty(List<CartLineSnapshot> lines) {
        if (lines.isEmpty()) {
            throw new BusinessRuleViolationException("Giỏ hàng đang trống");
        }
    }

    private void requireIdentity(ShopperIdentity customerIdentity) {
        if (customerIdentity == null) {
            throw new ResourceNotFoundException("Không tìm thấy khách hàng hiện tại");
        }
    }

    private void requireCartId(long cartId) {
        if (cartId <= 0) {
            throw new ResourceNotFoundException("Giỏ hàng không hợp lệ");
        }
    }
}
