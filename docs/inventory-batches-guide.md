# Tồn kho và lô hàng — giải thích đơn giản

Tài liệu cho cả nhóm nắm cách kho hoạt động. Quy tắc chi tiết nằm ở `business-rules.md` (v1.19–v1.21).

## 1. Tồn kho và lô hàng khác nhau thế nào?

- **Tồn kho** = **một con số** cho mỗi phiên bản: hiện có bao nhiêu cái. Ví dụ "Bánh Kim Sa 80g: 50 cái".
- **Lô hàng** = **từng đợt** hàng vào kho. Mỗi đợt có ngày sản xuất, hạn sử dụng và số còn lại.

Tồn kho **luôn bằng tổng số còn lại của các lô**. Ví dụ Bánh Kim Sa 80g:

| Lô | Ngày SX | Hạn dùng | Còn lại |
|---|---|---|---|
| Nhập 1/9 | 01/09 | 01/10 | 20 |
| Nhập 10/9 | 10/09 | 10/10 | 30 |
| **Tồn kho** | | | **50** |

- Trang **Tồn kho** trả lời: "còn bao nhiêu?"
- Trang **Lô hàng** trả lời: "số đó gồm những đợt nào, đợt nào sắp hết hạn?"

**Bán thì lấy lô hết hạn sớm nhất trước.** Khách mua 25 cái: lấy hết 20 cái lô 1/9, rồi 5 cái lô 10/9. Hàng cũ được bán trước, không bị để quá hạn.

**Lô đã quá hạn thì không bán nữa.**

- Trang Tồn kho ghi "trong đó N hết hạn".
- Khách chỉ thấy và chỉ mua được số còn hạn.
- Admin bấm **"Hủy hàng hết hạn"** để trừ số đó ra khỏi kho.

## 2. Từng loại hàng

### 🥮 Bánh

- Mỗi lần bấm **Nhập** tạo **1 lô mới**.
- **Bắt buộc ngày sản xuất.** Hạn tự tính = ngày sản xuất + "Thời hạn sử dụng (ngày)" của bánh (1–90 ngày).
- Không nhập được lô đã quá hạn.
- Ở trang Lô hàng, sửa ngày sản xuất của lô bánh thì hạn tự tính lại.

### 📦 Hộp rỗng (vỏ hộp)

- Mỗi lần **Nhập** vẫn tạo 1 lô, nhưng **không có ngày sản xuất và không có hạn**, vì vỏ không hư.
- Lô vỏ chỉ để đếm số lượng, không bao giờ hết hạn.
- Mỗi phiên bản hộp rỗng có **sức chứa** (số bánh/hộp), nhập ngay trong form phiên bản.

### 🎁 Hộp quà (vỏ + bánh bên trong)

- **Không nhập trực tiếp được.** Hộp quà chỉ vào kho bằng **"Đóng hộp"**.
- Hộp quà **không có thời hạn riêng**.
- Mỗi lần đóng hộp tạo 1 lô hộp quà:
  - **Ngày sản xuất** = ngày đóng hộp.
  - **Hạn** = hạn của chiếc bánh **hết hạn sớm nhất** bỏ vào hộp.

Ví dụ: hôm nay 12/11 đóng hộp, trong hộp có bánh hết hạn 25/11 và bánh hết hạn 30/11. **Hạn hộp quà là 25/11.** Hộp quà không bao giờ "sống lâu hơn" chiếc bánh bên trong.

Ở trang Lô hàng, lô hộp quà **chỉ đổi được mã lô**, không sửa được ngày.

## 3. Đóng hộp làm kho thay đổi thế nào?

Công thức hộp "Kim Phúc" gồm 1 vỏ 4 bánh, 2 bánh Kim Sa và 2 bánh Thập Cẩm. Đóng **10 hộp**:

| Phiên bản | Trước | Thay đổi | Sau |
|---|---|---|---|
| Vỏ hộp 4 bánh | 50 | −10 | 40 |
| Bánh Kim Sa | 100 | −20 | 80 |
| Bánh Thập Cẩm | 100 | −20 | 80 |
| **Hộp quà Kim Phúc** | 0 | **+10** | **10** |

- Bánh và vỏ được **chuyển** vào hộp quà, không mất đi đâu cả.
- Bánh lấy từ lô sắp hết hạn trước, và chỉ lấy bánh còn hạn.
- Không đủ bánh hoặc vỏ thì không đóng được, kho không đổi gì.

## 4. Khách mua thì trừ cái gì?

| Khách mua | Kho bị trừ |
|---|---|
| Bánh lẻ | Bánh đó |
| **Hộp quà có sẵn** | **Chỉ hộp quà** (vỏ và bánh đã trừ lúc đóng hộp rồi) |
| **Hộp tự chọn** | **1 vỏ + từng bánh** khách chọn (không tạo lô hộp quà) |

**Hủy đơn** thì cộng lại đúng những thứ đã trừ, trả về **đúng lô** đã bán ra.

## 5. Các nút trên trang Tồn kho

| Nút | Dùng khi | Ảnh hưởng tới lô |
|---|---|---|
| **Nhập** (bánh, vỏ) | Nhận hàng từ xưởng | Tạo 1 lô mới |
| **Đóng hộp** (hộp quà) | Ráp vỏ + bánh thành hộp quà | Tạo 1 lô hộp quà, trừ lô vỏ và bánh |
| **Điều chỉnh** | Kiểm kê thấy lệch, bị vỡ hay mất | Giảm: trừ từ các lô đang có. Tăng: tạo 1 lô không có ngày |
| **Hủy hàng hết hạn** | Bỏ số đã quá hạn | Trừ đúng số hết hạn khỏi các lô hết hạn |

Mỗi thao tác đều có một dòng trong **Lịch sử kho**: ai làm, làm gì, lúc nào.
