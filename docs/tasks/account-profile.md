# Task: Trang "Tài khoản của tôi" (sửa thông tin cá nhân, đổi mật khẩu)

**Nghiệp vụ:** Business Rules v1.12, mục 4. Các test case tương ứng là BR-70 đến BR-76.
**Module:** `security`, do người phụ trách Customer Sales sở hữu (xem `docs/team-work-plan.md`).
**Database:** không thêm bảng, cột hay migration nào.

Làm đúng theo tài liệu này. Gặp chỗ nào tài liệu chưa nói rõ thì **hỏi trưởng nhóm**, không tự quyết định nghiệp vụ.

---

## 1. Phạm vi

**Làm:**
- Trang `GET /account` hiện email (chỉ xem), form sửa họ tên và số điện thoại, form đổi mật khẩu.
- `POST /account/profile` để lưu họ tên và số điện thoại.
- `POST /account/password` để đổi mật khẩu.
- Thêm link "Thông tin tài khoản" vào menu Tài khoản trên header (chỉ hiện với khách).
- Cho phép mọi đường dẫn `/account/**` với vai trò `CUSTOMER` trong `SecurityConfig`.

**Không làm:**
- Không sửa email.
- Không thêm bảng hay cột.
- Không sửa module khác (`order`, `cart`, ...).
- Không làm trang tài khoản cho Admin.

---

## 2. Luật chấm của thầy (bắt buộc, sai là bị trừ điểm)

**Luật đầy đủ kèm ví dụ đúng/sai nằm ở [`docs/instructor-code-rules.md`](../instructor-code-rules.md). Đọc file đó trước khi code, rồi chép checklist cuối file đó vào mô tả PR.** Dưới đây là các điểm riêng của task này.

Trước khi tạo PR, tick đủ các mục sau:

- [ ] Luồng gọi là `controller` → `service` (interface) → `service.impl` → `repository`.
- [ ] Controller **chỉ gọi service**. Không inject repository, `EntityManager` hay `PasswordEncoder` vào controller.
- [ ] Mỗi service là **interface + một impl** trong `service.impl`. Constructor nhận interface.
- [ ] View chỉ nhận **DTO**, không nhận entity. Mỗi trang có **1 DTO tên `page`**. Form gắn vào **form DTO** riêng.
- [ ] DTO chuyển từ entity bằng **hàm static `entityToDto(...)`** trong DTO. Form ghi vào entity bằng `dtoToEntity(...)`.
- [ ] Chỉ dùng repository của module `security`. Không gọi repository của module khác.
- [ ] `@Transactional` đặt ở **service impl**, không đặt ở controller.
- [ ] Repository là interface Spring Data; truy vấn mới dùng tên hàm hoặc `@Query` JPQL, **không native SQL**.
- [ ] View không nhận entity, `Object[]` hay dòng kết quả thô.
- [ ] Template, CSS, JS cũng không viết dồn một dòng.
- [ ] Mỗi dòng một lệnh. Dùng `import`, không viết tên đầy đủ kiểu `com.se...`. **Không viết comment.**
- [ ] Không có dữ liệu do trình duyệt gửi lên quyết định "sửa ai". Khách luôn lấy từ `CurrentCustomerService.requireCurrentCustomer()`.

---

## 3. Danh sách file

Package gốc là `com.se201411.project.mooncake.security`.

| Tầng | File | Tạo mới hay sửa |
|---|---|---|
| controller | `controller/AccountController.java` | Tạo mới |
| service | `service/CustomerProfileService.java` | Sửa: thêm 3 hàm |
| service impl | `service/impl/CustomerProfileServiceImpl.java` | Sửa: cài đặt 3 hàm |
| service | `service/AuthenticationService.java` | Sửa: thêm 1 hàm |
| service impl | `service/impl/AuthenticationServiceImpl.java` | Sửa: cài đặt 1 hàm, inject thêm `PasswordResetTokenRepository` |
| dto | `dto/AccountPageView.java` | Tạo mới (record) |
| dto | `dto/ProfileUpdateForm.java` | Tạo mới (class form) |
| dto | `dto/ChangePasswordForm.java` | Tạo mới (class form) |
| exception | `exception/WrongCurrentPasswordException.java` | Tạo mới, kế thừa `BusinessRuleViolationException`, message cố định `"Mật khẩu hiện tại không đúng."` (theo mẫu `EmailAlreadyRegisteredException`) |
| exception | `exception/SamePasswordException.java` | Tạo mới, kế thừa `BusinessRuleViolationException`, message cố định `"Mật khẩu mới phải khác mật khẩu hiện tại."` |
| config | `config/SecurityConfig.java` | Sửa: thêm `"/account/**"` vào danh sách `hasRole("CUSTOMER")` |
| template | `src/main/resources/templates/security/account.html` | Tạo mới |
| template | `src/main/resources/templates/fragments/header.html` | Sửa: thêm 1 link |

Repository đã có sẵn, **không tạo mới**: `CustomerRepository`, `AccountRepository`, `PasswordResetTokenRepository` (dùng hàm có sẵn `invalidateOpenTokens`).

---

## 4. DTO

### 4.1. `AccountPageView` (record)
Gồm các trường:
- `String email`
- `String fullName`
- `String phone`
- `String memberSince`: ngày tạo tài khoản, định dạng `dd/MM/yyyy`.

Hàm `public static AccountPageView entityToDto(Customer customer)` dựng record từ `customer.getEmail()`, `getFullName()`, `getPhone()` và `getCreatedAt()` đã định dạng.

### 4.2. `ProfileUpdateForm` (class có getter/setter, giống `CustomerRegistrationForm`)
Copy **nguyên** annotation của hai trường tương ứng trong `CustomerRegistrationForm`:
- `fullName`: `@NotBlank(message = "Vui lòng nhập họ tên")`, `@Size(max = 40, ...)`, `@Pattern(regexp = "^[\\p{L}\\s]+$", ...)`.
- `phone`: `@NotBlank(message = "Vui lòng nhập số điện thoại")`, `@Pattern(regexp = "^0[0-9]{9}$", ...)`.

Hai hàm cần có:
- `public static ProfileUpdateForm entityToDto(Customer customer)` điền sẵn họ tên và số điện thoại hiện tại.
- `public void dtoToEntity(Customer customer)` gán `fullName.trim()` và `phone` vào customer, giống `CustomerRegistrationForm.dtoToEntity`.

**Không có** trường `customerId` hay `email`.

### 4.3. `ChangePasswordForm` (class có getter/setter)
- `currentPassword`: `@NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")`.
- `newPassword`: `@NotBlank(message = "Vui lòng nhập mật khẩu mới")`, `@Size(min = 8, max = 64, message = "Mật khẩu phải có từ 8 đến 64 ký tự")`.
- `confirmPassword`: `@NotBlank(message = "Vui lòng xác nhận mật khẩu")`.

---

## 5. Service

### 5.1. `CustomerProfileService`: thêm 3 hàm

```
AccountPageView accountPage(ShopperIdentity customerIdentity);
ProfileUpdateForm profileForm(ShopperIdentity customerIdentity);
void updateProfile(ShopperIdentity customerIdentity, ProfileUpdateForm form);
```

Cài đặt trong `CustomerProfileServiceImpl`:

1. **`accountPage`**, gắn `@Transactional(readOnly = true)`:
   - Tìm `Customer` bằng `customerRepository.findById(customerIdentity.customerId())`.
   - Không thấy thì ném `ResourceNotFoundException("Không tìm thấy thông tin khách hàng")`.
   - Trả về `AccountPageView.entityToDto(customer)`.
2. **`profileForm`**, gắn `@Transactional(readOnly = true)`: tìm customer như trên, trả về `ProfileUpdateForm.entityToDto(customer)`.
3. **`updateProfile`**, gắn `@Transactional`: tìm customer như trên, gọi `form.dtoToEntity(customer)`, rồi `customerRepository.save(customer)`.

Phần "tìm customer, không thấy thì báo lỗi" lặp lại 3 lần, nên tách thành hàm private `requireCustomer(ShopperIdentity)`.

### 5.2. `AuthenticationService`: thêm 1 hàm

```
void changePassword(ShopperIdentity customerIdentity, String currentPassword, String newPassword);
```

Cài đặt trong `AuthenticationServiceImpl`, gắn `@Transactional`, làm đúng thứ tự:
1. Tìm `Customer` theo `customerIdentity.customerId()`. Không thấy thì ném `ResourceNotFoundException`.
2. Lấy `Account account = customer.getAccount()`.
3. Nếu `!passwordEncoder.matches(currentPassword, account.getPasswordHash())` thì ném `new WrongCurrentPasswordException()`.
4. Nếu `passwordEncoder.matches(newPassword, account.getPasswordHash())` thì ném `new SamePasswordException()`.
5. Gọi `account.setPasswordHash(passwordEncoder.encode(newPassword))`, rồi `accountRepository.save(account)`.
6. Gọi `passwordResetTokenRepository.invalidateOpenTokens(account.getId(), LocalDateTime.now(clock))`.

Inject thêm `PasswordResetTokenRepository` qua constructor. Repository này thuộc cùng module `security` nên được phép.

---

## 6. Controller: `AccountController`

- Gắn `@Controller` và `@RequestMapping("/account")`.
- Constructor nhận `CurrentCustomerService`, `CustomerProfileService`, `AuthenticationService`.
- Tên view: `"security/account"` (file `templates/security/account.html`, cùng thư mục với các trang đăng nhập).

### 6.1. `GET /account`
1. `ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();`
2. Thêm attribute `"page"` = `customerProfileService.accountPage(customer)`.
3. Nếu model chưa có `"profileForm"` thì thêm `customerProfileService.profileForm(customer)`.
4. Nếu model chưa có `"passwordForm"` thì thêm `new ChangePasswordForm()`.
5. Trả về `"security/account"`.

### 6.2. `POST /account/profile`
Tham số: `@Valid @ModelAttribute("profileForm") ProfileUpdateForm form`, `BindingResult bindingResult`, `Model model`, `RedirectAttributes redirectAttributes`.
1. Lấy `customer` như trên.
2. Nếu `bindingResult.hasErrors()`:
   - thêm `"page"` = `accountPage(customer)`;
   - thêm `"passwordForm"` = `new ChangePasswordForm()`;
   - trả về `"security/account"`.
3. Gọi `customerProfileService.updateProfile(customer, form)`.
4. Thêm flash `"successMessage"` = `"Đã cập nhật thông tin cá nhân."`, rồi `return "redirect:/account";`.

### 6.3. `POST /account/password`
Tham số: `@Valid @ModelAttribute("passwordForm") ChangePasswordForm form`, `BindingResult bindingResult`, `Model model`, `RedirectAttributes redirectAttributes`.
1. Lấy `customer`.
2. Kiểm tra form giống `AuthenticationController.register`:
   - nếu `!Objects.equals(form.getNewPassword(), form.getConfirmPassword())` thì `bindingResult.rejectValue("confirmPassword", "password.mismatch", "Mật khẩu xác nhận không khớp")`;
   - nếu `PasswordRules.exceedsBcryptLimit(form.getNewPassword())` thì `rejectValue("newPassword", "password.tooLongForBcrypt", "Mật khẩu chứa ký tự đặc biệt quá dài, vui lòng rút ngắn")`.
3. Nếu có lỗi thì hiện lại trang:
   - thêm `"page"` = `accountPage(customer)`;
   - thêm `"profileForm"` = `profileForm(customer)`;
   - trả về `"security/account"`.
4. Gọi `authenticationService.changePassword(customer, form.getCurrentPassword(), form.getNewPassword())` trong `try`, với **hai** khối `catch`:
   - `catch (WrongCurrentPasswordException exception)`: `bindingResult.rejectValue("currentPassword", "password.wrong", exception.getMessage())`, rồi hiện lại trang như bước 3.
   - `catch (SamePasswordException exception)`: `bindingResult.rejectValue("newPassword", "password.same", exception.getMessage())`, rồi hiện lại trang như bước 3.

   Phần "hiện lại trang" lặp lại nhiều lần, nên tách thành hàm private `showAccountPage(Model model, ShopperIdentity customer)`. Hàm này chỉ thêm các attribute còn thiếu rồi trả về `"security/account"`. **Không** so sánh chuỗi message để phân biệt lỗi.
5. Thành công thì thêm flash `"successMessage"` = `"Đã đổi mật khẩu."`, rồi `return "redirect:/account";`.

---

## 7. Giao diện

### 7.1. `security/account.html`
- Lấy cấu trúc của `order/order-delivery-edit.html`: header fragment, `main`, footer fragment, các class giao diện. Gọi header bằng `header('account')`.
- Đầu `main` có khối hiện `successMessage` và `errorMessage`, copy từ `order/orders.html`.
- Tiêu đề: **"Tài khoản của tôi"**. Dòng phụ: `Thành viên từ ${page.memberSince}`.
- **Khối 1, "Thông tin cá nhân":**
  - Email hiển thị dạng chữ, không phải ô nhập: `${page.email}`, kèm ghi chú "Email dùng để đăng nhập, không thay đổi được".
  - Form `th:action="@{/account/profile}" th:object="${profileForm}" method="post"`, gồm ô "Họ tên" và ô "Số điện thoại".
  - Mỗi ô có `<label>` và dòng lỗi `th:errors`.
  - Nút **"Lưu thông tin"**.
- **Khối 2, "Đổi mật khẩu":**
  - Form `th:action="@{/account/password}" th:object="${passwordForm}" method="post"`, gồm 3 ô `type="password"`: "Mật khẩu hiện tại", "Mật khẩu mới", "Nhập lại mật khẩu mới".
  - **Không** điền lại giá trị mật khẩu khi có lỗi.
  - Nút **"Đổi mật khẩu"**.
- Dùng `th:action` thì CSRF token được thêm tự động, không cần làm gì thêm.

### 7.2. `fragments/header.html`
Trong menu Tài khoản, ngay **trên** dòng "Đơn hàng của tôi", thêm:
```
<a th:if="${customerLoggedIn}" th:href="@{/account}" href="/account">Thông tin tài khoản</a>
```

---

## 8. Test bắt buộc

Viết theo mẫu các test có sẵn trong `src/test/java/com/se201411/project/mooncake/security/`.

**`service/CustomerProfileServiceImplTest`** (tạo mới, dùng Mockito):
- `updatesNameAndPhoneOfTheSignedInCustomer`: họ tên có khoảng trắng hai đầu được trim.
- `missingCustomerIsReported`: ném `ResourceNotFoundException`.

**`service/AuthenticationServiceImplTest`** (thêm vào file có sẵn):
- `wrongCurrentPasswordIsRejectedAndHashUnchanged`
- `newPasswordEqualToCurrentIsRejected`
- `changePasswordStoresNewHashAndInvalidatesResetTokens`: verify `invalidateOpenTokens` được gọi.

**`controller/AccountControllerTest`** (tạo mới, MockMvc standalone giống `CustomerOrderControllerTest`):
- `rendersAccountPageWithOnePageView`
- `invalidPhoneShowsErrorWithoutCallingService`
- `mismatchedConfirmationShowsErrorWithoutCallingService`
- `wrongCurrentPasswordShowsFieldError`
- `successfulProfileUpdateRedirectsWithMessage`

**`config/SecurityConfigTest`** (thêm vào file có sẵn): khách chưa đăng nhập vào `/account` thì bị chuyển về login, Admin vào `/account` thì bị từ chối.

**`AccountIT`** (tạo mới ở `src/test/java/com/se201411/project/mooncake/`, giống `CustomerCartIT`, có `@Transactional` để rollback):
- Đăng ký khách bằng `authenticationService.registerCustomer`.
- Gọi `updateProfile`, kiểm tra DB đã đổi.
- Gọi `changePassword`, kiểm tra `passwordEncoder.matches(newPassword, hash)` đúng và mật khẩu cũ không còn đúng.
- **Chỉ chạy trên `MoonCakeTestDB`.** Không bao giờ chạy test ghi dữ liệu vào `MoonCakeDB`.

Lệnh chạy:
```
./mvnw test
./mvnw verify -Pintegration
```
Cả hai phải pass trước khi tạo PR.

---

## 9. Git

- Tạo nhánh từ `main` mới nhất: `feat/account-profile`.
- Không commit `.env`. Không sửa migration cũ.
- Commit message và mô tả PR **không** ghi tên công cụ AI.
- Không force-push.
- Mô tả PR gồm:
  - **Summary:** làm gì.
  - **Tests:** số test pass của `./mvnw test` và `./mvnw verify -Pintegration`.
  - **No schema change.**
- Gửi link PR cho trưởng nhóm review. Chưa được duyệt thì không tự merge.

---

## 10. Nghiệm thu: trưởng nhóm bấm thử

1. Đăng nhập khách, mở menu Tài khoản, bấm "Thông tin tài khoản". Trang hiện email, họ tên và số điện thoại hiện tại.
2. Sửa họ tên thành "  Trần Thị B  " và số điện thoại thành 0912345678, bấm Lưu. Trang báo "Đã cập nhật thông tin cá nhân." và hiện "Trần Thị B".
3. Nhập số điện thoại `091234`. Lỗi hiện ngay dưới ô, không lưu.
4. Mở trang thanh toán của một giỏ mới. Ô người nhận đã điền tên và số điện thoại mới. Đơn cũ vẫn hiện tên cũ.
5. Đổi mật khẩu với mật khẩu hiện tại sai. Báo "Mật khẩu hiện tại không đúng." dưới ô.
6. Đổi mật khẩu mới trùng mật khẩu cũ. Báo lỗi dưới ô mật khẩu mới.
7. Đổi mật khẩu đúng. Trang báo "Đã đổi mật khẩu.", vẫn đang đăng nhập. Đăng xuất, đăng nhập bằng mật khẩu mới thì được, bằng mật khẩu cũ thì không.
8. Đăng xuất rồi mở `/account`. Bị chuyển về trang đăng nhập.
9. Đăng nhập Admin rồi mở `/account`. Bị từ chối.
