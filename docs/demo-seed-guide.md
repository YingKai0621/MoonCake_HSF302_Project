# Dữ liệu demo cho buổi thuyết trình

Dùng **sản phẩm thật có sẵn** (V3: 12 bánh, 2 vỏ hộp, 2 hộp quà, ảnh thật). Dữ liệu được tạo bằng **chính các service của app**, nên đơn hàng, giá, lô và hạn sử dụng đúng luật như khi bấm trên web.

Bộ seed: [`../MoonCake/src/test/java/com/se201411/project/mooncake/demo/DemoDataSeeder.java`](../MoonCake/src/test/java/com/se201411/project/mooncake/demo/DemoDataSeeder.java). Nó **không chạy** cùng `mvn test` thông thường, và chỉ chạy được trên database `MoonCakeDemoDB` còn trống đơn hàng.

## Cách dựng lại từ đầu

1. Tạo database trống `MoonCakeDemoDB` trên SQL Server local (không đụng `MoonCakeDB`).
2. Build app: `./mvnw -DskipTests package`.
3. Chạy app một lần ở chế độ khởi tạo để Flyway tạo bảng và tạo super admin. Đặt các biến môi trường trước khi chạy:
   - `DB_NAME=MoonCakeDemoDB`
   - `BOOTSTRAP_ADMIN_EMAIL=admin.demo@example.test`
   - `BOOTSTRAP_ADMIN_PASSWORD=MoonCake@2026`
   - `SERVER_PORT=0`

   Lệnh: `java -jar target/MoonCake-0.0.1-SNAPSHOT.jar --spring.profiles.active=bootstrap-admin` (app tự tắt sau khi tạo admin).
4. Chạy bộ seed với `DB_NAME=MoonCakeDemoDB`: `./mvnw test -Dtest=DemoDataSeeder -Dsurefire.failIfNoSpecifiedTests=false`.
5. Chạy app bình thường với `DB_NAME=MoonCakeDemoDB` (ví dụ đổi dòng `DB_NAME` trong `.env`). Muốn quay lại dữ liệu cũ thì đổi lại `MoonCakeDB`.

Biến môi trường của hệ điều hành được ưu tiên hơn `.env`, nên có thể đặt `DB_NAME` khi chạy mà không sửa `.env`.

## Tài khoản demo (mật khẩu chung `MoonCake@2026`)

| Tài khoản | Vai trò |
|---|---|
| `admin.demo@example.test` | Super admin (thêm/khóa quản trị viên) |
| `nhanvien.demo@example.test` | Quản trị viên thường |
| `khach.demo@example.test` | Khách chính để demo mua hàng |
| `tran.binh@example.test`, `le.chi@example.test`, `pham.dung@example.test` | Khách có sẵn đơn hàng và đánh giá |

## Dữ liệu có sẵn

| Nhóm | Nội dung |
|---|---|
| Bánh dùng trong hộp quà | Mỗi loại 2 lô có hạn (NSX 12 ngày trước và 2 ngày trước) |
| Chà bông trứng nghiền | Có lô **sắp hết hạn** (còn 5 ngày) |
| Thập cẩm gà quay | Có 6 cái trong lô **đã hết hạn** → demo "Hủy hàng hết hạn" |
| Lưu sa than tre | Còn 4 cái → nhãn "Chỉ còn" |
| Cua bát bửu | Hết hàng |
| Vỏ hộp Kim Phúc 4 bánh, Lam Phúc 6 bánh | Có sức chứa, đủ tồn (vỏ không có hạn) |
| Hộp quà Kim Phúc, Lam Phúc | Đã đóng sẵn, đang bán; lô hộp quà có HSD theo bánh sớm nhất. Còn đủ bánh và vỏ để **đóng thêm** khi demo |
| Đơn hàng | 10 đơn COD rải trong 7 ngày: hoàn tất, đang giao, đã giao chờ khách xác nhận, đang chuẩn bị, chờ xử lý, khách hủy, admin hủy |
| Đánh giá | 2 đánh giá đã hiện trên trang chủ, 1 đánh giá chờ duyệt |

## Gợi ý khi demo

- `khach.demo` có 1 đơn hoàn tất và 1 đơn đang giao, nên còn đặt thêm được **1 đơn COD** (giới hạn 2 đơn COD đang mở). Muốn đặt nhiều đơn hơn thì dùng tài khoản khách khác hoặc chuyển trạng thái đơn trước.
- Hủy đơn mẫu được: hàng trả về đúng lô đã bán ra.
- Thanh toán VNPay chỉ demo được khi `.env` bật VNPay Sandbox; dữ liệu mẫu không có đơn VNPay.

## Script SQL cũ

`MoonCake/scripts/seed-local-demo-full.sql` và `seed-local-demo-inventory.sql` là bộ cũ: tạo sản phẩm "DEMO-" và ghi tồn kho không có lô, nên sinh lô bánh không có hạn. **Không dùng cho buổi thuyết trình.**
