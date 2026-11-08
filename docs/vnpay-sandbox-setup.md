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

## Luồng sử dụng

1. Customer chọn COD hoặc VNPAY tại checkout; hệ thống trừ kho một lần và tạo đơn PLACED. Chọn VNPAY sẽ chuyển thẳng đến Sandbox; lỗi cấu hình sẽ rollback cả checkout.
2. Với đơn COD chưa xử lý, cũng có thể mở chi tiết đơn và bấm **Thanh toán qua VNPay Sandbox**. Hệ thống lấy tổng tiền từ DB, chuyển phương thức sang VNPAY và tạo giao dịch 15 phút.
3. Thanh toán bằng dữ liệu test do VNPay công bố trên Sandbox; không dùng thẻ thật.
4. Return URL chỉ hiển thị thông báo. IPN hoặc kết quả querydr hợp lệ mới cập nhật PAID sau khi kiểm tra merchant, HMAC-SHA512, mã tham chiếu, tổng tiền và trạng thái giao dịch.
5. Admin xử lý/giao đơn online sau khi nhận PAID. Hoàn tất đơn online không thu COD lần nữa.

Không tạo giao dịch thứ hai khi giao dịch trước đang PENDING. Callback lặp trả mã 02. Giao dịch thất bại đã xác nhận có thể thử lại. Khi đóng trình duyệt hoặc mất IPN, bấm **Kiểm tra thanh toán VNPay** trong chi tiết đơn để truy vấn querydr. Phiên còn hạn dùng lại mã giao dịch cũ. Trạng thái chưa rõ kết quả vẫn giữ PENDING; chưa có hoàn tiền tự động. Không tự chuyển về COD hoặc tự hủy đơn VNPay để tránh trả tiền trùng và sai tồn kho.

## Các màn hình mới

- `/admin/overview`: tổng số đơn, chờ xử lý, doanh thu hoàn tất, đơn hủy; bảng doanh nghiệp; 100 góp ý mới nhất.
- `/feedback`: Customer gửi góp ý tối đa 2.000 ký tự, nội dung được hiển thị dạng text.
- Chi tiết đơn của Customer: sửa địa chỉ đầy đủ khi còn PLACED; kiểm tra sở hữu đơn và khóa dòng đơn trước khi lưu.

Mã đơn mới là `MC-` cộng 12 ký tự ngẫu nhiên; mã đơn cũ giữ nguyên. Việc biết mã đơn không cấp quyền xem đơn người khác.

## Kiểm chứng và giới hạn

Unit tests kiểm tra chữ ký bị sửa, sai tiền, callback thành công/thất bại/lặp, đơn hủy và tham số URL. Đã chạy `mvnw.cmd -Pintegration verify`: 143 kiểm thử đơn vị/web và 17 kiểm thử tích hợp đều đạt trên MoonCakeTestDB, bao gồm migrations đến V12. Chưa kiểm chứng giao dịch đầu-cuối trên VNPay Sandbox vì chưa có merchant. Kiểm tra trực quan toàn bộ giao diện chưa hoàn tất. Chưa có hoàn tiền tự động; querydr giữ nguyên PENDING khi chưa có kết quả cuối cùng.

Tài liệu chính thức: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
