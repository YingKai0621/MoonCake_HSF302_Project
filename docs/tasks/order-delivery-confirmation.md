# Task: Xác nhận giao hàng hai phía (trạng thái "Đã giao")

**Nghiệp vụ:** Business Rules v1.16. Test case từ BR-98 đến BR-104.
**Module:** `order`, cộng một chỗ nhỏ ở `dashboard`.
**Phân công:**
- Phần Admin ("Đã giao hàng") thuộc người phụ trách **Admin**.
- Phần khách ("Đã nhận được hàng") thuộc người phụ trách **Customer Sales**.
- Hai người có thể làm chung một nhánh, hoặc một người làm rồi người kia review.

**Database:** cần migration mới **V19**. **Không sửa V1–V18.**

**Đọc trước:** [`docs/instructor-code-rules.md`](../instructor-code-rules.md) (luật thầy chấm). Chép checklist cuối file đó vào mô tả PR.

Gặp chỗ nào tài liệu chưa nói, **hỏi trưởng nhóm**, không tự quyết nghiệp vụ.

---

## 1. Luồng mới

```
PLACED → PROCESSING → SHIPPING ──(Admin: "Đã giao hàng")──► DELIVERED ──(Khách: "Đã nhận được hàng")──► COMPLETED
                                                                │
                                                                └──(quá 3 ngày, hệ thống tự làm)──► COMPLETED
```

| Bước | Ai | Điều kiện | Kết quả |
|---|---|---|---|
| Đã giao hàng | Admin | Đơn `SHIPPING` | `DELIVERED`, ghi `delivered_at`. Đơn COD: payment `UNPAID → PAID`, ghi `paid_at` **trong cùng transaction** (giữ đúng logic thu COD hiện có) |
| Đã nhận được hàng | Khách sở hữu đơn | Đơn `DELIVERED` | `COMPLETED`, ghi `completed_at` và `customer_confirmed_at` |
| Tự hoàn tất | Job hệ thống | Đơn `DELIVERED`, `delivered_at` cách hiện tại ≥ 3 ngày | `COMPLETED`, ghi `completed_at`, **không** ghi `customer_confirmed_at` |

Mọi lần chuyển trạng thái dùng **cập nhật có điều kiện** (`where status = :trạngThaiCũ`) và kiểm tra **đúng 1 dòng** được đổi. Nhờ vậy khách và job cùng bấm một lúc thì chỉ một bên thắng.

## 2. Phạm vi

**Làm:** những gì có trong luồng trên, kèm:
- nhãn và màu cho trạng thái mới;
- tab "Đã giao" ở danh sách đơn admin;
- dòng thời gian thêm bước "Đã giao";
- dashboard tính cả đơn `DELIVERED`.

**Không làm:**
- khiếu nại hoặc "chưa nhận được hàng" trong hệ thống (khách liên hệ cửa hàng);
- đổi trả;
- gửi email;
- gọi API đơn vị vận chuyển.

---

## 3. Danh sách file

| Tầng | File | Việc |
|---|---|---|
| migration | `src/main/resources/db/migration/V19__order_delivery_confirmation.sql` | Tạo mới (mục 4) |
| enum | `enums/OrderStatus.java` | Thêm `DELIVERED` **giữa** `SHIPPING` và `COMPLETED` |
| entity | `entity/SalesOrder.java` | Thêm `deliveredAt`, `customerConfirmedAt` (`LocalDateTime`) + getter/setter |
| repository | `order/repository/OrderAdminRepository.java` + `impl/OrderAdminRepositoryImpl.java` | Sửa `transition(...)`: ghi `deliveredAt` khi sang `DELIVERED` (hàm `timestampField`) |
| repository | `order/repository/SalesOrderRepository.java` | Thêm `confirmReceived(...)`, `findDeliveredBefore(...)`, `autoComplete(...)` (mục 5.3) |
| service | `order/service/impl/OrderManagementServiceImpl.java` | Sửa `allowed(...)`, `transition(...)`, `actions(...)`, nhãn/màu (mục 5.1) |
| service | `order/service/CustomerOrderService.java` + impl | Thêm `confirmReceived(String orderCode, ShopperIdentity customer)` (mục 5.2) |
| service | `order/service/DeliveredOrderCompletionService.java` + `impl/DeliveredOrderCompletionServiceImpl.java` | Tạo mới: tự hoàn tất (mục 5.4) |
| scheduler | `order/scheduler/DeliveredOrderCompletionJob.java` | Tạo mới, theo mẫu `UnpaidOrderExpiryJob` |
| controller | `order/controller/CustomerOrderController.java` | Thêm `POST /orders/{orderCode}/received` |
| dto | `order/dto/OrderFulfilmentView.java` | Thêm `deliveredAt`, `customerConfirmedAt` (đã định dạng) |
| dto | `order/dto/CustomerOrderDetailView.java` | Thêm `boolean receivable` (khách được bấm "Đã nhận được hàng") và `String autoCompleteDate` |
| dashboard | `order/service/impl/OrderStatisticsServiceImpl.java`, `order/repository/OrderStatisticsRepository.java`, `dashboard/service/impl/DashboardServiceImpl.java` | Tính cả `DELIVERED` (mục 5.5) |
| template | `templates/admin/order-detail.html`, `templates/admin/orders.html` | Nút "Đã giao hàng", bước mới |
| template | `templates/order/order-detail.html`, `templates/order/orders.html` | Nút "Đã nhận được hàng", bước mới, câu nhắc hạn |

---

## 4. Migration V19

Làm theo mẫu `V8__add_cash_rounding_adjustment.sql`. Ràng buộc mới tham chiếu cột vừa thêm nên phải bọc trong `EXEC(N'...')`.

1. `SET XACT_ABORT ON;`
2. Bỏ ràng buộc cũ: `ALTER TABLE dbo.SALES_ORDER DROP CONSTRAINT CK_SALES_ORDER_status;`
3. Thêm cột: `delivered_at DATETIME2(3) NULL`, `customer_confirmed_at DATETIME2(3) NULL`.
4. Tạo lại ràng buộc trong `EXEC(N'...')`: `CK_SALES_ORDER_status CHECK (order_status IN ('PLACED','PROCESSING','SHIPPING','DELIVERED','COMPLETED','CANCELLED'))`.
5. **Không** đổi dữ liệu cũ. Đơn đã `COMPLETED` giữ nguyên, `delivered_at` để trống.

Cột `order_status` là `VARCHAR(16)`. `DELIVERED` có 9 ký tự nên vừa, không cần đổi độ dài.

---

## 5. Service

### 5.1. `OrderManagementServiceImpl` (Admin)
- **`allowed(from, to)`:** thay `SHIPPING → COMPLETED` bằng `SHIPPING → DELIVERED`. **Admin không còn chuyển thẳng sang `COMPLETED`.** Vẫn đúng 3 cặp: `PLACED→PROCESSING`, `PROCESSING→SHIPPING` (qua `ship`), `SHIPPING→DELIVERED`.
- **`transition(id, target)`:**
  - đổi chỗ `if (target == OrderStatus.COMPLETED)` gọi `paymentProcessor.completeDelivery(...)` thành `if (target == OrderStatus.DELIVERED)`;
  - COD vẫn thu tiền qua `CodPaymentProcessor.completeDelivery` (Strategy pattern, **không** `if (method == COD)`).
- **`OrderAdminRepositoryImpl.timestampField`:** thêm `DELIVERED → "deliveredAt"`, **bỏ** `COMPLETED` (Admin không còn hoàn tất).
- **`actions(...)`:** đổi `canComplete` thành `status == SHIPPING`, trên giao diện là nút "Đã giao hàng". Đơn `DELIVERED` không hủy được: `canCancel` giữ điều kiện chỉ `PLACED` hoặc `PROCESSING`.
- **Nhãn / màu:**
  - `statusLabel(DELIVERED)` = `"Đã giao"`;
  - `statusTone(DELIVERED)` dùng cùng màu với `"Đang giao"` hoặc màu thành công, tùy bạn, nhưng phải giống nhau ở trang admin và trang khách.
- **Tab danh sách đơn:** `statusTabs` đang duyệt `OrderStatus.values()`, nên enum mới tự có tab. Kiểm tra nhãn hiện đúng.

### 5.2. `CustomerOrderServiceImpl` (khách)
```
void confirmReceived(String orderCode, ShopperIdentity customerIdentity);
```
Gắn `@Transactional`, làm đúng thứ tự:
1. Tìm đơn **của chính khách**: `orderRepository.findOwnedByOrderCode(code, customerIdentity.customerId())`. Không thấy thì `ResourceNotFoundException("Không tìm thấy đơn hàng của bạn")`.
2. `int changed = orderRepository.confirmReceived(order.getId(), now);`
3. `changed != 1` thì `BusinessRuleViolationException("Đơn chưa ở trạng thái Đã giao nên chưa xác nhận được.")`.

Thêm vào chi tiết đơn:
- `receivable = (status == DELIVERED)`;
- `autoCompleteDate` = ngày (`dd/MM/yyyy`) của `delivered_at + 3 ngày`, giờ Việt Nam; dùng `VietnamTime`.
- Nhãn khách: `DELIVERED` = `"Đã giao hàng"`.
- Đơn `DELIVERED` **không** cho sửa địa chỉ, **không** cho tự hủy (giữ các điều kiện `placed` hiện có là đủ).

### 5.3. `SalesOrderRepository`: 3 truy vấn mới (JPQL, `@Modifying` cho câu cập nhật)
- **`confirmReceived(orderId, now)`:** `update SalesOrder` đặt `status = COMPLETED`, `completedAt = now`, `customerConfirmedAt = now`, `updatedAt = now`, với điều kiện `id = :orderId and status = DELIVERED`. Trả về `int`.
- **`findDeliveredBefore(cutoff)`:** `select o.id from SalesOrder o where o.status = DELIVERED and o.deliveredAt <= :cutoff`. Trả về `List<Long>`.
- **`autoComplete(orderId, now)`:** giống `confirmReceived` nhưng **không** đặt `customerConfirmedAt`.

Truyền trạng thái bằng tham số (`@Param("delivered") OrderStatus delivered`), **không** viết chữ `'DELIVERED'` cứng trong câu truy vấn.

### 5.4. Tự hoàn tất
- **`DeliveredOrderCompletionService`** (interface) có hàm `int completeOverdueDeliveries();`.
- **Impl:**
  - hằng số `AUTO_COMPLETE_DAYS = 3`;
  - `cutoff = LocalDateTime.now(clock).minusDays(AUTO_COMPLETE_DAYS)`;
  - lấy `findDeliveredBefore(cutoff)`;
  - với từng id gọi `autoComplete(id, now)`, cộng số dòng đổi được;
  - trả về tổng.
  - Đặt `@Transactional` trên hàm. Không cần khóa thêm, vì câu cập nhật đã có điều kiện trạng thái.
- **`DeliveredOrderCompletionJob`:** `@Component` chỉ gọi service. `@Scheduled` theo đúng mẫu `UnpaidOrderExpiryJob`, dùng biến `${AUTO_COMPLETE_CHECK_MS:3600000}` (1 giờ). Thêm dòng ghi chú biến này vào `.env.example`.

### 5.5. Dashboard
- `OrderStatisticsServiceImpl.topCompletedProducts`: đếm đơn `DELIVERED` **và** `COMPLETED`. Đổi truy vấn `findTopProducts` nhận `Collection<OrderStatus> statuses` và dùng `item.order.status in :statuses`.
- `DashboardServiceImpl`: dòng `if (order.getStatus() == OrderStatus.COMPLETED)` cộng doanh thu thì đổi thành tính cả `DELIVERED`. Tách thành hàm private `countsAsRevenue(OrderStatus status)` cho dễ đọc.
- Câu "Chưa có đơn hoàn thành trong 30 ngày" trên dashboard đổi thành "Chưa có đơn đã giao trong 30 ngày".

---

## 6. Controller và giao diện

### 6.1. `CustomerOrderController`
```
@PostMapping("/orders/{orderCode}/received")
```
- Lấy khách từ `currentCustomerService.requireCurrentCustomer()`, gọi `customerOrderService.confirmReceived(orderCode, customer)`.
- Thành công: flash `"successMessage"` = `"Cảm ơn bạn đã xác nhận nhận hàng."`.
- Bắt `MoonCakeException` thì flash `"errorMessage"`.
- Luôn chuyển về `/orders/{orderCode}`.
- `/orders/**` đã yêu cầu vai trò `CUSTOMER` trong `SecurityConfig`, nên không cần sửa cấu hình.

### 6.2. Trang khách `order/order-detail.html`
- Dòng thời gian thêm bước **"Đã giao"** giữa "Đang giao hàng" và "Hoàn thành", hiện `fulfilment.deliveredAt`.
- Khi `order.receivable`:
  - nút vàng **"Đã nhận được hàng"**, form `POST` tới `/orders/{code}/received`, có hộp xác nhận "Bạn đã nhận đủ hàng của đơn này?";
  - câu nhỏ bên dưới: **"Chưa nhận được hàng? Vui lòng liên hệ cửa hàng trước ${order.autoCompleteDate}. Sau ngày này đơn sẽ tự hoàn tất."**
- Đơn hoàn thành:
  - có `customerConfirmedAt` thì ghi "Bạn đã xác nhận nhận hàng lúc ...";
  - không có thì ghi "Đơn tự hoàn tất sau 3 ngày kể từ khi giao".

### 6.3. Trang admin `admin/order-detail.html`
- Nút **"Xác nhận hoàn tất"** đổi thành **"Đã giao hàng"**. Giữ ô tick bắt buộc, sửa chữ thành **"Đơn vị vận chuyển đã giao thành công và đã thu đủ tiền (nếu COD)"**.
- Dòng thời gian 4 bước thành 5 bước (thêm "Đã giao").
- Đơn `DELIVERED` hiện ghi chú **"Chờ khách xác nhận, tự hoàn tất ngày ..."**.

---

## 7. Test bắt buộc

**`admin/OrderManagementServiceTest`**, sửa và thêm:
- Các test đang dùng `transition(1, COMPLETED)` đổi thành `transition(1, DELIVERED)`; giữ ý nghĩa "giao xong thì thu COD".
- `adminCanNoLongerCompleteDirectly`: `transition(1, COMPLETED)` từ `SHIPPING` bị từ chối.
- `permitsOnlyThreeNonCancellationTransitions` trong `AdminFlowIT`: vẫn đúng 3 cặp, nhưng cặp thứ ba là `SHIPPING→DELIVERED`.

**`order/service/CustomerOrderServiceImplTest`:**
- `customerConfirmsReceivedDeliveredOrder`
- `confirmingOrderThatIsStillShippingIsRejected`
- `cannotConfirmSomeoneElsesOrder`
- `deliveredOrderShowsAutoCompleteDate`

**`order/service/DeliveredOrderCompletionServiceImplTest`** (mới):
- `completesOnlyOrdersDeliveredMoreThanThreeDaysAgo`
- `countsOnlyRowsActuallyChanged`

**`order/controller/CustomerOrderControllerTest`:** `receivedRedirectsBackWithMessage`.

**`admin/AdminPagesTest`:**
- đơn `SHIPPING` hiện nút "Đã giao hàng";
- đơn `DELIVERED` hiện ghi chú chờ khách.

**`dashboard`:** test doanh thu tính cả đơn `DELIVERED`.

**Test tích hợp** (`AdminFlowIT` hoặc file mới `DeliveryConfirmationIT`, chạy trên `MoonCakeTestDB`):
- tạo đơn COD `SHIPPING`;
- admin `transition(id, DELIVERED)`: đơn `DELIVERED`, payment `PAID`;
- gọi `confirmReceived`: đơn `COMPLETED`, `customer_confirmed_at` có giá trị;
- tạo đơn `DELIVERED` có `delivered_at` 4 ngày trước, gọi `completeOverdueDeliveries()`: đơn `COMPLETED`, `customer_confirmed_at` trống.
- Test `deliveryAndCodCommitTogetherAndConcurrentRepeatCannotWinTwice` đổi đích thành `DELIVERED`.

Cả hai lệnh phải pass **trước khi tạo PR**:
```
./mvnw test
./mvnw verify -Pintegration
```

---

## 8. Git
- Nhánh `feat/order-delivery-confirmation`, tạo từ `main` mới nhất.
- Không commit `.env`. Không sửa V1–V18. Commit và PR không ghi tên công cụ AI. Không force-push.
- Sửa `AGENTS.md`: dòng "Never edit an applied/shared migration (`V1__...` through `V18__...`)" thành `V19__...`, và "after Flyway V18" thành "after Flyway V19" (số bảng giữ nguyên 25).
- Mô tả PR gồm:
  - Summary;
  - checklist trong `docs/instructor-code-rules.md` đã tick;
  - Tests (số test pass của cả hai lệnh);
  - **Schema change: V19** (ghi rõ đổi ràng buộc `CK_SALES_ORDER_status` và thêm 2 cột).
- Chưa được trưởng nhóm duyệt thì không merge.

---

## 9. Nghiệm thu (trưởng nhóm bấm thử)

1. Đơn COD đang giao: admin bấm **"Đã giao hàng"** (tick ô xác nhận).
   - Đơn thành "Đã giao", thanh toán "Đã thanh toán".
   - Trang admin ghi "Chờ khách xác nhận, tự hoàn tất ngày ...".
2. Khách mở đơn đó: có nút **"Đã nhận được hàng"** và câu nhắc hạn liên hệ. **Không** có nút hủy hay sửa địa chỉ.
3. Khách bấm nút, xác nhận: báo cảm ơn, đơn "Hoàn thành", ghi "Bạn đã xác nhận nhận hàng lúc ...".
4. Tạo một đơn khác "Đã giao". Muốn thử nhanh thì đặt `AUTO_COMPLETE_CHECK_MS` nhỏ trong `.env` của máy test và sửa `delivered_at` trên **`MoonCakeTestDB`**, không phải `MoonCakeDB`. Sau lần chạy job, đơn tự "Hoàn thành".
5. Admin không còn nút hoàn tất trực tiếp. Đơn "Đã giao" không có nút hủy.
6. Dashboard: đơn "Đã giao" được tính vào doanh thu và món bán chạy.
7. Danh sách đơn admin có tab "Đã giao", đếm đúng.
