# Luật code theo cách thầy chấm (bắt buộc cho mọi PR)

Thầy chấm 1-1 và **trừ 2,5 điểm khi sai kiến trúc**, nên mọi PR phải qua checklist này. Mỗi luật có ví dụ **ĐÚNG** lấy từ code đang có trong dự án; bạn mở các file đó ra để làm theo. Muốn hiểu sâu hơn thì đọc thêm `docs/architecture-conventions.md`.

Trước khi tạo PR, chép checklist ở cuối file vào mô tả PR và tick từng dòng.

---

## 1. Các tầng và hướng gọi

```
controller  →  service (interface)  →  service.impl  →  repository (interface)  →  entity
                         ↑ dữ liệu ra/vào view đi qua dto
```

- Mỗi module (`cart`, `order`, `product`, `inventory`, `security`, `dashboard`, `feedback`, `storefront`) có các package con `controller`, `service`, `service.impl`, `repository`, `dto`, và `exception` nếu cần.
- Entity và enum dùng chung đặt ở package `entity` và `enums` ngoài cùng.
- Đặt file đúng package của module sở hữu chức năng. Không gom chung vào một package "admin".

**ĐÚNG:** `dashboard/controller/DashboardController` → `dashboard/service/DashboardService` → `dashboard/service/impl/DashboardServiceImpl`.

## 2. Controller chỉ gọi service

- Constructor của controller **chỉ** nhận service interface. Không inject repository, `EntityManager`, `PasswordEncoder`, `SessionRegistry`, gateway hay `Environment`.
- Controller không chứa nghiệp vụ: không tính tiền, không so sánh trạng thái đơn, không `if (method == PaymentMethod.VNPAY)`. Controller chỉ nhận request, gọi service, đưa DTO cho view, rồi chuyển trang.
- Mỗi trang có một controller rõ ràng, nằm trong module sở hữu trang đó.

**ĐÚNG:** `storefront/controller/StorefrontPageController` chỉ gọi `StorefrontService.homePage()` rồi `model.addAttribute("page", ...)`.

**SAI:**
```java
public AdminCustomerController(CustomerRepository customerRepository) { ... }
```

## 3. Service là interface + một impl

- `service/XxxService.java` là interface. `service/impl/XxxServiceImpl.java` gắn `@Service` và `implements XxxService`.
- Nơi dùng service luôn nhận **interface**, không nhận impl.
- `@Transactional` đặt ở **service impl**, không bao giờ ở controller. Hàm chỉ đọc dùng `@Transactional(readOnly = true)`.
- **Không tạo interface thừa** cho lớp tiện ích chỉ có một cài đặt và không phải service, ví dụ `VnpayGateway`, `PasswordRules`, `MoneyFormat`.

**ĐÚNG:** `order/service/OrderStatisticsService` + `order/service/impl/OrderStatisticsServiceImpl`.

## 4. Repository là interface, ưu tiên Spring Data JPA

- Repository là **interface** `extends JpaRepository<Entity, Long>`.
- Thứ tự ưu tiên khi viết truy vấn:
  1. Hàm đặt tên theo quy ước, ví dụ `countByPublishedFalseAndRatingIsNotNull()`.
  2. `@Query` JPQL.
- **Không viết native SQL.** Chỉ có ngoại lệ đã có sẵn cho khóa dữ liệu và cập nhật tồn kho có điều kiện, ví dụ `InventoryBalanceRepository.deductAvailable`.
- Cập nhật trạng thái dùng `@Modifying @Query` có điều kiện trạng thái cũ, trả về số dòng đổi. Ví dụ `ACTIVE → DISABLED` chỉ đổi khi đang `ACTIVE`.

**ĐÚNG:** `feedback/repository/FeedbackRepository.countByPublishedFalseAndRatingIsNotNull()`.

## 5. Không thò tay vào module khác

- Repository và service impl của một module **chỉ đọc/ghi bảng của module mình**.
- Cần dữ liệu của module khác thì **gọi service của module đó**. Service gọi service thì được. Repository gọi repository khác, hoặc truy vấn bảng của module khác, thì **không được**.
- Khi hai module gọi nhau, mỗi chiều phải dùng hai service khác nhau, để Spring không báo vòng phụ thuộc (xem sơ đồ ở từng tài liệu task).

**ĐÚNG:** trước khi xóa sản phẩm, `ProductAdminServiceImpl` hỏi `CartService.countItemsWithVariants`, `OrderManagementService.countOrdersContaining` và `InventoryService.hasStockRecords`.

**SAI:** viết `select count(i) from CartItem i ...` bên trong repository của `product`.

## 6. View chỉ nhận DTO, mỗi trang 1 DTO `page`

- Không đưa entity, `Object[]` hay dòng kết quả thô của truy vấn ra view.
- Mỗi trang nhận **một** DTO tên `page`, do service dựng. Form là **form DTO** riêng, dùng với `th:object`.
- Thông báo sau khi chuyển trang (flash `success`, `successMessage`, `errorMessage`...) không tính là dữ liệu trang.

**ĐÚNG:** `AdminInventoryController` gọi `model.addAttribute("page", inventory.inventoryPage(...))`; `CheckoutController` có form `checkoutForm` cộng page DTO `checkout`.

**SAI:**
```java
model.addAttribute("orders", ...);
model.addAttribute("tabs", ...);
model.addAttribute("status", ...);
model.addAttribute("page", ...);
```

## 7. Chuyển entity ↔ DTO bằng hàm trong DTO

- Entity sang DTO: hàm `public static Xxx entityToDto(Entity entity, ...)` đặt trong chính DTO. Không lặp `new Xxx(...)` + nhiều setter ở khắp nơi.
- Form ghi vào entity: hàm `public void dtoToEntity(Entity entity)` đặt trong form.

**ĐÚNG:** `security/dto/CustomerProfile.entityToDto(Customer)` và `security/dto/CustomerRegistrationForm.dtoToEntity(Customer)`.

## 8. Thanh toán theo Strategy pattern

- Mỗi phương thức là một `PaymentProcessor`: `CodPaymentProcessor`, `VnpayPaymentProcessor`.
- Checkout, hủy đơn và quản lý đơn gọi processor qua `PaymentProcessorRegistry`, **không** rẽ nhánh `if (method == ...)`.
- Luật riêng của một phương thức đặt trong processor của nó. Ví dụ ngưỡng COD 5.000.000đ nằm trong `CodPaymentProcessor.checkCanPlace`.

## 9. Viết đơn giản, giải thích được

- **Mỗi dòng một lệnh.** Không viết dồn một dòng cho Java, Thymeleaf, CSS hay JS.
- Dùng `import`, **không** viết tên đầy đủ kiểu `com.se201411.project.mooncake...` trong code.
- **Không viết comment.** Đặt tên hàm và biến rõ nghĩa thay cho comment.
- Hằng số có tên, ví dụ `MAX_OPEN_COD_ORDERS`, không để số "trôi" trong code.
- Tách hàm private ngắn khi một đoạn lặp lại hoặc quá dài.
- Đơn giản nhưng **vẫn phải đúng**: giữ kiểm tra quyền sở hữu, cập nhật có điều kiện, transaction và test.
- Người làm phải **giải thích được từng dòng** khi thầy hỏi. Không chép đoạn code mình không hiểu.

## 10. Bảo mật và dữ liệu

- Khách hiện tại luôn lấy từ `CurrentCustomerService.requireCurrentCustomer()`. Không nhận `customerId`, `accountId` hay `cartId` từ form làm bằng chứng sở hữu.
- Tiền, chiết khấu, tồn kho luôn tính lại trên server.
- Không sửa migration đã có. Không thêm bảng hay cột khi chưa có migration mới được duyệt.
- Không commit `.env`, mật khẩu hay token.

## 11. Test

- Nghiệp vụ mới phải có unit test cho service, test controller khi có trang mới, và test tích hợp khi đụng database.
- Test tích hợp **chỉ** chạy trên `MoonCakeTestDB`. Không ghi dữ liệu test vào `MoonCakeDB`.
- Trước khi tạo PR, cả hai lệnh phải pass: `./mvnw test` và `./mvnw verify -Pintegration`.

---

## Checklist cho mô tả PR (chép nguyên khối và tick)

```
- [ ] Đúng tầng: controller → service (interface) → service.impl → repository; file đặt đúng package của module
- [ ] Controller chỉ gọi service; không có nghiệp vụ, không if theo phương thức thanh toán
- [ ] Service mới là interface + 1 impl; nơi dùng nhận interface; @Transactional ở service impl
- [ ] Không tạo interface thừa cho lớp tiện ích
- [ ] Repository là interface Spring Data; không native SQL mới; cập nhật trạng thái có điều kiện
- [ ] Không đọc/ghi bảng module khác; cần thì gọi service module đó; không vòng phụ thuộc
- [ ] View chỉ nhận DTO; mỗi trang 1 DTO "page"; form dùng form DTO
- [ ] entityToDto / dtoToEntity đặt trong DTO
- [ ] Mỗi dòng một lệnh; dùng import; không comment; hằng số có tên
- [ ] Lấy khách/tài khoản từ server, không tin id trong form
- [ ] Có test; ./mvnw test và ./mvnw verify -Pintegration đều pass (chỉ trên MoonCakeTestDB)
- [ ] Không commit .env; không sửa migration cũ; không ghi tên công cụ AI trong commit/PR
```
