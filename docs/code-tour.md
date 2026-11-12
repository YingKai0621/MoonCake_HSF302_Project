# Tour đọc code MoonCake — chuẩn bị trả lời thầy

Không cần đọc hết. Nắm **1 quy tắc chung**, **cách lần theo 1 chức năng**, và **8 luồng chính** dưới đây là đủ chỉ cho thầy "chỗ nào xử lý cái gì".

Đường dẫn gốc: `MoonCake/src/main/java/com/se201411/project/mooncake/` (viết tắt `…/` bên dưới).

## 1. Quy tắc chung (nói trong 30 giây)

Mỗi chức năng đi qua 5 tầng, luôn theo một chiều:

```
Trình duyệt → Controller → Service (interface) → ServiceImpl → Repository → Entity (bảng)
                  │                                   │
                  └── nhận/trả DTO cho trang HTML ────┘
```

| Tầng | Làm gì | Ví dụ |
|---|---|---|
| **Controller** | Nhận request, gọi service, chọn trang trả về. **Không có luật nghiệp vụ, không gọi repository** | `CheckoutController` |
| **Service** (interface) | Hợp đồng: có những việc gì | `CheckoutService` |
| **ServiceImpl** | **Toàn bộ luật nghiệp vụ** và `@Transactional` | `CheckoutServiceImpl` |
| **Repository** | Đọc/ghi database (Spring Data JPA) | `SalesOrderRepository` |
| **Entity** | Ánh xạ 1 bảng | `SalesOrder` |
| **DTO** | Dữ liệu đưa lên trang / nhận từ form. Trang **không** nhận entity | `CheckoutForm`, `CheckoutPageView` |

Code chia theo **module**: `cart`, `order`, `product`, `inventory`, `security`, `feedback`, `dashboard`, `storefront`. Mỗi module có đủ `controller / service / service/impl / repository / dto`. Module này cần module khác thì **gọi service** của module kia, không đọc repository của nó.

## 2. Cách lần theo bất kỳ chức năng nào (khi thầy chỉ vào một nút)

1. Xem URL của nút/form (ví dụ `/checkout`).
2. Tìm `@PostMapping("/checkout")` hoặc `@GetMapping` → ra **controller**.
3. Controller gọi `xxxService.method(...)` → Ctrl+Click vào method → nhảy tới **interface** → Ctrl+Alt+B để tới **Impl**.
4. Đọc hàm trong Impl: đây là chỗ có luật.

## 3. Tám luồng chính

### 3.1 Đặt hàng (quan trọng nhất)
[`CheckoutServiceImpl.placeOrder`](../MoonCake/src/main/java/com/se201411/project/mooncake/order/service/impl/CheckoutServiceImpl.java#L174) — đọc từ trên xuống là thấy đủ các bước, **cùng một transaction**:

1. `requireOrderingAllowed` — chặn khách bỏ dở VNPay 3 lần/24h ([dòng 335](../MoonCake/src/main/java/com/se201411/project/mooncake/order/service/impl/CheckoutServiceImpl.java#L335)).
2. `paymentProcessorRegistry.forMethod(...)` — lấy bộ xử lý thanh toán COD hoặc VNPay (Strategy, xem 3.2).
3. `cartService.claimForCheckout` — khóa giỏ `ACTIVE → CHECKED_OUT`, bấm 2 lần không ra 2 đơn.
4. `pricingService.calculateCheckoutQuote` — **server tự tính** giá, chiết khấu, phí ship ([PricingServiceImpl](../MoonCake/src/main/java/com/se201411/project/mooncake/cart/service/impl/PricingServiceImpl.java#L80)).
5. `paymentProcessor.checkCanPlace` — luật giữ hàng: COD ≤ 5 triệu, tối đa 2 COD / 1 VNPay đang mở.
6. Lưu đơn, dòng hàng, thanh toán `UNPAID`.
7. `inventoryService.deductForSale` — **trừ kho ngay** (xem 3.4).
8. `startPayment` — VNPay trả link thanh toán, COD thì không.

### 3.2 Thanh toán (Strategy pattern — thầy hay hỏi)
- Interface [`PaymentProcessor`](../MoonCake/src/main/java/com/se201411/project/mooncake/order/service/PaymentProcessor.java#L13): `checkCanPlace`, `startPayment`, `checkCanCancel`…
- Hai cài đặt: [`CodPaymentProcessor`](../MoonCake/src/main/java/com/se201411/project/mooncake/order/service/impl/CodPaymentProcessor.java#L103), [`VnpayPaymentProcessor`](../MoonCake/src/main/java/com/se201411/project/mooncake/order/service/impl/VnpayPaymentProcessor.java#L368).
- [`PaymentProcessorRegistryImpl`](../MoonCake/src/main/java/com/se201411/project/mooncake/order/service/impl/PaymentProcessorRegistryImpl.java) chọn đúng bộ theo phương thức.
- **Không có `if (method == VNPAY)`** trong đặt hàng / hủy đơn. Thêm cổng mới chỉ cần thêm 1 class.
- VNPay chỉ thành **PAID** khi nhận IPN có chữ ký hợp lệ: [`VnpayController.ipn`](../MoonCake/src/main/java/com/se201411/project/mooncake/order/controller/VnpayController.java#L69) → [`handleNotification`](../MoonCake/src/main/java/com/se201411/project/mooncake/order/service/impl/VnpayPaymentProcessor.java#L200). Trang return chỉ hiện thông báo.

### 3.3 Giỏ hàng
[`CustomerCartServiceImpl.addVariant`](../MoonCake/src/main/java/com/se201411/project/mooncake/cart/service/impl/CustomerCartServiceImpl.java#L112): thêm cùng sản phẩm thì **tăng số lượng**, không thêm dòng trùng; kiểm tra tồn; chủ giỏ lấy từ tài khoản đang đăng nhập, không tin id do trình duyệt gửi.

### 3.4 Kho và lô hàng
- Bán: [`InventoryServiceImpl.deductForSale`](../MoonCake/src/main/java/com/se201411/project/mooncake/inventory/service/impl/InventoryServiceImpl.java#L98) — trừ tồn tổng bằng câu UPDATE có điều kiện (không âm), rồi trừ lô.
- Hủy: [`restoreForCancellation`](../MoonCake/src/main/java/com/se201411/project/mooncake/inventory/service/impl/InventoryServiceImpl.java#L125) — cộng lại đúng lô đã bán.
- Chọn lô: [`BatchStockServiceImpl.takeStock`](../MoonCake/src/main/java/com/se201411/project/mooncake/inventory/service/impl/BatchStockServiceImpl.java#L206) — **lô hết hạn sớm nhất trước**, bỏ qua lô đã hết hạn.
- Admin: [`InventoryAdminServiceImpl`](../MoonCake/src/main/java/com/se201411/project/mooncake/inventory/service/impl/InventoryAdminServiceImpl.java#L212) — `receive` (nhập, bánh bắt buộc NSX, HSD = NSX + số ngày), `adjust` (chỉ giảm), `pack` (đóng hộp).

### 3.5 Đóng hộp quà
[`InventoryAdminServiceImpl.pack`](../MoonCake/src/main/java/com/se201411/project/mooncake/inventory/service/impl/InventoryAdminServiceImpl.java#L267): trừ vỏ + bánh theo công thức, cộng hộp quà, tất cả một transaction. Sau đó [`BatchStockServiceImpl.finishPack`](../MoonCake/src/main/java/com/se201411/project/mooncake/inventory/service/impl/BatchStockServiceImpl.java#L89): HSD hộp quà = HSD sớm nhất của bánh; từ chối nếu bánh chưa có hạn.

### 3.6 Sản phẩm và hộp quà (admin)
- Phiên bản + sức chứa hộp: [`ProductAdminServiceImpl.saveVariant`](../MoonCake/src/main/java/com/se201411/project/mooncake/product/service/impl/ProductAdminServiceImpl.java#L468) → [`saveBoxCapacity`](../MoonCake/src/main/java/com/se201411/project/mooncake/product/service/impl/ProductAdminServiceImpl.java#L501).
- Tạo hộp quà kèm vỏ: [`GiftBoxAdminServiceImpl.create`](../MoonCake/src/main/java/com/se201411/project/mooncake/product/service/impl/GiftBoxAdminServiceImpl.java#L123).
- Chốt công thức: [`PresetConfigurationAdminServiceImpl.activate`](../MoonCake/src/main/java/com/se201411/project/mooncake/product/service/impl/PresetConfigurationAdminServiceImpl.java#L262) — tổng bánh phải đúng sức chứa.

### 3.7 Đơn hàng phía admin và việc tự động
- Chuyển trạng thái: [`OrderManagementServiceImpl.transition`](../MoonCake/src/main/java/com/se201411/project/mooncake/order/service/impl/OrderManagementServiceImpl.java#L118), giao hàng: [`ship`](../MoonCake/src/main/java/com/se201411/project/mooncake/order/service/impl/OrderManagementServiceImpl.java#L351).
- Hủy đơn: [`OrderCancellationServiceImpl`](../MoonCake/src/main/java/com/se201411/project/mooncake/order/service/impl/OrderCancellationServiceImpl.java#L100) — chuyển trạng thái có điều kiện, hoàn kho đúng 1 lần.
- Việc chạy định kỳ: [`UnpaidOrderExpiryJob`](../MoonCake/src/main/java/com/se201411/project/mooncake/order/scheduler/UnpaidOrderExpiryJob.java) (hủy VNPay quá 15 phút), [`DeliveredOrderCompletionJob`](../MoonCake/src/main/java/com/se201411/project/mooncake/order/scheduler/DeliveredOrderCompletionJob.java) (tự hoàn tất đơn đã giao quá hạn).

### 3.8 Đăng nhập và phân quyền
- [`SecurityConfig`](../MoonCake/src/main/java/com/se201411/project/mooncake/security/config/SecurityConfig.java#L51): xem sản phẩm thì ai cũng được; `/admin/**` cần ADMIN; giỏ, thanh toán, đơn cần CUSTOMER.
- Mỗi service admin gọi [`AdminAccessServiceImpl.requireAdmin`](../MoonCake/src/main/java/com/se201411/project/mooncake/security/service/impl/AdminAccessServiceImpl.java#L24) để kiểm tra lại ở tầng service.
- Quản trị viên: [`AdminAccountServiceImpl.lock`](../MoonCake/src/main/java/com/se201411/project/mooncake/security/service/impl/AdminAccountServiceImpl.java#L88) — chỉ super admin (mã nhỏ nhất) được khóa.

## 4. Câu thầy hay hỏi — trả lời và chỉ vào đâu

| Thầy hỏi | Trả lời ngắn | Chỉ vào |
|---|---|---|
| Controller có gọi repository không? | Không. Controller chỉ gọi service. | Mở bất kỳ controller nào, chỉ thấy `xxxService` trong constructor |
| Transaction đặt ở đâu? | Ở ServiceImpl, `@Transactional`. Đặt hàng là 1 transaction: lỗi bước nào thì hủy hết. | `CheckoutServiceImpl` |
| Hai người cùng mua chiếc bánh cuối? | Trừ kho bằng UPDATE có điều kiện "còn đủ hàng"; người sau nhận 0 dòng cập nhật → báo vượt tồn. | `deductForSale` |
| Bấm đặt hàng 2 lần? | Giỏ chuyển `ACTIVE → CHECKED_OUT` có điều kiện, lần 2 thất bại. | `claimForCheckout` |
| Giá có tin trình duyệt không? | Không, server tự tính lại mọi giá, chiết khấu, phí ship. | `PricingServiceImpl` |
| Thêm cổng thanh toán mới? | Thêm 1 class cài `PaymentProcessor`, không sửa đặt hàng/hủy đơn. | `PaymentProcessor`, registry |
| Sao biết VNPay đã trả tiền? | Chỉ tin IPN có chữ ký HMAC-SHA512 đúng và đúng số tiền, không tin trang return. | `handleNotification` |
| Trang HTML có nhận entity không? | Không, chỉ nhận DTO (`...PageView`, `...Form`). | Bất kỳ `model.addAttribute("page", ...)` |
| Bánh hết hạn xử lý sao? | Không bán lô hết hạn, bán lô sắp hết hạn trước, admin có nút hủy hàng hết hạn. | `takeStock`, trang Tồn kho |
| Sao không có nút tạo admin cho mọi người? | Admin đầu tiên tạo lúc cài đặt; chỉ super admin thêm/khóa admin khác. | `AdminAccountServiceImpl` |

## 5. Lịch đọc tối nay (khoảng 60 phút)

1. **10 phút** — mục 1 và 2, mở thử 1 controller → service → impl.
2. **20 phút** — đọc kỹ `placeOrder` (3.1) và Strategy thanh toán (3.2). Đây là phần thầy hỏi nhiều nhất.
3. **15 phút** — kho và đóng hộp (3.4, 3.5), đọc kèm [`inventory-batches-guide.md`](inventory-batches-guide.md).
4. **10 phút** — phần **mình phụ trách** trong [`team-work-plan.md`](team-work-plan.md).
5. **5 phút** — đọc lại bảng câu hỏi ở mục 4.

Quy tắc chấm chi tiết: [`instructor-code-rules.md`](instructor-code-rules.md). Luật nghiệp vụ: [`business-rules.md`](business-rules.md).
