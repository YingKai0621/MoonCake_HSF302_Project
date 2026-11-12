# Kịch bản test tay: hộp rỗng, hộp quà, đóng hộp và kho

Làm theo thứ tự. Mở thêm tab **Tồn kho** và **Lô hàng** để đối chiếu số. Ghi lại tồn kho trước mỗi bước có thay đổi kho.

Bước nào khác kết quả mong đợi: chụp màn hình, ghi số bước.

## 1. Hộp rỗng

| # | Làm | Kết quả mong đợi |
|---|---|---|
| 1 | Thêm sản phẩm, chọn loại **Hộp rỗng** | Có ô **Sức chứa**, **Chất liệu**. Không có Khối lượng, Nhân bánh, Thời hạn sử dụng |
| 2 | Để trống Sức chứa, bấm tạo | Báo lỗi ngay dưới ô |
| 3 | Nhập sức chứa 4, tạo | Thẻ phiên bản hiện "4 bánh / hộp" |
| 4 | Thêm **phiên bản thứ 2** cho cùng hộp (ví dụ "Vàng 4 bánh") | Lưu được, có sức chứa riêng |
| 5 | Đổi sức chứa phiên bản chưa ai dùng thành 6 | Lưu được |
| 6 | Mở phiên bản hộp rỗng cũ chưa có sức chứa, nhập sức chứa, lưu | Lưu được |

## 2. Tạo hộp quà

| # | Làm | Kết quả mong đợi |
|---|---|---|
| 7 | Vào **Thêm hộp quà** | Có phần **Chọn vỏ hộp**. Thẻ ghi "Tên hộp · Tên phiên bản", số bánh/hộp, số vỏ trong kho. Bên phải ghi **Giá bán**. Nút **Tạo hộp quà**. Không có ô Thời hạn sử dụng, không có công tắc hiển thị |
| 8 | Không chọn vỏ, bấm tạo | Chỉ **một** câu "Vui lòng chọn một mục." dưới nhóm thẻ |
| 9 | Chọn vỏ, tạo | Chuyển thẳng sang bước **chọn bánh**, tiêu đề "Chọn 4 bánh cho hộp" |
| 10 | Xem cửa hàng | Hộp quà mới **chưa hiện** |

## 3. Chọn bánh

| # | Làm | Kết quả mong đợi |
|---|---|---|
| 11 | Bấm +/− bánh | Dòng **"Giá lẻ vỏ + bánh"** đổi theo (tự tính: giá vỏ + giá bánh × số lượng) |
| 12 | Chọn thiếu hoặc thừa bánh | Nút "Xong, chốt" bị khóa, dòng gợi ý báo thiếu/thừa |
| 13 | Mở "Đổi vỏ hộp khác", chọn vỏ sức chứa khác | Tiêu đề số bánh đổi theo |
| 14 | Chọn đủ bánh, chốt | Sang bước **Đóng hộp & mở bán**. Tiêu đề "Bánh trong <Tên hộp · Tên phiên bản>" |

## 4. Nhập kho và lô hàng

| # | Làm | Kết quả mong đợi |
|---|---|---|
| 15 | Tồn kho → **Nhập** bánh, bỏ trống ngày sản xuất | Báo bắt buộc ngày sản xuất |
| 16 | Nhập bánh với ngày sản xuất đã quá hạn | Bị từ chối, tồn kho không đổi |
| 17 | Nhập bánh đúng | Lô mới có HSD = NSX + thời hạn của bánh |
| 18 | **Nhập** hộp rỗng | Không có ô ngày sản xuất. Lô có NSX và HSD là "—" |
| 19 | Dòng **hộp quà** ở Tồn kho | Không có nút "Nhập", chỉ có **"Đóng hộp"**. Bấm vào thì hộp quà được chọn sẵn |
| 20 | **Điều chỉnh** một bánh, nhập 2, lý do "Vỡ" | Tồn kho giảm 2. Lịch sử kho có dòng điều chỉnh −2 |
| 21 | Điều chỉnh nhập 0 hoặc số âm | Bị từ chối: "Số lượng bớt đi phải lớn hơn 0. Muốn thêm hàng hãy dùng Nhập kho." |
| 22 | Lô hàng → Sửa **lô bánh**, đổi ngày sản xuất | HSD tự tính lại |
| 23 | Lô hàng → Sửa **lô hộp quà** hoặc **lô hộp rỗng** | Chỉ có ô mã lô |

## 5. Đóng hộp

| # | Làm | Kết quả mong đợi |
|---|---|---|
| 24 | Đóng **2 hộp** | Vỏ −2, mỗi bánh −(số/hộp × 2), hộp quà +2. Lịch sử kho có các dòng "Đóng hộp" cùng một lần |
| 25 | Xem lô hộp quà vừa tạo | NSX = **hôm nay**. HSD = **HSD sớm nhất** của các lô bánh vừa bị trừ |
| 26 | Đóng nhiều hơn số bánh/vỏ đang có | Báo lỗi, kho không đổi gì |
| 26b | Đóng hộp khi bánh chỉ còn ở lô "Chưa có hạn dùng" | Báo "<bánh> còn lô chưa có hạn dùng…", kho không đổi. Nhập ngày cho lô đó rồi đóng lại thì được |

## 6. Mở bán, bán, hủy

| # | Làm | Kết quả mong đợi |
|---|---|---|
| 27 | Mở bán hộp quà | Hiện ngoài cửa hàng. Dòng "Hạn sử dụng" ghi **"Theo hạn của bánh trong hộp"** |
| 28 | Trang bánh lẻ | "Hạn sử dụng" vẫn ghi "N ngày kể từ ngày sản xuất" |
| 29 | Khách mua 1 hộp quà (COD) | Chỉ hộp quà −1. Vỏ và bánh **không** bị trừ thêm |
| 30 | Admin hủy đơn đó | Hộp quà +1 |

## 7. Những việc không được làm

| # | Làm | Kết quả mong đợi |
|---|---|---|
| 31 | Hộp quà đã đóng hộp hoặc đã bán | Không có nút xóa, hiện lý do. Không mở khóa được, chỉ có "Tạo bản sao để đổi bánh" |
| 32 | Bấm "Tạo bản sao" | Hộp quà mới giữ vỏ và danh sách bánh cũ, đang nháp |
| 33 | Đổi sức chứa của hộp rỗng đang được hộp quà dùng | Bị từ chối |
| 34 | Xóa phiên bản hộp rỗng đang được hộp quà dùng | Bị từ chối |
| 35 | Tạo hộp quà nháp mới rồi xóa | Xóa được, vỏ hộp vẫn còn |
| 36 | Nhân bản một **bánh** | Bản sao giữ nguyên thời hạn sử dụng, lưu lại được |

## 8. Hộp khách tự chọn

| # | Làm | Kết quả mong đợi |
|---|---|---|
| 37 | Mở bán phiên bản hộp rỗng, vào trang tự chọn hộp | Hộp hiện với tên "Tên hộp · Tên phiên bản" |
| 38 | Chọn đủ bánh, thêm vào giỏ | Dòng giỏ ghi tên vỏ đủ cả phiên bản |
| 39 | Đặt hàng | Kho trừ **1 vỏ + từng bánh** |
| 40 | Ngừng bán phiên bản hộp rỗng đó | Khách không thấy nữa. Admin **vẫn chọn được** khi tạo hộp quà |

## 9. Quản trị viên

| # | Làm | Kết quả mong đợi |
|---|---|---|
| 41 | Đăng nhập **super admin**, mở **Quản trị viên** | Thấy form "Thêm quản trị viên"; dòng của mình có nhãn **Super admin** và "(bạn)", không có nút Khóa |
| 42 | Thêm admin với email của một khách | Bị từ chối "Email này đã có tài khoản…" |
| 43 | Thêm admin với email mới, mật khẩu 8 ký tự trở lên | Thêm được, trạng thái "Đang hoạt động" |
| 44 | Đăng nhập bằng admin mới (trình duyệt khác) | Vào được trang quản trị. Trang Quản trị viên **không** có form thêm và nút Khóa |
| 45 | Super admin bấm **Khóa** admin mới | Admin mới bị đăng xuất, đăng nhập lại báo bị khóa |
| 46 | Super admin bấm **Mở khóa** | Admin mới đăng nhập lại được |
