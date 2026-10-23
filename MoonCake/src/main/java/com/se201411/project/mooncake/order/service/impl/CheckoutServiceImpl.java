package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.cart.dto.CartSnapshot;
import com.se201411.project.mooncake.cart.dto.CheckoutCartSnapshot;
import com.se201411.project.mooncake.cart.dto.CheckoutComponentQuote;
import com.se201411.project.mooncake.cart.dto.CheckoutLineQuote;
import com.se201411.project.mooncake.cart.dto.CheckoutQuote;
import com.se201411.project.mooncake.cart.service.CartService;
import com.se201411.project.mooncake.cart.service.PricingService;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.entity.OrderItem;
import com.se201411.project.mooncake.entity.OrderItemComponent;
import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.entity.VolumeDiscountTier;
import com.se201411.project.mooncake.enums.InventorySource;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.OrderType;
import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.inventory.dto.InventoryDemand;
import com.se201411.project.mooncake.inventory.dto.SaleInventoryCommand;
import com.se201411.project.mooncake.inventory.service.InventoryService;
import com.se201411.project.mooncake.order.dto.CheckoutForm;
import com.se201411.project.mooncake.order.dto.CheckoutPreparation;
import com.se201411.project.mooncake.order.dto.CheckoutResult;
import com.se201411.project.mooncake.order.dto.OrderSuccessItemView;
import com.se201411.project.mooncake.order.dto.OrderSuccessView;
import com.se201411.project.mooncake.order.repository.OrderItemComponentRepository;
import com.se201411.project.mooncake.order.repository.OrderItemRepository;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import com.se201411.project.mooncake.order.repository.SalesOrderRepository;
import com.se201411.project.mooncake.order.service.CheckoutService;
import com.se201411.project.mooncake.order.service.DeliveryAddressService;
import com.se201411.project.mooncake.security.dto.CustomerProfile;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.CustomerProfileService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CheckoutServiceImpl implements CheckoutService {

    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CartService cartService;
    private final PricingService pricingService;
    private final InventoryService inventoryService;
    private final CustomerProfileService customerProfileService;
    private final DeliveryAddressService deliveryAddressService;
    private final SalesOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemComponentRepository componentRepository;
    private final PaymentRepository paymentRepository;
    private final EntityManager entityManager;
    private final Clock clock;

    public CheckoutServiceImpl(
        CartService cartService,
        PricingService pricingService,
        InventoryService inventoryService,
        CustomerProfileService customerProfileService,
        DeliveryAddressService deliveryAddressService,
        SalesOrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        OrderItemComponentRepository componentRepository,
        PaymentRepository paymentRepository,
        EntityManager entityManager,
        Clock clock
    ) {
        this.cartService = cartService;
        this.pricingService = pricingService;
        this.inventoryService = inventoryService;
        this.customerProfileService = customerProfileService;
        this.deliveryAddressService = deliveryAddressService;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.componentRepository = componentRepository;
        this.paymentRepository = paymentRepository;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Override
    public CheckoutPreparation prepareCheckout(ShopperIdentity customerIdentity) {
        CheckoutCartSnapshot cart = cartService.requireCurrentActiveCart(customerIdentity);
        CheckoutQuote quote = pricingService.calculateCheckoutQuote(cart);
        CustomerProfile customer = customerProfileService.requireProfile(customerIdentity);
        return new CheckoutPreparation(cart.cartId(), customer, quote);
    }

    @Override
    public CheckoutPreparation prepareCheckout(
        long cartId,
        ShopperIdentity customerIdentity
    ) {
        CartSnapshot cart = cartService.requireActiveCart(cartId, customerIdentity);
        CheckoutCartSnapshot checkoutCart = new CheckoutCartSnapshot(
            cart.cartId(),
            cart.customerIdentity(),
            cart.lines()
        );
        CheckoutQuote quote = pricingService.calculateCheckoutQuote(checkoutCart);
        CustomerProfile customer = customerProfileService.requireProfile(customerIdentity);
        return new CheckoutPreparation(cart.cartId(), customer, quote);
    }

    @Override
    @Transactional
    public CheckoutResult placeOrder(
        ShopperIdentity customerIdentity,
        CheckoutForm form
    ) {
        validateForm(form);
        String shippingAddress = deliveryAddressService.buildShippingAddress(
            form.getStreetAddress(),
            form.getWardCode(),
            form.getProvinceCode()
        );

        CheckoutCartSnapshot cart = cartService.claimForCheckout(
            form.getCartId(),
            customerIdentity
        );
        CheckoutQuote quote = pricingService.calculateCheckoutQuote(cart);
        CustomerProfile customer = customerProfileService.requireProfile(customerIdentity);
        LocalDateTime now = LocalDateTime.now(clock);

        SalesOrder order = createOrder(customer, form, shippingAddress, quote, now);
        orderRepository.save(order);

        saveOrderItems(order, quote);
        savePayment(order, quote.total(), now);

        List<InventoryDemand> demands = buildInventoryDemands(quote);
        inventoryService.deductForSale(new SaleInventoryCommand(
            order.getId(),
            "SALE:" + order.getId(),
            demands
        ));

        return new CheckoutResult(order.getOrderCode());
    }

    @Override
    public OrderSuccessView requireOrder(
        String orderCode,
        ShopperIdentity customerIdentity
    ) {
        if (orderCode == null || orderCode.isBlank()) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng");
        }

        Optional<SalesOrder> orderResult = orderRepository.findOwnedByOrderCode(
            orderCode.trim(),
            customerIdentity.customerId()
        );
        if (orderResult.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng của bạn");
        }
        SalesOrder order = orderResult.get();

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        List<OrderSuccessItemView> itemViews = new ArrayList<>();
        int totalQuantity = 0;
        for (OrderItem item : orderItems) {
            totalQuantity = Math.addExact(totalQuantity, item.getQuantity());
            String sku = item.getSkuSnapshot();
            if (sku == null) {
                sku = item.getBoxSkuSnapshot();
            }
            itemViews.add(new OrderSuccessItemView(
                item.getProductNameSnapshot(),
                sku,
                item.getQuantity(),
                formatVnd(item.getUnitFinalPriceSnapshot()),
                formatVnd(item.getLineTotal())
            ));
        }

        return new OrderSuccessView(
            order.getOrderCode(),
            order.getRecipientName(),
            order.getRecipientPhone(),
            order.getShippingAddressSnapshot(),
            order.getOrderNote(),
            itemViews,
            totalQuantity,
            formatVnd(order.getSubtotalAmount()),
            formatVnd(order.getDiscountAmount()),
            formatVnd(order.getShippingFee()),
            formatSignedVnd(order.getRoundingAdjustment()),
            formatVnd(order.getTotalAmount())
        );
    }

    private SalesOrder createOrder(
        CustomerProfile customer,
        CheckoutForm form,
        String shippingAddress,
        CheckoutQuote quote,
        LocalDateTime now
    ) {
        SalesOrder order = new SalesOrder();
        order.setOrderCode(generateOrderCode());
        order.setCustomer(entityManager.getReference(Customer.class, customer.customerId()));
        order.setOrderType(OrderType.RETAIL);
        order.setBuyerNameSnapshot(customer.fullName());
        order.setBuyerPhoneSnapshot(customer.phone());
        order.setBuyerEmailSnapshot(customer.email());
        order.setRecipientName(form.getRecipientName().trim());
        order.setRecipientPhone(form.getRecipientPhone());
        order.setShippingAddressSnapshot(shippingAddress);
        order.setOrderNote(normalizeOptional(form.getOrderNote()));
        order.setSubtotalAmount(quote.subtotal());
        order.setDiscountAmount(quote.discount());
        order.setShippingFee(quote.shippingFee());
        order.setRoundingAdjustment(quote.roundingAdjustment());
        order.setTotalAmount(quote.total());
        order.setStatus(OrderStatus.PLACED);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        return order;
    }

    private void saveOrderItems(SalesOrder order, CheckoutQuote quote) {
        for (CheckoutLineQuote line : quote.lines()) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            if (line.cartLine().variantId() != null) {
                item.setVariant(entityManager.getReference(
                    ProductVariant.class,
                    line.cartLine().variantId()
                ));
            } else {
                item.setConfiguration(entityManager.getReference(
                    BoxConfiguration.class,
                    line.cartLine().configurationId()
                ));
            }
            if (line.volumeDiscountTierId() != null) {
                item.setVolumeDiscountTier(entityManager.getReference(
                    VolumeDiscountTier.class,
                    line.volumeDiscountTierId()
                ));
            }
            item.setItemKind(line.itemKind());
            item.setInventorySourceSnapshot(line.inventorySource());
            item.setProductNameSnapshot(line.productName());
            item.setSkuSnapshot(line.sku());
            item.setDiscountGroupSnapshot(line.discountGroup());
            item.setBoxNameSnapshot(line.boxName());
            item.setBoxSkuSnapshot(line.boxSku());
            item.setQuantity(line.cartLine().quantity());
            item.setUnitBasePriceSnapshot(line.unitBasePrice());
            item.setDiscountPercentSnapshot(line.discountPercent());
            item.setUnitDiscountSnapshot(line.unitDiscount());
            item.setUnitFinalPriceSnapshot(line.unitFinalPrice());
            item.setLineTotal(line.lineTotal());
            orderItemRepository.save(item);

            saveComponents(item, line.components());
        }
    }

    private void saveComponents(
        OrderItem item,
        List<CheckoutComponentQuote> componentQuotes
    ) {
        for (CheckoutComponentQuote quote : componentQuotes) {
            OrderItemComponent component = new OrderItemComponent();
            component.setOrderItem(item);
            component.setComponentVariant(entityManager.getReference(
                ProductVariant.class,
                quote.variantId()
            ));
            component.setComponentRole(quote.role());
            component.setComponentNameSnapshot(quote.name());
            component.setQuantityPerSet(quote.quantityPerSet());
            component.setUnitPriceSnapshot(quote.unitPrice());
            componentRepository.save(component);
        }
    }

    private void savePayment(
        SalesOrder order,
        BigDecimal total,
        LocalDateTime now
    ) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(PaymentMethod.COD);
        payment.setStatus(PaymentStatus.UNPAID);
        payment.setAmount(total);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        paymentRepository.save(payment);
    }

    private List<InventoryDemand> buildInventoryDemands(CheckoutQuote quote) {
        Map<Long, Integer> quantityByVariant = new LinkedHashMap<>();

        for (CheckoutLineQuote line : quote.lines()) {
            int setQuantity = line.cartLine().quantity();
            if (line.inventorySource() == InventorySource.STOCKED_SKU) {
                addDemand(quantityByVariant, line.cartLine().variantId(), setQuantity);
                continue;
            }

            for (CheckoutComponentQuote component : line.components()) {
                int componentQuantity = Math.multiplyExact(
                    component.quantityPerSet(),
                    setQuantity
                );
                addDemand(quantityByVariant, component.variantId(), componentQuantity);
            }
        }

        List<InventoryDemand> demands = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantityByVariant.entrySet()) {
            demands.add(new InventoryDemand(entry.getKey(), entry.getValue()));
        }
        return List.copyOf(demands);
    }

    private void addDemand(
        Map<Long, Integer> quantityByVariant,
        long variantId,
        int quantity
    ) {
        int current = quantityByVariant.getOrDefault(variantId, 0);
        quantityByVariant.put(variantId, Math.addExact(current, quantity));
    }

    private void validateForm(CheckoutForm form) {
        if (form == null || form.getCartId() == null || form.getCartId() <= 0) {
            throw new BusinessRuleViolationException("Giỏ hàng không hợp lệ");
        }
        if (isBlank(form.getRecipientName())) {
            throw new BusinessRuleViolationException("Vui lòng nhập tên người nhận");
        }
        if (form.getRecipientPhone() == null
            || !form.getRecipientPhone().matches("^0[0-9]{9}$")) {
            throw new BusinessRuleViolationException(
                "Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng 0"
            );
        }
        if (isBlank(form.getProvinceCode())
            || isBlank(form.getWardCode())
            || isBlank(form.getStreetAddress())) {
            throw new BusinessRuleViolationException("Vui lòng nhập đầy đủ địa chỉ nhận hàng");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String generateOrderCode() {
        String date = LocalDate.now(clock).format(ORDER_DATE);
        String randomPart = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, 8)
            .toUpperCase(Locale.ROOT);
        return "MC-" + date + "-" + randomPart;
    }

    private String formatVnd(BigDecimal amount) {
        return NumberFormat.getNumberInstance(VIETNAMESE).format(amount) + "đ";
    }

    private String formatSignedVnd(BigDecimal amount) {
        if (amount.signum() == 0) {
            return null;
        }
        if (amount.signum() > 0) {
            return "+ " + formatVnd(amount);
        }
        return "- " + formatVnd(amount.abs());
    }
}
