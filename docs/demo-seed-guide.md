# Seed dữ liệu demo đầy đủ

Script: [`../MoonCake/scripts/seed-local-demo-full.sql`](../MoonCake/scripts/seed-local-demo-full.sql).

## Cách chạy trong IntelliJ / SSMS

1. Khởi động ứng dụng thành công để Flyway chạy hết V1–V4.
2. Đăng ký một tài khoản **Customer** trên website. Tài khoản Admin không dùng làm người mua.
3. Mở Query Console của Data Source SQL Server, chọn database local `MoonCakeDB` (hoặc `MoonCakeTestDB` khi kiểm thử riêng).
4. Mở script và sửa dòng `@CustomerEmail` thành email tài khoản vừa đăng ký.
5. Chọn chế độ **Auto-commit**, chạy **toàn bộ file trong một lần**, không chạy từng đoạn. Script tự quản lý transaction.
6. Kết quả cuối gồm bảng tồn kho và 12 đơn hàng. Refresh trang web để xem dữ liệu.

Không đưa script vào thư mục Flyway migration và không chèn vào V4. Đây là dữ liệu demo local, không phải dữ liệu triển khai chung. Script không tạo database hoặc tài khoản và không cần mật khẩu trong file SQL.

## Dữ liệu được tạo

| Nhóm | Nội dung |
|---|---|
| Danh mục | 3 danh mục DEMO |
| Bánh | 8 SKU, thông tin thực phẩm lấy từ các sản phẩm V3; giá demo 65.000–99.000 đồng |
| Hộp rỗng | 3 SKU chứa 4, 6, 8 bánh; giá demo 45.000–85.000 đồng |
| PRESET | 2 công thức READY đủ bánh; 1 DRAFT chưa đủ bánh, chưa mở bán |
| Kho | Nhập linh kiện; đóng 160 hộp 4 bánh và 100 hộp 6 bánh; xuất bán, hoàn khi hủy |
| Doanh nghiệp | 2 bản ghi giả lập, email `example.test` |
| Đơn hàng | 12 đơn `DEMO-ORDER-01` đến `DEMO-ORDER-12`, mỗi đơn có COD và giỏ CHECKED_OUT |
| Customer | Tất cả đơn thuộc Customer đã chọn, có thể xem lịch sử sau khi đăng nhập |

Ảnh là logo minh họa. Giá, người nhận, địa chỉ và doanh nghiệp là dữ liệu giả lập, không dùng để giao hàng. Script không tự bịa thành phần hoặc hạn sử dụng bánh.

## Các tình huống để thử

| Dữ liệu | Cách kiểm tra |
|---|---|
| Đơn 01–03: PLACED | Nhận xử lý hoặc hủy; hủy phải hoàn kho đúng một lần |
| Đơn 04–06: PROCESSING | Chuyển SHIPPING hoặc hủy |
| Đơn 07–08: SHIPPING | Hoàn tất giao hàng và thu COD; không hủy sau khi giao vận |
| Đơn 09–10: COMPLETED | COD đã PAID, xem tổng tiền và snapshot |
| Đơn 11–12: CANCELLED | COD UNPAID, đã có CANCEL_RETURN; không hoàn kho lần hai |
| Đơn 03, 05, 08, 10 | Số lượng 50/100: kiểm tra bậc chiết khấu và phí giao hàng |
| Đơn 02, 04, 05, 09, 12 | CUSTOM: trừ tồn hộp rỗng và từng loại bánh |
| DEMO-GIFT-04/06 | PRESET: bán trừ tồn thành phẩm; đóng hộp trừ linh kiện |
| DEMO-GIFT-08 | DRAFT: sửa công thức, bổ sung đủ 8 bánh rồi READY trước khi đóng hộp |
| DEMO-CAKE-06 | Sản phẩm/variant inactive |
| DEMO-CAKE-07 | Tồn 3, dưới ngưỡng cảnh báo 10 |
| DEMO-CAKE-08 | Tồn 0 |

Các trang Admin: `/admin/catalog`, `/admin/presets`, `/admin/inventory`, `/admin/orders`. Dùng tài khoản Admin hiện có để truy cập.

Chiết khấu lấy từ các tier đang active và có hiệu lực tại ngày giả lập của đơn; nếu bạn đã sửa tier V2, kết quả sẽ theo dữ liệu hiện tại. Phí giao hàng 30.000 đồng, hoặc 150.000 đồng khi tổng số hộp quà từ 50. Bánh bên trong hộp không được cộng vào nhóm bánh lẻ.

## Chạy lại và xử lý lỗi

- Seed chạy trong một transaction; lỗi sẽ rollback toàn bộ dữ liệu của lần chạy đó.
- Danh mục `demo-seed-v1` đánh dấu seed thành công. Chạy lại sẽ bỏ qua, không nạp thêm hàng hoặc tạo đơn trùng, kể cả khi đã thao tác trên đơn demo.
- Không xóa riêng danh mục đánh dấu để ép chạy lại. Muốn một bộ sạch, dùng database local mới, chạy migrations, đăng ký Customer rồi seed.
- Script chỉ thêm SKU `DEMO-`; không tăng tồn hoặc chỉnh sửa SKU hiện có. Không cần chạy script `seed-local-demo-inventory.sql` cho bộ dữ liệu này.
- Lỗi `Register an active CUSTOMER...`: kiểm tra email và trạng thái tài khoản, sau đó chạy lại toàn file.
- Lỗi `Reserved DEMO identifiers...`: có dữ liệu trùng tiền tố từ nguồn khác; kiểm tra trước, không xóa lịch sử đơn tùy tiện.

## Kiểm chứng

Đã kiểm tra cú pháp bằng SQL Server `SET PARSEONLY ON`, không thực thi seed vào database. Khi chạy thật, script kiểm tra tổng tiền đơn/thanh toán, sức chứa công thức READY và đối chiếu tồn kho với tổng lịch sử biến động trước khi commit. Chưa xác minh toàn bộ luồng giao diện trên bộ seed này.
