# Chạy VNPay Sandbox

## Cấu hình

Đăng ký merchant thử nghiệm tại https://sandbox.vnpayment.vn/devreg/ để nhận TmnCode và HashSecret. Dự án không tự cấp hai giá trị này.

Trong `MoonCake/.env`:

```properties
VNPAY_ENABLED=true
VNPAY_TMN_CODE=MA_MERCHANT_CUA_BAN
VNPAY_HASH_SECRET=KHOA_MERCHANT_CUA_BAN
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=https://TEN-MIEN-CONG-KHAI/payments/vnpay/return
```

Dùng URL HTTPS công khai hoặc tunnel trỏ về port ứng dụng. Đăng ký IPN URL trong cấu hình merchant: `https://TEN-MIEN-CONG-KHAI/payments/vnpay/ipn`. `VNPAY_RETURN_URL` phải khớp website được VNPay chấp nhận. Nếu chỉ chạy localhost, trình duyệt có thể quay lại nhưng VNPay không gọi được IPN localhost; đơn sẽ chưa PAID. Khi tunnel đổi địa chỉ, cập nhật Return URL và IPN URL.

Working directory là `MoonCake`, Spring import `.env` qua `application.properties`. Khởi động ứng dụng để Flyway chạy V9 (giao dịch VNPay), V10 (góp ý), V11 (video), V12 (lô hàng). Không sửa migration đã áp dụng. Không commit `.env`.

## Đưa backend ra internet bằng Cloudflare Tunnel

VNPay chỉ gọi được IPN tới địa chỉ HTTPS công khai.

1. Cài `cloudflared`, chạy ứng dụng ở cổng 8080.
2. Chạy nhanh: `cloudflared tunnel --url http://localhost:8080`, lấy địa chỉ `https://xxxx.trycloudflare.com`. Địa chỉ này **đổi mỗi lần chạy**.
3. Đặt `APP_PUBLIC_URL=https://xxxx.trycloudflare.com` trong `MoonCake/.env`, khởi động lại ứng dụng.
4. Khai báo IPN URL `https://xxxx.trycloudflare.com/payments/vnpay/ipn` trong trang merchant VNPay Sandbox.
5. Khi demo nên dùng named tunnel với domain riêng để địa chỉ không đổi.

## Luồng sử dụng

1. Customer chọn COD hoặc VNPAY tại checkout; hệ thống trừ kho một lần và tạo đơn PLACED. Chọn VNPAY sẽ chuyển thẳng đến Sandbox; lỗi cấu hình sẽ rollback cả checkout.
2. Phương thức thanh toán cố định từ lúc checkout (Business Rules v1.7). Đơn COD không có nút thanh toán VNPay. Đơn VNPay chưa trả tiền có nút **Thanh toán lại** khi không còn giao dịch đang chờ; số tiền luôn lấy từ DB và mỗi giao dịch có hạn 15 phút.
3. Thanh toán bằng dữ liệu test do VNPay công bố trên Sandbox; không dùng thẻ thật.
4. Return URL chỉ hiển thị thông báo. IPN hoặc kết quả querydr hợp lệ mới cập nhật PAID sau khi kiểm tra merchant, HMAC-SHA512, mã tham chiếu, tổng tiền và trạng thái giao dịch.
5. Admin xử lý/giao đơn online sau khi nhận PAID. Hoàn tất đơn online không thu COD lần nữa.

Không tạo giao dịch thứ hai khi giao dịch trước đang PENDING. Callback lặp trả mã 02. Khách bấm Hủy đơn VNPay chưa trả tiền thì hệ thống tự hỏi VNPay (querydr): chưa trả thì hủy ngay, đã trả thì từ chối. Tiền về sau khi đơn đã hủy vẫn được ghi PAID và Admin thấy "Cần hoàn tiền". Admin có nút **Kiểm tra thanh toán VNPay** khi IPN bị lỡ. Chưa có hoàn tiền tự động.

## Các màn hình mới

- `/admin/overview`: tổng số đơn, chờ xử lý, doanh thu hoàn tất, đơn hủy; bảng doanh nghiệp; 100 góp ý mới nhất.
- `/feedback`: Customer gửi góp ý tối đa 2.000 ký tự, nội dung được hiển thị dạng text.
- Chi tiết đơn của Customer: sửa thông tin nhận hàng tại `/orders/{code}/delivery/edit` khi còn PLACED (tên, số điện thoại, địa chỉ, ghi chú).

Mã đơn mới là `MC-` cộng 12 ký tự ngẫu nhiên; mã đơn cũ giữ nguyên. Việc biết mã đơn không cấp quyền xem đơn người khác.

## Kiểm chứng và giới hạn

Unit tests kiểm tra chữ ký bị sửa, sai tiền, callback thành công/thất bại/lặp, đơn hủy và tham số URL. Đã chạy `mvnw.cmd -Pintegration verify`: 143 kiểm thử đơn vị/web và 17 kiểm thử tích hợp đều đạt trên MoonCakeTestDB, bao gồm migrations đến V12. Chưa kiểm chứng giao dịch đầu-cuối trên VNPay Sandbox vì chưa có merchant. Kiểm tra trực quan toàn bộ giao diện chưa hoàn tất. Chưa có hoàn tiền tự động; querydr giữ nguyên PENDING khi chưa có kết quả cuối cùng.

Tài liệu chính thức: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
