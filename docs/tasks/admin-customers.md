# Task: Admin quản lý khách hàng (xem, khóa, mở khóa tài khoản)

**Nghiệp vụ:** Business Rules v1.13, mục 10 "Quản lý khách hàng". Test case từ BR-77 đến BR-86.
**Người làm:** người phụ trách Admin.
**Phải phối hợp với người phụ trách Customer Sales** ở các file đánh dấu ⚠ (thuộc phần của họ). Gửi họ xem trước khi sửa.
**Database:** không thêm bảng, cột hay migration. Dùng cột có sẵn `ACCOUNT.status` (`ACTIVE` / `DISABLED`).

Làm đúng theo tài liệu này. Gặp chỗ nào tài liệu chưa nói, **hỏi trưởng nhóm**, không tự quyết nghiệp vụ.

---

## 1. Phạm vi

**Làm:**
- `GET /admin/customers`: danh sách khách, ô tìm theo email hoặc số điện thoại, phân trang 20 dòng.
- `GET /admin/customers/{customerId}`: chi tiết khách, các đơn đang mở, nút Khóa / Mở khóa.
- `POST /admin/customers/{customerId}/lock`: khóa, kèm lựa chọn hủy luôn đơn chưa thanh toán.
- `POST /admin/customers/{customerId}/unlock`: mở khóa.
- Khóa có hiệu lực ngay: hết phiên đăng nhập của khách, và bước đặt hàng kiểm tra lại tài khoản.
- Nhãn **"Khách đã bị khóa"** và liên kết sang trang khách ở danh sách đơn và chi tiết đơn của Admin.
- Thêm mục **"Khách hàng"** vào menu Admin.

**Không làm:**
- Xóa tài khoản.
- Sửa thông tin hộ khách, đặt lại mật khẩu hộ khách.
- Khóa tài khoản Admin.
- Lưu lý do khóa.
- Gửi email thông báo.

---

## 2. Luật chấm của thầy (bắt buộc)

**Luật đầy đủ kèm ví dụ đúng/sai nằm ở [`docs/instructor-code-rules.md`](../instructor-code-rules.md). Đọc file đó trước khi code, rồi chép checklist cuối file đó vào mô tả PR.** Dưới đây là các điểm riêng của task này.

- [ ] Luồng gọi: `controller` → `service` (interface) → `service.impl` → `repository`.
- [ ] Controller **chỉ gọi service**. Không inject repository, `SessionRegistry` hay `EntityManager` vào controller.
- [ ] Mỗi service là **interface + một impl**. Constructor nhận interface.
- [ ] View chỉ nhận DTO. **Mỗi trang 1 DTO tên `page`**. Form gắn vào form DTO.
- [ ] Chuyển entity sang DTO bằng hàm static `entityToDto(...)` đặt trong DTO.
- [ ] **Không gọi repository của module khác.**
  - `security` cần dữ liệu đơn thì gọi service bên `order`.
  - `order` cần trạng thái tài khoản thì gọi service bên `security`.
- [ ] `@Transactional` đặt ở service impl.
- [ ] Repository là interface Spring Data; truy vấn mới dùng tên hàm hoặc `@Query` JPQL, **không native SQL**.
- [ ] View không nhận entity, `Object[]` hay dòng kết quả thô.
- [ ] Template, CSS, JS cũng không viết dồn một dòng.
- [ ] Mỗi dòng một lệnh. Dùng `import`, không viết tên đầy đủ. **Không comment.**
- [ ] Không `if (method == VNPAY)`. Hủy đơn luôn đi qua `OrderCancellationService.cancelForAdmin` có sẵn.

---

## 3. Tránh vòng phụ thuộc (đọc kỹ)

Hai module gọi nhau. Chỉ được nối theo đúng sơ đồ dưới đây, nếu không Spring sẽ báo vòng phụ thuộc khi khởi động:

```
security.CustomerAdminServiceImpl  ──►  order.OrderStatisticsService     (đếm đơn, đơn đang mở)
                                   ──►  order.OrderCancellationService   (hủy đơn)
                                   ──►  security.CustomerProfileService  (khóa / mở khóa / tra trạng thái)

order.OrderManagementServiceImpl   ──►  security.CustomerProfileService  (tra khách nào đang bị khóa)
order.CheckoutServiceImpl          ──►  security.CustomerProfileService  (kiểm tra còn ACTIVE)
```

- `CustomerProfileServiceImpl` **chỉ** dùng repository của `security`. **Không** inject service nào của `order`.
- `OrderStatisticsServiceImpl` **chỉ** dùng repository của `order`. **Không** inject service nào của `security`.

---

## 4. Danh sách file

| Module | File | Tạo hay sửa |
|---|---|---|
| security | `controller/AdminCustomerController.java` | Tạo mới |
| security | `service/CustomerAdminService.java` + `service/impl/CustomerAdminServiceImpl.java` | Tạo mới |
| security | `service/CustomerProfileService.java` + impl | Sửa: thêm 5 hàm (mục 6.1) |
| security | `repository/CustomerRepository.java` | Sửa: thêm 2 truy vấn |
| security | `repository/AccountRepository.java` | Sửa: thêm 1 truy vấn cập nhật có điều kiện |
| security | `dto/AdminCustomerRow.java`, `dto/AdminCustomerListPageView.java`, `dto/AdminCustomerDetailPageView.java`, `dto/AccountLockForm.java`, `dto/AccountLockResult.java` | Tạo mới |
| security | ⚠ `config/SecurityConfig.java` | Sửa: session registry, đường dẫn hết phiên, xử lý đăng nhập lỗi |
| order | `service/OrderStatisticsService.java` + impl | Sửa: thêm 2 hàm (mục 6.2) |
| order | `repository/OrderStatisticsRepository.java` | Sửa: thêm 2 truy vấn |
| order | `dto/CustomerOrderCount.java`, `dto/CustomerOpenOrderView.java` | Tạo mới |
| order | `dto/AdminOrderView.java` | Sửa: thêm `Long customerId`, `boolean customerLocked` |
| order | `service/impl/OrderManagementServiceImpl.java` | Sửa: điền 2 trường mới |
| order | ⚠ `service/impl/CheckoutServiceImpl.java` | Sửa: 1 dòng kiểm tra trong `placeOrder` |
| templates | `admin/customers.html`, `admin/customer-detail.html` | Tạo mới |
| templates | `admin/layout.html` | Sửa: thêm mục menu |
| templates | `admin/orders.html`, `admin/order-detail.html` | Sửa: nhãn và liên kết |
| templates | ⚠ `security/login.html` | Sửa: thêm thông báo `param.disabled` |

---

## 5. DTO

**security.dto**
- `AdminCustomerRow` (record): `long customerId`, `String email`, `String fullName`, `String phone`, `String memberSince` (dd/MM/yyyy), `boolean active`, `long orderCount`, `long cancelledCount`.
  - Hàm `static AdminCustomerRow entityToDto(Customer customer, CustomerOrderCount count)`.
  - Khi khách chưa có đơn nào, `count` là `CustomerOrderCount.none(customer.getId())`.
- `AdminCustomerListPageView` (record): `List<AdminCustomerRow> rows`, `String keyword`, `int pageNumber`, `boolean hasNext`.
- `AdminCustomerDetailPageView` (record): `AdminCustomerRow customer`, `List<CustomerOpenOrderView> openOrders`, `int cancellableCount`.
  - `cancellableCount` là số đơn trong `openOrders` có `cancellableOnLock = true`.
- `AccountLockForm` (class): `boolean cancelUnpaidOrders = true` (mặc định bật).
- `AccountLockResult` (record): `int cancelledCount`, `List<String> manualOrderCodes`.
  - Có hàm `String message()` trả về câu tóm tắt, theo mẫu ở mục 7.3.

**order.dto**
- `CustomerOrderCount` (record): `long customerId`, `long total`, `long cancelled`.
  - Hàm `static CustomerOrderCount none(long customerId)` trả về 0 đơn.
- `CustomerOpenOrderView` (record): `long orderId`, `String orderCode`, `String statusLabel`, `String paymentLabel` (ví dụ "COD · Chưa thanh toán", "VNPay · Đã thanh toán"), `String formattedTotal`, `boolean cancellableOnLock`.
  - `cancellableOnLock = (status == PLACED) && (payment status == UNPAID)`.
  - Điều kiện này **không** phụ thuộc phương thức thanh toán.

---

## 6. Service

### 6.1. `CustomerProfileService` (security), thêm 5 hàm

```
Set<Long> disabledCustomerIds(Collection<Long> customerIds);
void requireActiveAccount(ShopperIdentity customerIdentity);
boolean disableAccount(long customerId);
boolean enableAccount(long customerId);
long accountIdOf(long customerId);
```

| Hàm | Transaction | Làm gì |
|---|---|---|
| `disabledCustomerIds` | `@Transactional(readOnly = true)` | Danh sách rỗng thì trả về tập rỗng. Còn lại dùng 1 truy vấn: id khách trong danh sách có `account.status = DISABLED`. |
| `requireActiveAccount` | `@Transactional(readOnly = true)` | Tài khoản của khách không `ACTIVE` thì ném `BusinessRuleViolationException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ cửa hàng để được hỗ trợ.")`. |
| `disableAccount` | `@Transactional` | Cập nhật có điều kiện `ACTIVE → DISABLED`, chỉ cho `role = CUSTOMER`. Trả về `true` nếu đúng 1 dòng thay đổi. |
| `enableAccount` | `@Transactional` | Ngược lại: `DISABLED → ACTIVE`. |
| `accountIdOf` | `@Transactional(readOnly = true)` | Trả về `customer.getAccount().getId()`. Không thấy khách thì ném `ResourceNotFoundException`. |

`AccountRepository`: thêm `@Modifying @Query` cập nhật `status` theo `id`, `role` và `status` cũ, trả về `int` (số dòng đổi).

### 6.2. `OrderStatisticsService` (order), thêm 2 hàm

```
Map<Long, CustomerOrderCount> orderCountsByCustomer(Collection<Long> customerIds);
List<CustomerOpenOrderView> openOrdersOf(long customerId);
```

- **`orderCountsByCustomer`:** danh sách rỗng thì trả về map rỗng. Còn lại dùng **1 truy vấn** gom nhóm theo `order.customer.id`, đếm tổng đơn và số đơn `CANCELLED`. Không truy vấn từng khách một.
- **`openOrdersOf`:** các đơn của khách có trạng thái `PLACED`, `PROCESSING` hoặc `SHIPPING`, mới nhất trước, kèm payment. Chuyển sang `CustomerOpenOrderView`.

### 6.3. `CustomerAdminService` (security, mới)

```
AdminCustomerListPageView customersPage(String keyword, int page);
AdminCustomerDetailPageView customerPage(long customerId);
AccountLockResult lock(long customerId, boolean cancelUnpaidOrders);
void unlock(long customerId);
```

Mọi hàm gọi `adminAccessService.requireAdmin()` đầu tiên.

**`customersPage`**, `@Transactional(readOnly = true)`:
1. `keyword` được trim. Rỗng thì coi như không lọc.
2. Lấy trang khách từ `CustomerRepository` bằng truy vấn tìm theo email hoặc số điện thoại có chứa keyword. Phân trang 20 dòng, mới đăng ký trước, có `join fetch` account.
3. Lấy số đơn bằng `orderStatisticsService.orderCountsByCustomer(ids của trang)`.
4. Ghép thành `AdminCustomerRow`, trả về `AdminCustomerListPageView`.

**`customerPage`**, `@Transactional(readOnly = true)`: tìm khách (không thấy thì `ResourceNotFoundException`), lấy số đơn và `openOrdersOf`, ghép thành page view.

**`lock`**, **KHÔNG** gắn `@Transactional`. Đây là hàm điều phối, mỗi bước dùng transaction riêng của service được gọi:
1. `boolean changed = customerProfileService.disableAccount(customerId);`
   - Nếu `!changed` thì ném `BusinessRuleViolationException("Tài khoản đã bị khóa trước đó.")`.
2. Gọi hàm private `expireSessions(customerProfileService.accountIdOf(customerId))`, xem mục 6.4.
3. Nếu `cancelUnpaidOrders`, lấy `openOrdersOf(customerId)`. Với **từng** đơn có `cancellableOnLock`:
   - `try { orderCancellationService.cancelForAdmin(order.orderId()); cancelled = cancelled + 1; }`
   - `catch (MoonCakeException exception) { manual.add(order.orderCode()); }`
4. Các đơn đang mở mà **không** hủy (không đủ điều kiện, hoặc hủy thất bại) đều cho vào `manual`.
5. Trả về `new AccountLockResult(cancelled, manual)`.

**`unlock`**, không gắn `@Transactional`: gọi `customerProfileService.enableAccount(customerId)`. Trả về `false` thì ném `BusinessRuleViolationException("Tài khoản đang hoạt động.")`.

### 6.4. Hết phiên đăng nhập ngay khi khóa
- `CustomerAdminServiceImpl` inject `SessionRegistry` (bean khai báo trong `SecurityConfig`, mục 8).
- `expireSessions(long accountId)`:
  - duyệt `sessionRegistry.getAllPrincipals()`;
  - với principal là `MoonCakeUserPrincipal` có `getAccountId() == accountId`, gọi `expireNow()` cho mọi `SessionInformation` của principal đó (`getAllSessions(principal, false)`).
- Không cần sửa `equals` / `hashCode` của `MoonCakeUserPrincipal`.

### 6.5. ⚠ `CheckoutServiceImpl.placeOrder`
Ngay sau dòng `requireOrderingAllowed(...)`, thêm:
```
customerProfileService.requireActiveAccount(customerIdentity);
```
Dòng này nằm trong transaction đặt hàng, nên khóa đã commit thì không tạo được đơn nữa.

### 6.6. `OrderManagementServiceImpl`
- Khi dựng danh sách `AdminOrderView` (trang danh sách và trang chi tiết):
  - gom id khách của các đơn đang hiển thị;
  - gọi **một lần** `customerProfileService.disabledCustomerIds(ids)`;
  - điền `customerId` và `customerLocked` cho từng đơn.
- **Không** gọi theo từng đơn.

---

## 7. Controller và giao diện

### 7.1. `AdminCustomerController` (security)
`@Controller`, `@RequestMapping("/admin/customers")`. Constructor chỉ nhận `CustomerAdminService`.

| Đường dẫn | Làm gì |
|---|---|
| `GET` (tham số `q` không bắt buộc, `page` mặc định 0) | `model.addAttribute("page", customerAdminService.customersPage(q, page))`, trả về `"admin/customers"` |
| `GET /{customerId}` | Thêm `"page"` = `customerPage(customerId)`. Thêm `"lockForm"` = `new AccountLockForm()` nếu model chưa có. Trả về `"admin/customer-detail"` |
| `POST /{customerId}/lock` (`@ModelAttribute AccountLockForm lockForm`) | Gọi `lock(customerId, lockForm.isCancelUnpaidOrders())`. Flash `"success"` = `result.message()`. Bắt `MoonCakeException` thì flash `"lockError"` = `exception.getMessage()`. Chuyển về `/admin/customers/{customerId}` |
| `POST /{customerId}/unlock` | Gọi `unlock`. Flash `"success"` = `"Đã mở khóa tài khoản."`. Bắt `MoonCakeException` thì flash `"lockError"`. Chuyển về trang chi tiết |

### 7.2. Giao diện
- **Mẫu để copy:** `admin/orders.html` (bảng, tìm kiếm, phân trang) và `admin/order-detail.html` (trang chi tiết). Dùng `admin/layout :: head`, `nav`, `flash` giống các trang admin khác.
- **`admin/customers.html`:**
  - ô tìm `q` và nút "Tìm";
  - bảng cột: Email, Họ tên, Số điện thoại, Ngày đăng ký, Số đơn (dạng "12 đơn · 3 đã hủy"), Trạng thái (nhãn "Đang hoạt động" xanh / "Đã khóa" đỏ), link "Xem";
  - pager "← Trang trước" / "Trang sau →" theo `page.pageNumber` và `page.hasNext`.
- **`admin/customer-detail.html`:**
  - Khối thông tin khách.
  - Khối lỗi `<p th:if="${lockError}" class="form-alert" role="alert" th:text="${lockError}">`. Layout admin chỉ tự hiện flash `success`, nên lỗi phải hiện riêng ở đây.
  - Bảng **"Đơn đang mở"** gồm mã đơn (link sang chi tiết đơn), trạng thái, thanh toán, tổng tiền. Đơn không đủ điều kiện hủy có ghi chú "Xử lý thủ công".
  - Khách đang hoạt động: hiện form khóa, gồm:
    - checkbox `cancelUnpaidOrders` với chữ **"Hủy luôn các đơn chưa thanh toán đang chờ xử lý (N đơn)"**, N lấy từ `page.cancellableCount`;
    - nút đỏ **"Khóa tài khoản"**, có hộp xác nhận ghi tên khách, theo cách các nút xóa hiện có đang làm.
  - Khách đã bị khóa: chỉ hiện nút **"Mở khóa tài khoản"**.
- **`admin/layout.html`:** thêm mục **"Khách hàng"** (`/admin/customers`) ngay dưới "Đơn hàng".
- **`admin/orders.html` và `admin/order-detail.html`:**
  - đơn có `customerLocked` hiện nhãn đỏ **"Khách đã bị khóa"**;
  - trang chi tiết đơn có link **"Xem khách hàng"** tới `/admin/customers/{customerId}`.
- **⚠ `security/login.html`:** thêm khối `th:if="${param.disabled}"`, nội dung **"Tài khoản của bạn đã bị khóa. Vui lòng liên hệ cửa hàng để được hỗ trợ."**. Giữ nguyên câu `param.error` hiện có.

### 7.3. Câu tóm tắt `AccountLockResult.message()`
- Không hủy đơn nào, không có đơn cần xử lý: `"Đã khóa tài khoản."`
- Có hủy: `"Đã khóa tài khoản. Đã hủy N đơn chưa thanh toán và hoàn kho."`
- Có đơn cần xử lý tay: thêm `" Cần xử lý thủ công: MC-..., MC-..."`

---

## 8. ⚠ `SecurityConfig`

1. Khai báo bean `SessionRegistry sessionRegistry()` trả về `new SessionRegistryImpl()`.
2. Khai báo bean `HttpSessionEventPublisher httpSessionEventPublisher()`.
3. Trong cấu hình `http`:
   - `sessionManagement` với `maximumSessions(-1)` (không giới hạn số phiên);
   - `sessionRegistry(sessionRegistry())`;
   - `expiredUrl("/login?disabled")`.
4. `formLogin` vẫn dùng `failureUrl("/login?error")` như hiện tại. Không thêm trang báo riêng cho tài khoản bị khóa khi đăng nhập, để tránh lộ email đã đăng ký.
5. `/admin/**` đã là `hasRole("ADMIN")` nên không cần sửa.

---

## 9. Test bắt buộc

**`security/service/CustomerAdminServiceImplTest`** (mới, Mockito):
- `lockCancelsOnlyUnpaidPlacedOrders`: chỉ đơn `cancellableOnLock` được gọi `cancelForAdmin`.
- `failedCancellationGoesToManualListAndLockStays`: `cancelForAdmin` ném lỗi thì mã đơn nằm trong `manualOrderCodes`, và vẫn trả kết quả.
- `lockWithoutCancellationKeepsEveryOrder`
- `lockingAlreadyLockedAccountIsRejected`
- `lockExpiresTheCustomerSessions`: mock `SessionRegistry`, verify `expireNow()`.
- `unlockActivatesAccount`

**`security/service/CustomerProfileServiceImplTest`:** `requireActiveAccountRejectsDisabledAccount`, `disabledCustomerIdsWithEmptyInputReturnsEmpty`.

**`order/service/OrderStatisticsServiceImplTest`:** `openOrderIsCancellableOnlyWhenPlacedAndUnpaid`.

**`order/service/CheckoutServiceImplTest`:** `disabledAccountCannotPlaceOrder`, verify không lưu đơn.

**`admin/AdminPagesTest`:** render `/admin/customers` và `/admin/customers/{id}`; danh sách đơn hiện nhãn "Khách đã bị khóa".

**`security/config/SecurityConfigTest`:** `CUSTOMER` vào `/admin/customers` thì bị từ chối.

**`AdminCustomerIT`** (mới, chạy trên `MoonCakeTestDB`, giống `AdminFlowIT`, tự dọn dữ liệu):
- tạo khách và 1 đơn COD `PLACED`;
- khóa có hủy: tài khoản `DISABLED`, đơn `CANCELLED`, tồn kho trả lại đúng một lần;
- mở khóa: tài khoản `ACTIVE`.

Lệnh chạy, cả hai phải pass:
```
./mvnw test
./mvnw verify -Pintegration
```

---

## 10. Git

- Nhánh `feat/admin-customers`, tạo từ `main` mới nhất.
- Các file ⚠ gửi người phụ trách Customer Sales xem trước.
- Không commit `.env`. Không sửa migration. Commit và PR không ghi tên công cụ AI. Không force-push.
- Mô tả PR gồm Summary, Tests (số test pass của cả hai lệnh), "No schema change".
- Chưa được trưởng nhóm duyệt thì không merge.

---

## 11. Nghiệm thu (trưởng nhóm bấm thử)

1. Admin mở menu "Khách hàng": thấy danh sách. Tìm theo một phần email, rồi theo số điện thoại, đều ra đúng khách.
2. Dùng khách A đang đăng nhập ở trình duyệt khác. Tạo cho A 1 đơn COD và 1 đơn VNPay chưa trả.
3. Admin mở trang A: thấy 2 đơn, ô hủy ghi "(2 đơn)" và đã được tick. Bấm "Khóa tài khoản", xác nhận.
   - Thông báo "Đã khóa tài khoản. Đã hủy 2 đơn chưa thanh toán và hoàn kho."
   - Tồn kho các món trả lại đúng.
4. Ở trình duyệt của A, bấm bất kỳ trang nào: bị đưa về trang đăng nhập kèm thông báo tài khoản đã bị khóa. Đăng nhập lại không được.
5. Với khách B có đơn đang `SHIPPING`, khóa B: đơn không bị hủy, nằm trong "Cần xử lý thủ công". Ở trang đơn Admin, đơn có nhãn "Khách đã bị khóa" và vẫn xác nhận giao được.
6. Khóa khách C và bỏ tick ô hủy: đơn của C giữ nguyên.
7. Mở khóa A: A đăng nhập lại được, các đơn đã hủy vẫn là "Đã hủy".
8. Đăng nhập khách rồi mở `/admin/customers`: bị từ chối.
