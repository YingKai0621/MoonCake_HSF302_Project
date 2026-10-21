package com.se201411.project.mooncake.cart.service.impl;

import com.se201411.project.mooncake.cart.dto.CartItemView;
import com.se201411.project.mooncake.cart.dto.CustomerCartView;
import com.se201411.project.mooncake.cart.repository.CartItemRepository;
import com.se201411.project.mooncake.cart.repository.CartRepository;
import com.se201411.project.mooncake.cart.service.CustomerCartService;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ConcurrentOperationException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Cart;
import com.se201411.project.mooncake.entity.CartItem;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.enums.CartStatus;
import com.se201411.project.mooncake.product.dto.CartVariantSnapshot;
import com.se201411.project.mooncake.product.service.ProductQueryService;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CustomerCartServiceImpl implements CustomerCartService {

    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductQueryService productQueryService;
    private final EntityManager entityManager;
    private final Clock clock;

    public CustomerCartServiceImpl(
        CartRepository cartRepository,
        CartItemRepository cartItemRepository,
        ProductQueryService productQueryService,
        EntityManager entityManager,
        Clock clock
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productQueryService = productQueryService;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Override
    public CustomerCartView getCurrentCart(ShopperIdentity customerIdentity) {
        requireIdentity(customerIdentity);

        Optional<Cart> cartResult = cartRepository.findByCustomerIdAndStatus(
            customerIdentity.customerId(),
            CartStatus.ACTIVE
        );
        if (cartResult.isEmpty()) {
            return CustomerCartView.empty();
        }

        Cart cart = cartResult.get();
        List<CartItem> cartItems = cartItemRepository.findVariantItemsByCartId(cart.getId());
        return buildCartView(cart, cartItems);
    }

    @Override
    public int countCurrentItems(ShopperIdentity customerIdentity) {
        requireIdentity(customerIdentity);

        Optional<Cart> cartResult = cartRepository.findByCustomerIdAndStatus(
            customerIdentity.customerId(),
            CartStatus.ACTIVE
        );
        if (cartResult.isEmpty()) {
            return 0;
        }

        long totalQuantity = cartItemRepository.sumQuantityByCartId(cartResult.get().getId());
        if (totalQuantity > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) totalQuantity;
    }

    @Override
    @Transactional
    public void addVariant(
        ShopperIdentity customerIdentity,
        long variantId,
        int quantity
    ) {
        requireIdentity(customerIdentity);
        requirePositiveId(variantId, "SKU không hợp lệ");
        requirePositiveQuantity(quantity);

        productQueryService.requireSellableVariant(variantId);
        Cart cart = lockOrCreateActiveCart(customerIdentity.customerId());

        Optional<CartItem> existingItem = cartItemRepository.findVariantItem(
            cart.getId(),
            variantId
        );
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(addQuantities(item.getQuantity(), quantity));
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            ProductVariant variantReference = entityManager.getReference(
                ProductVariant.class,
                variantId
            );
            item.setVariant(variantReference);
            item.setQuantity(quantity);
            item.setAddedAt(now());
            cartItemRepository.save(item);
        }

        cart.setUpdatedAt(now());
    }

    @Override
    @Transactional
    public void updateItemQuantity(
        ShopperIdentity customerIdentity,
        long cartItemId,
        int quantity
    ) {
        requireIdentity(customerIdentity);
        requirePositiveId(cartItemId, "Dòng giỏ hàng không hợp lệ");
        requirePositiveQuantity(quantity);

        Cart cart = requireLockedActiveCart(customerIdentity.customerId());
        CartItem item = requireOwnedItem(cart.getId(), cartItemId);

        if (item.getVariant() == null) {
            throw new BusinessRuleViolationException(
                "Dòng hộp tự chọn sẽ được xử lý trong chức năng CUSTOM"
            );
        }
        if (quantity > item.getQuantity()) {
            productQueryService.requireSellableVariant(item.getVariant().getId());
        }

        item.setQuantity(quantity);
        cart.setUpdatedAt(now());
    }

    @Override
    @Transactional
    public void removeItem(ShopperIdentity customerIdentity, long cartItemId) {
        requireIdentity(customerIdentity);
        requirePositiveId(cartItemId, "Dòng giỏ hàng không hợp lệ");

        Cart cart = requireLockedActiveCart(customerIdentity.customerId());
        CartItem item = requireOwnedItem(cart.getId(), cartItemId);
        if (item.getConfigurationId() != null) {
            throw new BusinessRuleViolationException(
                "Dòng hộp tự chọn sẽ được xử lý trong chức năng CUSTOM"
            );
        }
        cartItemRepository.delete(item);
        cart.setUpdatedAt(now());
    }

    private CustomerCartView buildCartView(Cart cart, List<CartItem> cartItems) {
        if (cartItems.isEmpty()) {
            return new CustomerCartView(
                cart.getId(),
                List.of(),
                0,
                BigDecimal.ZERO,
                formatVnd(BigDecimal.ZERO),
                false
            );
        }

        List<Long> variantIds = new ArrayList<>();
        for (CartItem item : cartItems) {
            variantIds.add(item.getVariant().getId());
        }

        List<CartVariantSnapshot> variantSnapshots = productQueryService
            .findVariantsForCart(variantIds);
        Map<Long, CartVariantSnapshot> variantsById = new LinkedHashMap<>();
        for (CartVariantSnapshot snapshot : variantSnapshots) {
            variantsById.put(snapshot.variantId(), snapshot);
        }

        List<CartItemView> itemViews = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean hasUnavailableItems = false;

        for (CartItem item : cartItems) {
            long variantId = item.getVariant().getId();
            CartVariantSnapshot variant = variantsById.get(variantId);
            if (variant == null) {
                throw new ResourceNotFoundException(
                    "Không tìm thấy thông tin SKU trong giỏ: " + variantId
                );
            }

            BigDecimal lineTotal = variant.unitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
            CartItemView itemView = new CartItemView(
                item.getId(),
                variant.variantId(),
                variant.productSlug(),
                variant.productName(),
                variant.variantName(),
                variant.sku(),
                variant.productType(),
                variant.imageUrl(),
                variant.imageAlt(),
                item.getQuantity(),
                variant.unitPrice(),
                formatVnd(variant.unitPrice()),
                lineTotal,
                formatVnd(lineTotal),
                variant.currentlySellable()
            );
            itemViews.add(itemView);

            totalQuantity = addQuantities(totalQuantity, item.getQuantity());
            subtotal = subtotal.add(lineTotal);
            if (!variant.currentlySellable()) {
                hasUnavailableItems = true;
            }
        }

        return new CustomerCartView(
            cart.getId(),
            itemViews,
            totalQuantity,
            subtotal,
            formatVnd(subtotal),
            hasUnavailableItems
        );
    }

    private Cart lockOrCreateActiveCart(long customerId) {
        Optional<Cart> existingCart = cartRepository.findByCustomerIdAndStatusForUpdate(
            customerId,
            CartStatus.ACTIVE
        );
        if (existingCart.isPresent()) {
            return existingCart.get();
        }

        cartRepository.createActiveCartIfMissing(customerId);
        Optional<Cart> createdCart = cartRepository.findByCustomerIdAndStatusForUpdate(
            customerId,
            CartStatus.ACTIVE
        );
        if (createdCart.isEmpty()) {
            throw new ConcurrentOperationException(
                "Không thể tạo giỏ hàng lúc này, vui lòng thử lại"
            );
        }
        return createdCart.get();
    }

    private Cart requireLockedActiveCart(long customerId) {
        Optional<Cart> cartResult = cartRepository.findByCustomerIdAndStatusForUpdate(
            customerId,
            CartStatus.ACTIVE
        );
        if (cartResult.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy giỏ hàng đang hoạt động");
        }
        return cartResult.get();
    }

    private CartItem requireOwnedItem(long cartId, long cartItemId) {
        Optional<CartItem> itemResult = cartItemRepository.findByIdAndCartId(
            cartItemId,
            cartId
        );
        if (itemResult.isEmpty()) {
            throw new ResourceNotFoundException(
                "Không tìm thấy sản phẩm trong giỏ hàng của bạn"
            );
        }
        return itemResult.get();
    }

    private int addQuantities(int first, int second) {
        try {
            return Math.addExact(first, second);
        } catch (ArithmeticException exception) {
            throw new BusinessRuleViolationException("Số lượng sản phẩm quá lớn");
        }
    }

    private void requireIdentity(ShopperIdentity customerIdentity) {
        if (customerIdentity == null) {
            throw new IllegalArgumentException("Customer identity must not be null");
        }
    }

    private void requirePositiveId(long id, String message) {
        if (id <= 0) {
            throw new BusinessRuleViolationException(message);
        }
    }

    private void requirePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleViolationException("Số lượng phải lớn hơn 0");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String formatVnd(BigDecimal amount) {
        return NumberFormat.getNumberInstance(VIETNAMESE).format(amount) + "đ";
    }
}
