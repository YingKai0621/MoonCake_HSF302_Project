# MoonCake MVP — Business Rules

**Dự án:** HSF302 — MoonCake
**Phiên bản tài liệu:** 1.21 — 12/11/2026

**Trạng thái:** Hạn hộp quà theo bánh bên trong; không đổi schema

**Database:** Microsoft SQL Server 2019+
**Nguồn đối chiếu:** `MoonCake_Logical_V5 (1).drawio`, `MoonCake_Physical_ERD_SQLServer.drawio`, `MoonCake_Physical_Schema_SQLServer.sql`, `MoonCake_Physical_ERD_Design_Notes.md`.

> **Đọc trước khi triển khai:** File này mô tả **quy tắc nghiệp vụ**; Physical SQL/ERD mô tả **cấu trúc dữ liệu**. Nếu tài liệu, SQL và source code hiện tại mâu thuẫn, **báo cáo và hỏi người phụ trách**, không tự thay đổi nghiệp vụ, thêm bảng hoặc sửa migration đã áp dụng. Physical SQL đã được chuyển thành Flyway V1 và kiểm thử trên SQL Server 2019; từ thời điểm migration được chia sẻ, không sửa V1 mà tạo migration mới.

### Thay đổi chính trong phiên bản 1.21

- **Hộp quà không còn thời hạn sử dụng riêng.** Chỉ **bánh** có "Thời hạn sử dụng (ngày)" (bắt buộc, 1–90). Form tạo/sửa hộp quà không có ô này; lưu hộp quà thì `shelf_life_days` để trống (dữ liệu cũ của hộp quà không còn được dùng).
- **Đóng hộp quà:** lô hộp quà có ngày sản xuất là ngày đóng hộp (giờ Việt Nam) và **HSD = HSD sớm nhất của các bánh dùng để đóng** (bánh được lấy từ lô sắp hết hạn trước). Thay cho quy tắc "ngày sớm hơn" ở v1.17.
- Trang sản phẩm của khách ghi "Hạn sử dụng: Theo hạn của bánh trong hộp" cho hộp quà; bánh vẫn ghi "N ngày kể từ ngày sản xuất".
- Nhân bản sản phẩm giữ nguyên thời hạn sử dụng của bánh.

### Thay đổi chính trong phiên bản 1.20

- **Thêm hộp quà bắt buộc chọn vỏ hộp ngay trên trang tạo**, từ danh sách hộp rỗng **đã có sức chứa** (ảnh, tên, số bánh/hộp, số vỏ trong kho). Admin không phải tạo hộp rỗng mới. Nếu chưa có hộp rỗng nào dùng được, trang hiện nút "Thêm hộp rỗng" và không cho tạo.
- Bấm "Tạo hộp quà": hệ thống tạo sản phẩm, SKU hộp quà và **công thức nháp gắn vỏ đã chọn trong một transaction**, rồi chuyển sang bước chọn bánh. Công thức còn nháp thì vẫn đổi được vỏ.
- Hộp quà mới **luôn ẩn** cho tới bước "Đóng hộp & mở bán"; trang tạo không còn công tắc "Hiển thị trên cửa hàng ngay". Thẻ bên phải ghi "Giá bán" (hộp quà chỉ có một SKU ngầm).
- Ở bước chọn bánh, thanh dưới cùng hiện **"Giá lẻ vỏ + bánh"** (giá vỏ + giá lẻ từng bánh × số lượng, cập nhật khi đổi số bánh) cạnh **giá bán** để Admin đối chiếu. Đây chỉ là số tham khảo, không đổi giá bán.

### Thay đổi chính trong phiên bản 1.19

- **Sức chứa hộp nằm trong phiên bản hộp rỗng.** Khi tạo hộp rỗng hoặc thêm/sửa phiên bản hộp rỗng, Admin nhập **sức chứa (1–24 bánh)** và chất liệu ngay trong form phiên bản; hệ thống lưu `PRODUCT_VARIANT` và `BOX` trong **một transaction**. Không còn bước "mẫu hộp" riêng. Mỗi phiên bản hộp rỗng có đúng một sức chứa (`UQ_BOX_empty_box_variant_id`).
- Phiên bản hộp rỗng **bắt buộc có sức chứa**; phiên bản hộp rỗng không có ô khối lượng hay nhân bánh.
- Hộp đã được dùng trong công thức hộp quà hoặc hộp khách tự chọn thì **không đổi được sức chứa**; muốn số bánh khác thì tạo phiên bản hộp mới.
- Chỉ còn **một công tắc "Mở bán phiên bản"**. Hộp có sức chứa luôn chọn được khi làm công thức hộp quà; khách chỉ thấy hộp ở trang tự chọn khi phiên bản đang mở bán.
- **Nhập kho theo loại:**
  - **Bánh:** ngày sản xuất bắt buộc ngay trên form; HSD = ngày sản xuất + thời hạn của bánh.
  - **Hộp rỗng:** không có ngày sản xuất và HSD; vỏ hộp không hết hạn.
  - **Hộp quà:** **không nhập trực tiếp**; chỉ vào kho bằng **Đóng hộp quà** (trừ vỏ hộp và bánh, HSD theo bánh bên trong, xem v1.21). Dòng hộp quà ở trang Kho có nút "Đóng hộp" thay cho "Nhập". "Điều chỉnh" vẫn dùng để sửa sai khi kiểm kê.
- **Sửa lô:** chỉ lô **bánh** sửa được ngày sản xuất (HSD tự tính lại). Lô hộp quà giữ ngày đóng hộp và HSD đã tính lúc đóng; lô hộp rỗng không có ngày; cả hai chỉ đổi được mã lô.
- Đổi thời hạn sử dụng của sản phẩm chỉ áp dụng cho lô nhập sau; lô đã nhập giữ HSD cũ.

### Thay đổi chính trong phiên bản 1.18

- **Số lượng bán được cho khách** (trang sản phẩm, giỏ hàng, trang thanh toán) = tồn có thể bán trừ đi số còn lại trong các lô **đã quá HSD**. Khách không thấy "còn hàng" với hàng đã hết hạn; bước đặt hàng chỉ lấy từ lô còn hạn như trước.
- **Không nhập được lô đã hết hạn:** nếu ngày sản xuất + thời hạn sử dụng trước hôm nay (giờ Việt Nam) thì báo "Lô này đã hết hạn (HSD …), không nhập được." và không đổi tồn.
- Nếu tới bước đặt hàng mà lô còn hạn không đủ (ví dụ lô vừa hết hạn), khách nhận câu "<tên sản phẩm>: vượt quá số lượng sản phẩm trong kho…" thay cho câu chứa mã SKU.
- **Trang Tồn kho của Admin** hiển thị "trong đó N hết hạn" và nút **"Hủy hàng hết hạn"**: hệ thống ghi một điều chỉnh âm đúng bằng số hết hạn (lý do "Hủy hàng hết hạn", có lịch sử kho), trừ từ các lô hết hạn trước.
- Câu báo khi khách đặt vượt tồn (trang sản phẩm, giỏ, thanh toán, đặt hàng) là **"Vượt quá số lượng sản phẩm trong kho"**, không ghi số còn lại. Thao tác của Admin (điều chỉnh, đóng hộp) vẫn ghi số.

### Thay đổi chính trong phiên bản 1.17

- Mỗi sản phẩm **bánh** và **hộp quà** có **thời hạn sử dụng tính theo ngày** (1–90 ngày; chọn nhanh 7, 15, 30, 45, 60, 90). Vỏ hộp không có thời hạn. Không dùng mốc 6 hay 12 tháng vì không hợp với bánh Trung Thu.
- **Nhập kho:** với bánh và hộp quà, **ngày sản xuất là bắt buộc** và **HSD của lô tự tính = ngày sản xuất + thời hạn**; Admin không nhập HSD tay. Sửa ngày sản xuất của lô thì HSD tự tính lại.
- **Đóng hộp quà:** lô hộp quà có ngày sản xuất là ngày đóng hộp và HSD là **ngày sớm hơn** giữa (ngày đóng hộp + thời hạn hộp quà) và HSD sớm nhất của các bánh bỏ vào hộp.
- Ngày sản xuất không được sau **hôm nay theo giờ Việt Nam**; ngày lọc lô còn hạn khi bán và ngày trong mã lô cũng theo giờ Việt Nam.
- **Trang sản phẩm của khách** hiển thị "Hạn sử dụng: N ngày kể từ ngày sản xuất" và "Lô đang bán: NSX … · HSD …" của lô sẽ bán ra tiếp theo (còn hàng, chưa hết hạn, hạn gần nhất); đổi phiên bản thì đổi theo. **Trang sửa sản phẩm và trang hộp quà của Admin** hiển thị cùng thông tin lô đang bán, nên Admin và khách luôn thấy giống nhau.
- Ô chữ "Hạn sử dụng" cũ không còn nhập trên form; dữ liệu cũ chỉ dùng khi sản phẩm chưa có số ngày. Cách bảo quản vẫn ở ô "Cách bảo quản".
- Flyway V20 thêm cột `PRODUCT.shelf_life_days` (trống hoặc 1–90, ràng buộc `CK_PRODUCT_shelf_life_days`) và điền 30 ngày cho bánh và hộp quà hiện có.

### Thay đổi chính trong phiên bản 1.16

- Thêm trạng thái đơn **`DELIVERED` (Đã giao)** giữa `SHIPPING` và `COMPLETED`. Luồng mới: `SHIPPING → DELIVERED → COMPLETED`.
- **Admin** bấm **"Đã giao hàng"** khi đơn vị vận chuyển báo giao xong: đơn sang `DELIVERED`, ghi `delivered_at`. Đơn COD được ghi đã thu tiền (`PAID`, `paid_at`) ngay trong cùng transaction này.
- **Khách** bấm **"Đã nhận được hàng"** ở trang chi tiết đơn khi đơn đang `DELIVERED`: đơn sang `COMPLETED`, ghi `completed_at` và `customer_confirmed_at`.
- Khách không bấm thì **hệ thống tự hoàn tất** đơn `DELIVERED` sau **3 ngày** kể từ `delivered_at` (`completed_at` có giá trị, `customer_confirmed_at` để trống).
- Đơn `DELIVERED` không hủy được, không sửa thông tin nhận hàng. Khách chưa nhận được hàng thì liên hệ cửa hàng trước hạn tự hoàn tất; đổi trả và khiếu nại vẫn ngoài phạm vi MVP.
- Doanh thu và món bán chạy trên dashboard tính cả đơn `DELIVERED` lẫn `COMPLETED`.
- Flyway V19 mở rộng `CK_SALES_ORDER_status` thêm `DELIVERED` và thêm hai cột `delivered_at`, `customer_confirmed_at` vào `SALES_ORDER`.

### Thay đổi chính trong phiên bản 1.15

- **Mốc thời gian của đơn:** hệ thống tự ghi giờ khi đơn chuyển sang Đang xử lý (`processing_at`), Đang giao (`shipping_at`), Hoàn thành (`completed_at`) và khi bị hủy (`cancelled_at`). Trang chi tiết đơn của khách và của Admin hiển thị giờ của từng bước. Đơn trước v1.15 không có các giờ này.
- **Thông tin giao hàng:** khi bàn giao vận chuyển (`PROCESSING → SHIPPING`), Admin bắt buộc chọn **đơn vị vận chuyển** (Giao Hàng Nhanh, Giao Hàng Tiết Kiệm, Viettel Post, Cửa hàng tự giao), nhập **mã vận đơn** (bắt buộc trừ khi cửa hàng tự giao; lưu chữ in hoa, tối đa 50 ký tự) và chọn **ngày giao dự kiến** (không trước hôm nay; mặc định hôm nay + 2 ngày). Khách thấy "Giao bởi ..., mã vận đơn ..." và "Dự kiến giao: ..." cho tới khi đơn hoàn thành. Đây là thông tin nhập tay; hệ thống không gọi API của đơn vị vận chuyển.
- **Khách không chọn ngày nhận hàng.** Ngày giao do cửa hàng và đơn vị vận chuyển quyết định; khách có thể ghi mong muốn vào ghi chú đơn hàng.
- **Đăng nhập:** tài khoản Admin đăng nhập xong vào thẳng `/admin/overview` (hoặc trang quản trị đang mở trước đó); khách hàng giữ hành vi cũ. Admin xem cửa hàng qua liên kết "Xem cửa hàng".
- **Nhập kho theo lô:** ngày sản xuất không được sau hôm nay, cả khi nhập kho lẫn khi sửa thông tin lô.
- Flyway V18 thêm 7 cột vào `SALES_ORDER` (`processing_at`, `shipping_at`, `completed_at`, `cancelled_at`, `shipping_carrier`, `tracking_code`, `estimated_delivery_date`) và ràng buộc `CK_SALES_ORDER_shipping_carrier`. Không thêm bảng.

### Thay đổi chính trong phiên bản 1.14

- Khi hủy đơn, dòng hoàn kho `STOCK_MOVEMENT (CANCEL_RETURN)` ghi **người thực hiện** vào cột có sẵn `performed_by_account_id`: tài khoản Admin khi Admin hủy, tài khoản khách khi khách tự hủy, để trống khi hệ thống tự hủy đơn VNPay quá hạn.
- Trang chi tiết đơn đã hủy của khách hiển thị: "Đơn hàng đã được cửa hàng hủy. Nếu cần hỗ trợ, vui lòng liên hệ cửa hàng." (thêm "Cửa hàng sẽ liên hệ để hoàn tiền cho bạn." nếu đơn đã thanh toán), "Bạn đã hủy đơn hàng này.", hoặc câu chung "Đơn hàng này đã bị hủy và không còn được xử lý." khi không có người thực hiện (hệ thống tự hủy, hoặc đơn hủy trước v1.14).
- Không thêm bảng hay cột.

### Thay đổi chính trong phiên bản 1.13

- Admin có trang **Khách hàng** (`/admin/customers`): tìm theo email hoặc số điện thoại, xem số đơn, **khóa** và **mở khóa** tài khoản `CUSTOMER`. Dùng `ACCOUNT.status` sẵn có (`ACTIVE`/`DISABLED`).
- Khóa có **hiệu lực ngay**: phiên đăng nhập đang mở của khách bị hết hạn; bước đặt hàng kiểm tra lại tài khoản còn `ACTIVE` trong cùng transaction.
- Khi khóa, Admin chọn có **hủy luôn các đơn chưa thanh toán đang chờ** (`PLACED` + payment `UNPAID`) hay không; mặc định là có. Đơn đã thanh toán, đang xử lý hoặc đang giao **không bao giờ bị hủy tự động**; Admin xử lý từng đơn.
- Đơn của khách đang bị khóa hiện nhãn **"Khách đã bị khóa"** trên trang đơn hàng của Admin.
- Không xóa tài khoản, không sửa thông tin hộ khách, không đặt lại mật khẩu hộ, không khóa tài khoản Admin. Không lưu lý do khóa (cần migration, để phiên bản sau).

### Thay đổi chính trong phiên bản 1.12

- Khách `CUSTOMER` đã đăng nhập có trang **Tài khoản của tôi** (`/account`): xem email, sửa **họ tên** và **số điện thoại**, **đổi mật khẩu**.
- Email là tên đăng nhập nên **chỉ hiển thị, không sửa được**.
- Đổi mật khẩu phải nhập đúng mật khẩu hiện tại; mật khẩu mới theo quy tắc đăng ký và phải khác mật khẩu hiện tại. Đổi xong vẫn giữ đăng nhập; các link quên mật khẩu chưa dùng bị vô hiệu.
- Đơn đã đặt giữ nguyên snapshot người mua. Đơn đặt sau khi sửa dùng thông tin mới.
- Không thêm bảng hay cột; chỉ cập nhật `CUSTOMER.full_name`, `CUSTOMER.phone` và `ACCOUNT.password_hash`.

### Thay đổi chính trong phiên bản 1.11

- **Không cho vượt tồn từ sớm.** Thêm vào giỏ hoặc tăng số lượng vượt số còn bán bị từ chối với thông báo "Vượt quá số lượng sản phẩm trong kho"; giảm số lượng luôn được phép. Dòng giỏ vượt tồn khóa nút thanh toán. Trang thanh toán kiểm tra lại tổng nhu cầu theo SKU (cả bánh trong hộp CUSTOM) và đưa khách về giỏ nếu thiếu. Bước đặt hàng vẫn trừ kho có điều kiện như cũ.
- **Không giới hạn số lượng mỗi đơn**, để giữ nguyên các bậc chiết khấu đến 500 hộp.
- **COD chỉ cho đơn có tổng thanh toán đến 5.000.000 VND.** Đơn lớn hơn chỉ thanh toán VNPay; trang thanh toán khóa lựa chọn COD và chọn sẵn VNPay.
- Mỗi khách có tối đa **2 đơn COD chờ xử lý** (`PLACED`, payment `UNPAID`) và **1 đơn VNPay chưa thanh toán** cùng lúc.
- Đơn VNPay chưa thanh toán **tự hủy sau 15 phút** (trước đây 30 phút), khớp hạn giao dịch VNPay 15 phút; hệ thống kiểm tra mỗi phút. Vẫn hỏi VNPay (`querydr`) trước khi hủy như v1.9.
- Khách có **từ 3 đơn VNPay bị hủy khi chưa thanh toán trong 24 giờ** bị **tạm khóa đặt đơn mới (cả COD và VNPay)** cho tới khi số đơn đó trong 24 giờ gần nhất giảm dưới 3. Thông báo nêu lý do và mời liên hệ cửa hàng. Chỉ đếm đơn VNPay vì đơn COD bị hủy có thể do Admin hủy.
- Trang sản phẩm chỉ hiện số lượng cụ thể khi sắp hết (từ ngưỡng cảnh báo trở xuống) hoặc khi khách đặt vượt; còn lại hiện còn hàng.
- Luật riêng của từng phương thức (ngưỡng COD, số đơn chờ) nằm trong `PaymentProcessor` (Strategy); luật khóa đặt đơn áp dụng chung trong `CheckoutService`, kiểm tra cả khi mở trang thanh toán. Checkout không rẽ nhánh theo phương thức. Không thêm bảng hay cột.

### Thay đổi chính trong phiên bản 1.10

- PRESET đã `READY` được Admin **mở khóa về `DRAFT`** để sửa thành phần khi **cả bốn** điều kiện đều đúng: SKU GIFT_SET đang ngừng bán, chưa có tồn thành phẩm hay lịch sử kho, không nằm trong giỏ hàng nào và chưa được đơn nào tham chiếu. Server kiểm tra lại các điều kiện này trong cùng giao dịch khi mở khóa.
- Khi PRESET đã đóng hộp, đã bán hoặc đã được tham chiếu, cấu hình vẫn **bất biến** như trước. Admin có thể **tạo bản sao** hộp quà: hệ thống tạo SKU GIFT_SET mới (ngừng bán) và PRESET `DRAFT` mới chép sẵn vỏ hộp và danh sách bánh để sửa; hộp quà cũ giữ nguyên.
- Không thêm bảng hay cột.

### Thay đổi chính trong phiên bản 1.9

- Đơn VNPay chưa thanh toán **tự hủy** khi đã quá **30 phút** kể từ lúc đặt **và** không còn giao dịch VNPay nào trong hạn 15 phút. Nếu khách bấm thanh toán lại sát giờ, hạn hủy lùi tới lúc giao dịch đó hết hạn, nên đơn không bao giờ bị hủy khi khách đang ở cổng thanh toán.
- Hệ thống kiểm tra 5 phút một lần. Trước khi hủy luôn hỏi VNPay (`querydr`): đã trả thì ghi `PAID` và giữ đơn; không kết nối được VNPay thì để lại, lần kiểm tra sau thử lại. Hủy đi qua đúng luồng hủy hiện có nên hoàn tồn đúng một lần.
- Đơn COD không bao giờ tự hủy.
- Trang chi tiết đơn VNPay chờ thanh toán hiển thị giờ tự hủy và đồng hồ đếm ngược.
- Không thêm bảng hay cột; dùng thời điểm đặt đơn và `VNPAY_ATTEMPT` sẵn có.

### Thay đổi chính trong phiên bản 1.8

- Góp ý của khách kèm **số sao từ 1 đến 5**. Khách phải đăng nhập để gửi; hệ thống lấy khách từ tài khoản đang đăng nhập, không nhận `customer_id` từ form.
- Góp ý mới luôn ở trạng thái **ẩn**. Chỉ Admin được bấm **Hiển thị** hoặc **Ẩn**; góp ý cũ chưa có số sao không thể hiển thị.
- Trang chủ hiển thị tối đa 20 đánh giá đã được duyệt **và từ 4 sao trở lên**, mới nhất trước, gồm số sao, nội dung và tên khách. Trang chủ không hiển thị điểm trung bình, vì danh sách đã được lọc. Admin vẫn xem được mọi góp ý, kể cả đánh giá thấp. Form góp ý báo trước cho khách rằng đánh giá được duyệt có thể hiển thị kèm tên.
- Flyway V13 thêm `FEEDBACK.rating` (`INT NULL`, kiểm tra 1–5) và `FEEDBACK.is_published` (`BIT NOT NULL`, mặc định 0). Không thêm bảng; schema vẫn **24 bảng**.

### Thay đổi chính trong phiên bản 1.7

- Khách chọn **COD** hoặc **VNPay Sandbox** khi checkout. Phương thức được **cố định từ lúc đặt**; không còn nút chuyển đơn COD sang VNPay.
- Đơn VNPay chưa trả tiền ở trạng thái "chờ thanh toán" (`PLACED` + `UNPAID`). Mỗi lần thanh toán là một giao dịch 15 phút; khách được **thanh toán lại** khi không còn giao dịch đang chờ.
- Đơn VNPay chưa trả tiền **được hủy ngay** (khách hoặc Admin). Khi bấm hủy, hệ thống tự hỏi VNPay một lần: đã trả thì từ chối, chưa trả thì đóng giao dịch và hủy. Sửa lỗi đơn bị kẹt mãi ở `PLACED`.
- Tiền VNPay về **sau khi đơn đã hủy** được ghi nhận `PAID` và hiển thị "cần hoàn tiền" cho Admin.
- Đơn VNPay đã `PAID`: khách không tự hủy; Admin được hủy khi `PLACED`/`PROCESSING` và **hoàn tiền thủ công** qua cổng merchant VNPay (MVP chưa tự động hoàn tiền).
- Admin chỉ chuyển đơn VNPay sang `PROCESSING` khi đã `PAID`; hoàn tất đơn VNPay không thu COD lần nữa.
- Sửa thông tin nhận hàng áp dụng cho mọi đơn `PLACED` thuộc khách, **không phụ thuộc phương thức thanh toán**, vì không làm đổi số tiền.
- Flyway V9 thêm `VNPAY_ATTEMPT` và cho phép `PAYMENT.payment_method = VNPAY`. V10 (`FEEDBACK`), V11 (`PRODUCT_VIDEO`) và V12 (`STOCK_BATCH`, `BATCH_ALLOCATION`) thuộc các tính năng góp ý, video sản phẩm và lô hàng; schema hiện có **24 bảng**.

### Thay đổi chính trong phiên bản 1.6

- Khách được sửa tên người nhận, số điện thoại, địa chỉ và ghi chú giao hàng của **đơn thuộc chính mình** khi đơn còn `PLACED`.
- Việc sửa thông tin nhận hàng không thay đổi người mua, sản phẩm, số lượng, giá, chiết khấu, phí giao hàng, tổng tiền, payment hoặc tồn kho.
- Cập nhật phải kiểm tra lại ownership từ principal `CUSTOMER` và dùng conditional update theo trạng thái `PLACED`; nếu đơn đồng thời chuyển sang xử lý thì yêu cầu sửa thất bại.
- Từ `PROCESSING` trở đi, khách không tự sửa thông tin nhận hàng mà liên hệ cửa hàng.
- Quyết định này dùng các cột snapshot sẵn có trong `SALES_ORDER`, không thêm bảng/cột và không cần Flyway migration mới.

### Thay đổi chính trong phiên bản 1.5

- Tổng tiền phải thu COD được làm tròn đến 1.000 VND gần nhất theo `HALF_UP`: phần lẻ dưới 500 VND làm tròn xuống, từ 500 VND làm tròn lên.
- Chỉ làm tròn một lần trên tổng cuối sau chiết khấu và phí giao hàng; không làm tròn lại từng dòng hàng.
- `SALES_ORDER.rounding_adjustment` lưu chênh lệch làm tròn; `total_amount` và `PAYMENT.amount` đều dùng số tiền sau làm tròn.
- Đơn lịch sử nhận `rounding_adjustment = 0` và giữ nguyên tổng tiền cũ. Flyway V8 không thêm bảng mới.

### Thay đổi chính trong phiên bản 1.4

- Nhóm hộp quà `GIFT_BOX` dùng sáu bậc: 5–19 = 5%, 20–49 = 8%, 50–99 = 10%, 100–199 = 12%, 200–499 = 15%, từ 500 = 18%.
- Nhóm bánh lẻ `MOONCAKE` giữ hai bậc cũ: 50–99 = 5%, từ 100 = 10%.
- Ngưỡng phí giao hàng giữ nguyên và độc lập với chiết khấu: dưới 50 hộp là 30.000 VND, từ 50 hộp là 150.000 VND.
- Flyway V5 vô hiệu hóa tier `GIFT_BOX` cũ và thêm sáu tier mới; không xóa tier lịch sử và không thay đổi cấu trúc 19 bảng.
- Flyway V6 chỉ bổ sung đường dẫn ảnh nội bộ cho 16 SKU catalog hiện có; không thêm sản phẩm, thay đổi nghiệp vụ hoặc thay đổi cấu trúc bảng.
- Flyway V7 thay ảnh đại diện của hai SKU vỏ hộp CUSTOM bằng ảnh thể hiện đúng hộp 4 ngăn và 6 ngăn; không đổi giá, sức chứa hay nghiệp vụ.

### Thay đổi chính trong phiên bản 1.3

- Khách đã đăng nhập được tự hủy **đơn của chính mình** khi đơn còn `PLACED`.
- Admin được hủy đơn khi còn `PLACED` hoặc `PROCESSING`; khách không tự hủy đơn đã sang `PROCESSING` mà liên hệ cửa hàng để được xử lý.
- Từ `SHIPPING` trở đi, cả khách và Admin đều không hủy theo luồng MVP; đổi/trả sau giao vẫn ngoài phạm vi.
- Mọi điểm vào hủy đơn dùng chung một transaction/service để chuyển trạng thái có điều kiện, hoàn đúng tồn đã trừ và ghi `CANCEL_RETURN` đúng một lần.
- Quyết định này không thêm bảng/cột/trạng thái và không cần Flyway migration mới.

### Thay đổi chính trong phiên bản 1.2

- Khách chưa đăng nhập vẫn được xem catalog và chi tiết sản phẩm nhưng không được tạo/sửa giỏ, tạo hộp CUSTOM hoặc checkout.
- Khách được tự đăng ký tài khoản `CUSTOMER`; đăng ký tạo `ACCOUNT` và `CUSTOMER` nguyên tử, sau đó có thể đăng nhập để mua hàng.
- Bỏ guest cart/guest checkout khỏi hành vi MVP của ứng dụng. Các cột nullable liên quan guest vẫn được giữ trong schema V1 đã áp dụng nhưng backend mới không sử dụng chúng.
- Chốt điều hướng: thao tác mua khi chưa đăng nhập đưa khách đến màn hình đăng nhập; không thực hiện âm thầm thao tác mua trước khi xác thực.
- Phân lại ownership: một luồng Customer Sales sở hữu customer authentication và toàn bộ luồng bán; luồng Admin dùng chung nền tảng bảo mật và không tạo `SecurityConfig` thứ hai.

### Hiệu chỉnh validation trong phiên bản 1.2.1

- Số điện thoại đăng ký gồm đúng 10 chữ số, bắt đầu bằng `0`; không nhận khoảng trắng, chữ, dấu phân cách hoặc mã quốc gia `+84` trong MVP.
- Email được loại bỏ khoảng trắng hai đầu trước khi kiểm tra định dạng và độ dài.
- Mật khẩu vẫn có 8–64 ký tự, đồng thời không vượt quá giới hạn 72 byte UTF-8 của BCrypt; input vượt giới hạn phải nhận lỗi validation thân thiện.

### Thay đổi chính trong phiên bản 1.1

- Chốt thời điểm khóa giá, cách làm tròn chiết khấu VND và công thức tổng hợp tiền đơn.
- Chốt một `CART_ITEM` cho mỗi đích mua trong một giỏ; thêm lại cùng đích thì tăng số lượng.
- Chốt quy ước snapshot thành phần cho bánh/hộp rời, PRESET và CUSTOM.
- Chốt PRESET đã READY/đã đại diện hàng đóng sẵn là bất biến; đổi thành phần phải tạo SKU và PRESET configuration mới.
- Chốt state machine đơn hàng, quan hệ giữa giao hàng thành công và thu COD.
- Làm rõ checkout một lần bằng chuyển trạng thái `ACTIVE → CHECKED_OUT` nguyên tử, vòng đời giỏ, SKU bị ngừng bán, tier giảm giá chồng nhau và phí ship khi đơn không có hộp quà.
- Làm rõ khóa idempotency cho SALE, CANCEL_RETURN, PACK và trách nhiệm tạo `INVENTORY_BALANCE` cho SKU có quản lý tồn.

### Hiệu chỉnh trạng thái trong phiên bản 1.1.1

- Đồng bộ câu chữ hậu Flyway: V1/V2 đã được tạo, chia sẻ và kiểm thử; các checklist cũ “trước khi tạo V1” được chuyển thành hồ sơ xác minh và quy tắc cho migration tiếp theo.
- Không thay đổi bất kỳ quyết định nghiệp vụ, bảng, cột, ràng buộc hoặc dữ liệu seed nào của phiên bản 1.1.

### Thứ tự ưu tiên khi có khác biệt

1. Business Rules đã được người phụ trách xác nhận là nguồn quyết định **hành vi nghiệp vụ**.
2. Flyway migration đang áp dụng là nguồn quyết định **cấu trúc dữ liệu thực thi**; Physical ERD/DDL là tài liệu thiết kế đối chiếu và phải được giữ đồng bộ bằng tài liệu/migration mới đã duyệt.
3. Sau khi Flyway migration đã được áp dụng ở môi trường dùng chung, không sửa file migration cũ; mọi thay đổi đi bằng migration mới.
4. Source code phải tuân theo Business Rules và schema đang được Flyway quản lý; không dùng code để âm thầm thay đổi nghiệp vụ.

## 1. Phạm vi MVP

MVP hỗ trợ bán bánh Trung Thu trên web (Spring Boot MVC + Thymeleaf), đăng ký/đăng nhập khách hàng, giỏ hàng, checkout, đặt hàng, thanh toán COD hoặc VNPay Sandbox, chiết khấu theo số lượng và trang Admin quản lý sản phẩm/đơn hàng/tồn kho. Khách chưa đăng nhập được xem catalog; khách **phải đăng nhập bằng tài khoản CUSTOMER đang ACTIVE** trước khi thêm vào giỏ, tạo hộp CUSTOM hoặc đặt hàng.

**Bốn cách mua hàng:**

1. **Bánh lẻ:** mua `PRODUCT` loại `MOONCAKE` qua SKU trong `PRODUCT_VARIANT`.
2. **Hộp rỗng:** mua `PRODUCT` loại `EMPTY_BOX` qua SKU. Sức chứa của mỗi phiên bản hộp rỗng lưu trong `BOX` và được nhập cùng form phiên bản.
3. **Hộp quà có sẵn:** mua `PRODUCT` loại `GIFT_SET` (SKU hộp đã đóng sẵn, **có tồn kho riêng**). Thành phần hộp được mô tả bằng `BOX_CONFIGURATION` loại `PRESET`.
4. **Hộp tự chọn:** chọn `BOX` rồi chọn đủ bánh theo sức chứa để tạo `BOX_CONFIGURATION` loại `CUSTOM`. Khách có thể bắt đầu từ hộp hoặc bánh, nhưng kết quả lưu **một cấu hình**.

**Không tự ghép hàng rời:** 1 hộp rỗng + 5 bánh lẻ trong giỏ vẫn là các dòng hàng riêng, trừ khi khách chủ động tạo hộp tự chọn.

## 2. Danh mục, SKU và mẫu hộp

- `CATEGORY` nhóm các sản phẩm; `PRODUCT` giữ thông tin chung; `PRODUCT_VARIANT` là SKU có thể bán, giá và trạng thái hoạt động. `PRODUCT_IMAGE` chứa ảnh hiển thị.
- `PRODUCT.product_type` chỉ thuộc `MOONCAKE`, `EMPTY_BOX`, `GIFT_SET` trong MVP. **`GIFT_SET` (loại sản phẩm) khác `GIFT_BOX` (nhóm chiết khấu)**; không đổi tên/đồng nhất hai giá trị khi viết code.
- `BOX.empty_box_variant_id` trỏ đến đúng một SKU `EMPTY_BOX`; `BOX.capacity_cakes` là số bánh tối đa/đúng chuẩn của hộp. MVP yêu cầu **đúng sức chứa** khi hoàn tất hộp tự chọn (ví dụ hộp 4 bánh phải có đúng 4 bánh).
- `PRODUCT_VARIANT.unit_price` là giá bán SKU. Đối với hộp quà PRESET, **giá bán lấy theo SKU GIFT_SET**; không tự cộng lại giá bánh/vỏ hộp tại checkout.
- Với hộp CUSTOM, **giá một bộ trước chiết khấu = giá SKU hộp rỗng + tổng (giá SKU từng bánh × số bánh đó trong một hộp)**. Số bộ mua là số lượng trên `CART_ITEM`/`ORDER_ITEM`.
- SKU ngừng bán (`is_active = 0`) không được thêm mới vào giỏ/đặt mới; dữ liệu sản phẩm cũ vẫn phải được giữ để tra cứu đơn cũ.
- Giá và chính sách giảm giá hiển thị trong giỏ chỉ là **tạm tính**, không phải cam kết giữ giá. Checkout luôn đọc lại giá, trạng thái SKU và tier đang hiệu lực từ backend.
- Nếu SKU bị ngừng bán sau khi đã được thêm vào giỏ, checkout phải từ chối các dòng bị ảnh hưởng và không tạo đơn một phần. Hệ thống thông báo rõ dòng hàng cần xóa/thay thế; không tự bỏ dòng rồi đặt phần còn lại.
- Giá chỉ được khóa sau khi checkout thành công và được lưu vào snapshot của `ORDER_ITEM`/`ORDER_ITEM_COMPONENT`. Thay đổi giá sau đó không làm đổi đơn cũ.
- Tên bánh, thành phần, hạn sử dụng và hướng dẫn bảo quản chỉ hiển thị theo dữ liệu thật do cửa hàng cung cấp; **không tự sinh thông tin thực phẩm**.

## 3. Cấu hình hộp quà

- `BOX_CONFIGURATION.configuration_type = PRESET`: cấu hình do cửa hàng định nghĩa, có `preset_variant_id` trỏ đến SKU `GIFT_SET`, **không gắn giỏ khách** (`cart_id IS NULL`).
- `BOX_CONFIGURATION.configuration_type = CUSTOM`: cấu hình do khách tạo, gắn với `CART` chủ sở hữu (`cart_id` có giá trị), **không gắn SKU PRESET** (`preset_variant_id IS NULL`).
- `BOX_CONFIGURATION_ITEM` liệt kê SKU bánh `MOONCAKE` và `quantity_per_box`. Không dùng SKU hộp rỗng hoặc SKU GIFT_SET làm bánh bên trong.
- PRESET chỉ được chỉnh ở trạng thái `DRAFT`. Từ v1.10, PRESET `READY` được mở khóa về `DRAFT` nếu SKU GIFT_SET đang ngừng bán, chưa có tồn thành phẩm hay lịch sử kho, không nằm trong giỏ hàng và chưa được đơn nào tham chiếu. Khi SKU GIFT_SET đã được mở bán, đã có tồn thành phẩm hoặc đã được giỏ/lịch sử tham chiếu, cấu hình và các `BOX_CONFIGURATION_ITEM` của nó là **bất biến**; muốn đổi thành phần phải tạo SKU GIFT_SET và PRESET mới (có thể dùng chức năng tạo bản sao).
- Nếu công thức hộp PRESET thay đổi, Admin phải tạo **SKU GIFT_SET mới + BOX_CONFIGURATION PRESET mới + danh sách component mới**. SKU/config cũ được giữ nguyên để bán hết tồn phù hợp hoặc chuyển `is_active = 0`; không gắn thành phần mới lên SKU của hàng đã đóng sẵn.
- Database hiện bảo vệ quan hệ một PRESET trên mỗi `preset_variant_id`, nhưng không dùng trigger để cấm `UPDATE`. Quy tắc bất biến PRESET phải được bảo vệ tại Admin/Service và bằng quyền truy cập database; không cho người dùng nghiệp vụ sửa trực tiếp các bảng cấu hình.
- Cấu hình CUSTOM ở trạng thái nháp (`DRAFT`) có thể chưa đủ bánh. Chỉ được đánh dấu sẵn sàng (`READY`), cho vào checkout và đặt hàng khi **tổng `quantity_per_box` bằng `BOX.capacity_cakes`**.
- Chỉ khách hàng đã đăng nhập sở hữu giỏ mới được sửa cấu hình CUSTOM. Hệ thống lấy `customer_id` từ phiên đăng nhập, kiểm tra quyền sở hữu và kiểm tra `CART_ITEM.cart_id` khớp `BOX_CONFIGURATION.cart_id`; không tin ID do form gửi lên làm bằng chứng sở hữu.
- Quy ước snapshot `ORDER_ITEM_COMPONENT` được khóa như sau:
  - Bánh lẻ và hộp rỗng mua rời (`ORDER_ITEM` loại STANDARD): không tạo component; toàn bộ snapshot nằm trên `ORDER_ITEM`.
  - Hộp PRESET/GIFT_SET: snapshot **1 thành phần BOX** (`quantity_per_set = 1`) và các thành phần CAKE theo công thức PRESET tại checkout để hiển thị lịch sử. Tên/giá component là thông tin snapshot, không dùng để cộng lại giá bán PRESET; tồn kho vẫn chỉ trừ SKU GIFT_SET thành phẩm và `inventory_source_snapshot = STOCKED_SKU`.
  - Hộp CUSTOM: snapshot **1 thành phần BOX** (`quantity_per_set = 1`) và các thành phần CAKE, gồm tên SKU, số lượng trên mỗi bộ và giá tại checkout. Đây là dấu vết các thành phần dùng để tính giá và xuất kho; `inventory_source_snapshot = COMPONENTS`.
  - Các component cùng SKU trong một hộp phải được cộng gộp thành một dòng snapshot; `quantity_per_set` là số lượng trong **một bộ**, còn số bộ nằm trên `ORDER_ITEM.quantity`.
- Không cho sửa hoặc xóa cấu hình đã được một đơn thành công tham chiếu. Giỏ/config có thể được sửa trước checkout; sau checkout, `ORDER_ITEM` và `ORDER_ITEM_COMPONENT` là lịch sử bất biến qua luồng nghiệp vụ thông thường.

## 4. Đăng ký, đăng nhập và giỏ hàng

- Catalog, trang chi tiết sản phẩm và tài nguyên tĩnh là công khai. Xem giỏ, thêm/sửa/xóa dòng giỏ, tạo/sửa CUSTOM configuration, checkout, xem lịch sử/chi tiết đơn và yêu cầu tự hủy đơn yêu cầu tài khoản `CUSTOMER` đang `ACTIVE`.
- Khách được tự đăng ký bằng email, mật khẩu, họ tên và số điện thoại. Backend luôn gán `ACCOUNT.role = CUSTOMER`; không nhận role/status/account ID từ form đăng ký.
- `ACCOUNT.email` là tên đăng nhập; MVP không có username riêng. Backend loại bỏ khoảng trắng hai đầu và chuyển email về chữ thường trước khi kiểm tra trùng, lưu hoặc đăng nhập.
- Mật khẩu đăng ký có từ 8 đến 64 ký tự và phải nhập xác nhận khớp. Đăng ký thành công chuyển khách về trang đăng nhập; không tự động đăng nhập.
- Số điện thoại đăng ký gồm đúng 10 chữ số và bắt đầu bằng `0`. MVP không nhận khoảng trắng, dấu phân cách hoặc mã quốc gia `+84` trong trường này.
- Do sử dụng BCrypt, mật khẩu đăng ký không được vượt quá 72 byte khi mã hóa UTF-8; backend phải từ chối bằng lỗi validation thay vì để quá trình hash thất bại.
- Đăng ký phải chuẩn hóa email nhất quán, từ chối email đã tồn tại, hash mật khẩu bằng `PasswordEncoder`, rồi tạo `ACCOUNT` và `CUSTOMER` trong **cùng một transaction**. Nếu một bước thất bại thì không lưu bản ghi còn lại.
- Do MVP không có xác minh email hay duyệt tài khoản, đăng ký thành công tạo `ACCOUNT.status = ACTIVE`. Social login, MFA, email verification và account approval vẫn ngoài phạm vi.
- Khách hàng (`CUSTOMER`, `ACTIVE`) được tự khôi phục mật khẩu qua email. Trang `/forgot-password` luôn trả cùng một thông báo dù email có tồn tại hay không, để không lộ tài khoản nào đã đăng ký.
- Mỗi yêu cầu khôi phục tạo một token ngẫu nhiên (ít nhất 256 bit) có hạn **30 phút**. Database chỉ lưu SHA-256 của token trong `PASSWORD_RESET_TOKEN`; không lưu hay ghi log token gốc ngoài đường link gửi cho khách. Tạo token mới vô hiệu hóa các token chưa dùng trước đó của cùng tài khoản; cùng một tài khoản chỉ được yêu cầu lại sau 60 giây.
- Đặt lại mật khẩu dùng cùng quy tắc mật khẩu khi đăng ký (8–64 ký tự, tối đa 72 byte UTF-8, nhập xác nhận khớp). Token chỉ dùng được một lần: backend đánh dấu `used_at` bằng cập nhật có điều kiện `used_at IS NULL AND expires_at > now` rồi mới đổi `ACCOUNT.password_hash` trong **cùng một transaction**. Thành công chuyển khách về `/login`, không tự động đăng nhập.
- Khi chưa cấu hình SMTP (`MAIL_HOST` trống), ứng dụng chỉ ghi đường link khôi phục vào log để phục vụ demo local; môi trường thật phải cấu hình SMTP.
- **Tài khoản của tôi (v1.12).** Khách `CUSTOMER` đang `ACTIVE` mở `/account` để xem email và sửa thông tin của chính mình. Khách được xác định từ principal phía server; form không chứa và backend không nhận `customer_id` hay `account_id`.
- Sửa **họ tên** và **số điện thoại** theo đúng quy tắc khi đăng ký: họ tên bắt buộc, tối đa 40 ký tự, chỉ chữ cái và khoảng trắng, loại bỏ khoảng trắng hai đầu; số điện thoại đúng 10 chữ số và bắt đầu bằng `0`. Hai trường được cập nhật cùng nhau trong một transaction.
- **Email không sửa được** trong MVP vì là tên đăng nhập và địa chỉ nhận link khôi phục; đổi email cần xác minh email mới, nằm ngoài phạm vi.
- **Đổi mật khẩu** yêu cầu mật khẩu hiện tại, mật khẩu mới và xác nhận. Mật khẩu mới theo quy tắc đăng ký (8–64 ký tự, tối đa 72 byte UTF-8, xác nhận khớp) và phải khác mật khẩu hiện tại. Sai mật khẩu hiện tại thì không đổi gì. Thành công thì lưu hash mới bằng `PasswordEncoder`, vô hiệu mọi `PASSWORD_RESET_TOKEN` chưa dùng của tài khoản trong cùng transaction, và khách vẫn đăng nhập.
- Sửa thông tin không thay đổi các đơn đã đặt (snapshot giữ nguyên). Trang thanh toán của đơn mới tự điền họ tên và số điện thoại mới vào thông tin người nhận.
- `CUSTOMER.account_id` và `CUSTOMER.email` được backend gắn với tài khoản vừa đăng ký. Trong luồng mua MVP hiện tại, mọi `CUSTOMER` được dùng làm chủ giỏ phải có `account_id`; mọi đơn thành công phải có `SALES_ORDER.customer_id`.
- Khi khách chưa đăng nhập bấm “Thêm vào giỏ”, “Mua ngay” hoặc bắt đầu hộp CUSTOM, ứng dụng đưa khách đến `/login` cùng một return URL nội bộ an toàn. Không tạo giỏ, không lưu pending cart mutation và không tự phát lại POST trước khi xác thực; sau đăng nhập, khách quay lại trang phù hợp và xác nhận thao tác mua.
- `CART` của luồng MVP ứng dụng thuộc về đúng một `CUSTOMER`: `customer_id` có giá trị và `guest_token IS NULL`. Một khách có tối đa một giỏ `ACTIVE` ở một thời điểm.
- Schema V1 vẫn cho phép owner bằng `guest_token` để giữ checksum và thiết kế 19 bảng đã áp dụng. Đây là khả năng cấu trúc chưa dùng; backend MVP hiện tại không tạo, đọc như giỏ hiện hành, gộp hoặc checkout guest cart. Không cần migration chỉ để thu hẹp hành vi ứng dụng.
- `CART_ITEM` lưu **đúng một** loại đích mua: `variant_id` (bánh lẻ, hộp rỗng, GIFT_SET) **hoặc** `configuration_id` (CUSTOM); `quantity` là số nguyên dương.
- Trong cùng một giỏ, mỗi `variant_id` hoặc `configuration_id` chỉ xuất hiện ở **một `CART_ITEM`**. Thêm lại cùng đích mua phải tăng `quantity`, không tạo dòng trùng. Backend vẫn phải cộng gộp theo SKU khi kiểm tra tồn để phòng dữ liệu cũ hoặc lỗi đồng thời.
- Thêm, xóa, đổi số lượng trong giỏ **không trừ tồn kho**, nhưng không được đặt số lượng vượt số còn bán (giảm luôn được phép). Trang thanh toán và bước đặt hàng kiểm tra lại tồn theo tổng nhu cầu mỗi SKU (từ v1.11).
- Checkout thành công phải chuyển giỏ hiện tại sang `CHECKED_OUT`. Giỏ đó và các CUSTOM configuration của nó không được dùng lại hoặc sửa tiếp; lần mua sau hệ thống tạo một giỏ `ACTIVE` mới khi cần.
- Checkout thất bại không đổi trạng thái giỏ và không xóa lựa chọn của khách.
- Một `CART` chỉ được checkout thành công **một lần**. Checkout phải bắt đầu bằng thao tác chuyển có điều kiện `ACTIVE → CHECKED_OUT`; chỉ request cập nhật được đúng một dòng mới được phép tạo đơn và trừ tồn.
- Việc claim giỏ, kiểm tra dữ liệu, tạo order/items/components/payment, trừ tồn và ghi movements phải nằm trong **cùng transaction**. Nếu transaction rollback thì giỏ trở lại `ACTIVE`; nếu commit thì mọi request lặp hoặc đồng thời sau đó phải thất bại an toàn, không tạo đơn và không trừ tồn lần hai.
- Không có luồng đưa giỏ `CHECKED_OUT` trở lại `ACTIVE`. Request checkout lặp có thể trả kết quả “giỏ đã được checkout”/conflict, nhưng không được mô phỏng một lần đặt hàng mới.
- Mọi thao tác giỏ/checkout lấy `customer_id` từ principal phía server. `cart_id` hoặc `customer_id` do trình duyệt gửi không chứng minh quyền sở hữu; service phải đối chiếu giỏ với khách hiện tại.

## 5. Checkout, đơn hàng và trạng thái

- Khi khách `CUSTOMER` đã đăng nhập chọn phương thức thanh toán (**COD** hoặc **VNPay**) và nhấn **Đặt hàng**, backend tự kiểm tra quyền sở hữu giỏ, SKU/cấu hình đang hợp lệ, đủ tồn kho, tính giá, chiết khấu, phí giao hàng và thông tin người nhận.
- Nếu hợp lệ, hệ thống tự động tạo `SALES_ORDER`, `ORDER_ITEM`, `ORDER_ITEM_COMPONENT`, `PAYMENT` và cập nhật kho **trong cùng một giao dịch nhất quán**. Đơn COD hiển thị đặt hàng thành công; đơn VNPay chuyển khách sang cổng VNPay Sandbox (mục 6.2).
- **Không cần Admin duyệt đơn.** Admin không online thì khách vẫn đặt hàng bình thường.
- Chuyển trạng thái đơn hợp lệ trong MVP:
  - `PLACED` → `PROCESSING` hoặc `CANCELLED`.
  - `PROCESSING` → `SHIPPING` hoặc `CANCELLED`.
  - `SHIPPING` → `DELIVERED` (Admin xác nhận đã giao; từ v1.16).
  - `DELIVERED` → `COMPLETED` (khách xác nhận đã nhận, hoặc hệ thống tự hoàn tất sau 3 ngày; từ v1.16).
  - `COMPLETED` và `CANCELLED` là trạng thái kết thúc; không chuyển ngược hoặc chuyển sang trạng thái khác.
- Khách chỉ được tự hủy đơn **của chính mình** khi trạng thái còn `PLACED` và **chưa thanh toán** (`PAYMENT.UNPAID`). Quyền sở hữu phải lấy từ principal `CUSTOMER` phía server; mã đơn hoặc `customer_id` do trình duyệt gửi không chứng minh quyền sở hữu.
- Admin được hủy đơn khi trạng thái còn `PLACED` hoặc `PROCESSING`. Khi đơn đã sang `PROCESSING`, khách không còn nút tự hủy và phải liên hệ cửa hàng nếu cần; Admin quyết định xử lý theo trạng thái thực tế.
- Hủy đơn VNPay còn tuân theo điều kiện đối soát ở mục 6.3.
- Yêu cầu hủy từ `SHIPPING`, `COMPLETED` hoặc `CANCELLED` bị từ chối và không tự hoàn tồn. Đổi/trả sau khi giao nằm ngoài phạm vi MVP.
- Chuyển sang `CANCELLED` phải là conditional update theo đúng trạng thái hiện tại và yêu cầu cập nhật đúng một dòng. Request hủy lặp/đồng thời hoặc cạnh tranh với chuyển trạng thái khác không được hoàn kho hay ghi movement lần hai.
- Đơn `COMPLETED` là giao thành công theo xác nhận vận hành. Không tự chuyển `COMPLETED` khi khách chỉ nhấn đặt hàng.
- Khách được sửa snapshot tên người nhận, số điện thoại, địa chỉ và ghi chú của đơn thuộc mình khi còn `PLACED`, với **mọi phương thức thanh toán** (phí giao hàng không phụ thuộc địa chỉ nên số tiền không đổi). Không hỗ trợ sửa sản phẩm, số lượng, giá, phí, payment hoặc các snapshot khác của đơn đã đặt.
- Chưa hỗ trợ hoàn tiền tự động, đổi trả sau giao, nhiều kiện giao hàng hoặc nhiều địa chỉ giao cho cùng một đơn trong MVP.
- `SALES_ORDER` lưu **snapshot**: người mua, người nhận, địa chỉ, phí vận chuyển, tổng tiền và trạng thái. `ORDER_ITEM` lưu snapshot SKU/tên/giá/phần trăm giảm/thành tiền; `ORDER_ITEM_COMPONENT` lưu chi tiết hộp. Đơn cũ không thay đổi khi sản phẩm hoặc chính sách giảm giá được cập nhật.
- `subtotal_amount` là tổng giá gốc của tất cả dòng trước giảm; `discount_amount` là tổng giảm thực tế của các dòng đủ điều kiện; phí giao hàng không tham gia giảm giá.
- Tổng trước làm tròn là `subtotal_amount - discount_amount + shipping_fee`. Chỉ sau bước này mới làm tròn đến 1.000 VND gần nhất theo `HALF_UP`.
- Kiểm tra tổng tiền lưu: **`total_amount = subtotal_amount - discount_amount + shipping_fee + rounding_adjustment`**; `rounding_adjustment` nằm trong khoảng `-499..500` VND. Tổng tiền, chiết khấu và làm tròn đều tính trên server; **không tin số tiền frontend gửi lên**.
- Mọi bước tạo đơn, snapshot, payment, trừ kho, ghi movement và chuyển giỏ sang `CHECKED_OUT` phải commit cùng nhau. Nếu bất kỳ bước nào thất bại thì rollback toàn bộ và không hiển thị thành công.

## 6. Thanh toán — COD và VNPay Sandbox

### 6.1. Quy tắc chung

- MVP hỗ trợ hai phương thức `PAYMENT.payment_method`: `COD` và `VNPAY`. Khách chọn tại checkout; mặc định là COD. VNPay chỉ hiển thị khi cấu hình `VNPAY_ENABLED=true` và đủ thông tin merchant.
- Khi tạo đơn, tạo **đúng một** bản ghi `PAYMENT` ở trạng thái `UNPAID`, số tiền bằng `SALES_ORDER.total_amount` đã làm tròn. Không ghi `PAID` tại checkout.
- **Phương thức thanh toán cố định từ lúc đặt hàng.** Không có thao tác đổi COD ↔ VNPay sau checkout. Khách muốn đổi phương thức thì hủy đơn (khi còn được hủy) rồi đặt lại.
- Mỗi đơn có tối đa một khoản `PAID`. Không tạo `PAYMENT` mới cho các lần thử thanh toán; các lần thử VNPay được lưu trong `VNPAY_ATTEMPT`.
- Tồn kho được trừ khi checkout thành công với **cả hai** phương thức; đơn VNPay chưa trả tiền vẫn giữ hàng cho đến khi được thanh toán hoặc bị hủy.
- Không có bước **duyệt đơn** hoặc **duyệt tiền trước khi chấp nhận đơn**.
- Không chuyển khoản thủ công, không upload biên lai. Chỉ đơn VNPay chưa thanh toán được tự hủy theo lịch, sau 15 phút từ v1.11; đơn COD không tự hủy.
- Giới hạn giữ hàng (v1.11): COD chỉ cho đơn đến 5.000.000 VND; mỗi khách tối đa 2 đơn COD chờ xử lý và 1 đơn VNPay chưa thanh toán; 3 đơn VNPay bị hủy khi chưa thanh toán trong 24 giờ thì tạm khóa đặt đơn mới (mọi phương thức).

### 6.2. COD

- Chỉ chuyển sang `PAID` và ghi `paid_at` **khi có xác nhận hàng đã giao và tiền COD thực sự được thu**.
- Xác nhận giao thành công và thu COD là một thao tác nghiệp vụ nguyên tử: chuyển đơn `SHIPPING → DELIVERED` (trước v1.16 là `SHIPPING → COMPLETED`), chuyển payment `UNPAID → PAID` và ghi `paid_at` trong cùng transaction. Nếu một bước thất bại thì không cập nhật bước còn lại.
- Gọi lặp thao tác xác nhận giao/thu COD không được tạo payment mới, ghi đè `paid_at` hoặc thực hiện lại tác động khác; lần sau phải trả về kết quả đã xử lý hoặc bị từ chối an toàn.
- Khi đơn COD bị hủy hợp lệ trước giao, `PAYMENT` giữ `UNPAID` và `paid_at` giữ null; `SALES_ORDER.CANCELLED` thể hiện đơn đã hủy. Không có trạng thái payment `CANCELLED` và không hoàn tiền vì chưa thu tiền.

### 6.3. VNPay Sandbox

**Trạng thái hiển thị cho khách** (suy ra từ dữ liệu, không thêm cột):

| Trạng thái hiển thị | Điều kiện |
|---|---|
| Chờ thanh toán | Đơn `PLACED`, payment `UNPAID` |
| Đang thanh toán | Có `VNPAY_ATTEMPT` `PENDING` chưa hết hạn |
| Đã thanh toán | Payment `PAID` |

**Thanh toán và thanh toán lại**

- Sau khi đặt đơn VNPay, hệ thống tạo một `VNPAY_ATTEMPT` (`PENDING`, hết hạn sau **15 phút**) và chuyển khách sang cổng Sandbox. Số tiền luôn lấy từ `SALES_ORDER.total_amount` trong database.
- Nếu cấu hình VNPay lỗi tại thời điểm checkout, toàn bộ checkout rollback; giỏ vẫn `ACTIVE`.
- Mỗi đơn có tối đa **một** attempt `PENDING`. Bấm thanh toán lại khi attempt cũ còn hạn thì dùng lại attempt đó; khi attempt cũ đã hết hạn thì hệ thống tự hỏi VNPay, đóng attempt cũ (nếu chưa trả) rồi tạo attempt mới.
- Khách được **thanh toán lại** đơn VNPay của mình khi đơn còn `PLACED` và payment `UNPAID`.

**Xác nhận đã trả tiền**

- Chỉ chuyển payment `UNPAID → PAID` (và attempt `PENDING → PAID`) khi nhận **IPN** hoặc kết quả **truy vấn `querydr`** hợp lệ: đúng chữ ký HMAC-SHA512, đúng `TmnCode`, đúng mã giao dịch, đúng số tiền và trạng thái giao dịch thành công.
- **Return URL chỉ hiển thị thông báo**, không bao giờ tự cập nhật `PAID`.
- IPN/kết quả lặp cho attempt đã xử lý không thay đổi dữ liệu lần hai.
- Backend phải có địa chỉ HTTPS công khai (ví dụ Cloudflare Tunnel) để VNPay gọi được IPN. Admin có nút **Kiểm tra thanh toán VNPay** để hệ thống gọi `querydr` khi IPN bị lỡ.

**Hủy đơn VNPay**

- Khách (khi `PLACED`) hoặc Admin (khi `PLACED`/`PROCESSING`) bấm hủy một lần. Nếu đơn có attempt `PENDING`, hệ thống tự hỏi VNPay (`querydr`) trước khi hủy:
  - VNPay báo **đã thanh toán** → từ chối hủy, báo đơn đã được thanh toán.
  - Mọi trường hợp còn lại (VNPay báo chưa thanh toán, trả mã lỗi như `91`/`99`, hoặc không kết nối được) → attempt `FAILED`, đơn được hủy ngay, hoàn tồn đúng một lần; payment giữ `UNPAID`. Nếu tiền vẫn về sau đó, áp dụng quy tắc tiền về muộn bên dưới.
- Đơn VNPay **đã trả tiền**: khách không tự hủy mà liên hệ cửa hàng. Admin được hủy khi `PLACED`/`PROCESSING`; payment giữ `PAID` và Admin **hoàn tiền thủ công** trên cổng merchant VNPay. MVP không gọi API hoàn tiền.
- **Tiền về muộn:** nếu khách vẫn trả trên trang VNPay cũ sau khi đơn đã hủy, IPN hợp lệ vẫn được ghi nhận: attempt và payment chuyển `PAID`, đơn giữ `CANCELLED`. Trang Admin hiển thị "Cần hoàn tiền" để Admin hoàn tiền thủ công; không mở lại đơn.

**Xử lý và hoàn tất**

- Admin chỉ chuyển đơn VNPay `PLACED → PROCESSING` khi payment đã `PAID`.
- `SHIPPING → COMPLETED` của đơn VNPay không thu COD và không đổi payment.

### 6.4. Tương lai, không triển khai hiện tại

- payOS/VietQR hoặc cổng thanh toán khác, hoàn tiền tự động qua API, nhiều lần `PAID` một phần — thêm bằng migration và phiên bản tài liệu mới khi thật sự triển khai.

## 7. Chiết khấu số lượng — hai nhóm tách biệt

Chiết khấu **tự động, áp dụng cho mọi khách hàng đã đăng nhập** (cá nhân hoặc mua theo thông tin doanh nghiệp), không yêu cầu xác minh doanh nghiệp để nhận giảm giá số lượng.

| Nhóm | Tổng số lượng trong nhóm | Mức giảm |
|---|---:|---:|
| `GIFT_BOX` | 1–4 | 0% |
| `GIFT_BOX` | 5–19 | 5% |
| `GIFT_BOX` | 20–49 | 8% |
| `GIFT_BOX` | 50–99 | 10% |
| `GIFT_BOX` | 100–199 | 12% |
| `GIFT_BOX` | 200–499 | 15% |
| `GIFT_BOX` | Từ 500 | 18% |
| `MOONCAKE` | 1–49 | 0% |
| `MOONCAKE` | 50–99 | 5% |
| `MOONCAKE` | Từ 100 | 10% |

- **Nhóm `GIFT_BOX`:** cộng **số bộ** hộp quà `GIFT_SET` đóng sẵn và hộp `CUSTOM` trong **cùng một đơn**.
- **Nhóm `MOONCAKE`:** cộng **số bánh lẻ** thuộc các SKU bánh trong cùng đơn.
- `EMPTY_BOX` mua rời không tính vào hai nhóm, không được giảm theo chính sách này.
- **Không đếm bánh nằm bên trong hộp quà** vào nhóm bánh lẻ (tránh giảm hai lần).
- Xét **bậc cao nhất đạt được của từng nhóm** rồi áp dụng phần trăm đó lên **tiền các dòng hàng thuộc chính nhóm đó**, không giảm cả đơn hàng hoặc phí vận chuyển.
- Có thể có **hai bậc giảm khác nhau trong cùng một đơn**. Ví dụ 30 PRESET + 25 CUSTOM = 55 hộp ⇒ giảm 10% trên các dòng hộp quà; 60 Lava lẻ + 45 Thập Cẩm lẻ = 105 bánh ⇒ giảm 10% trên các dòng bánh lẻ.
- `VOLUME_DISCOUNT_TIER` là nguồn cấu hình các ngưỡng; lưu tỷ lệ và số tiền giảm đã áp dụng trong snapshot của `ORDER_ITEM`. Không tự tạo thêm mã giảm giá hoặc cộng dồn khuyến mãi khác ở MVP.
- Seed hiện hành gồm đúng 8 tier active: sáu tier `GIFT_BOX` tại các ngưỡng 5/20/50/100/200/500 và hai tier `MOONCAKE` tại các ngưỡng 50/100. Không tìm thấy tier phù hợp nghĩa là 0%.
- Trong cùng một `discount_group`, các khoảng active không được giao nhau và mỗi số lượng chỉ khớp tối đa một tier. Unique index chỉ ngăn trùng `min_quantity`; Admin/Service phải từ chối khoảng chồng nhau trước khi lưu.
- Giá tiền tính bằng VND nguyên đồng, dùng `BigDecimal`/số nguyên; không dùng `float`/`double` cho tiền.
- Quy tắc làm tròn được khóa theo **từng đơn vị hàng**, làm tròn HALF_UP đến một đồng VND (giá trị `.5` làm tròn lên đối với tiền dương):
  - `unit_discount_snapshot = ROUND_HALF_UP(unit_base_price_snapshot × discount_pct_snapshot / 100)`.
  - `unit_final_price_snapshot = unit_base_price_snapshot - unit_discount_snapshot`.
  - `line_total = quantity × unit_final_price_snapshot`.
  - Giảm của một dòng = `quantity × unit_discount_snapshot`; `SALES_ORDER.discount_amount` là tổng giảm của các dòng.
- Tier và giá được xác định lại tại checkout; cập nhật tier sau khi đơn đã tạo không làm thay đổi snapshot của đơn cũ.

## 8. Phí vận chuyển — quy tắc demo

- Nếu **tổng số bộ hộp quà đủ điều kiện trong đơn dưới 50**, `shipping_fee = 30.000 VND`.
- Nếu **từ 50 bộ hộp quà trở lên**, `shipping_fee = 150.000 VND`.
- Bánh lẻ và hộp rỗng **không được cộng vào ngưỡng 50 hộp** để tính phí vận chuyển.
- Đơn hợp lệ có `0` bộ hộp quà vẫn thuộc trường hợp “dưới 50”, nên phí demo là **30.000 VND**; MVP không có miễn phí vận chuyển cho đơn chỉ gồm bánh lẻ/hộp rỗng.
- Phí là **giả định demo đã chốt**, không phải báo giá vận chuyển thực tế; chưa tính khoảng cách, trọng lượng, tỉnh/thành hay kết nối đơn vị giao hàng. Hiển thị phí trước khi xác nhận đặt và lưu vào `SALES_ORDER.shipping_fee`.
- Backend phải tính lại phí từ các dòng hợp lệ tại checkout; không tin tỉnh/thành, mức phí hoặc tổng số hộp do frontend gửi lên.
- Nếu sau này thay đổi mức phí, đơn đã tạo vẫn giữ phí tại thời điểm đặt. Có thể cấu hình bằng hệ thống ở Service/config; MVP **không yêu cầu bảng SHIPPING riêng**.

## 9. Quản lý tồn kho

- Admin quản lý tồn qua `INVENTORY_BALANCE` và `STOCK_MOVEMENT`: xem số lượng có thể bán, cảnh báo sắp hết, nhập thêm (`RECEIVE`), điều chỉnh (`ADJUST`) và xem lịch sử tăng/giảm.
- Mỗi SKU được quản lý tồn (`MOONCAKE`, `EMPTY_BOX`, `GIFT_SET`) phải có đúng một dòng `INVENTORY_BALANCE` trước khi được mở bán. Tạo/activate SKU phải kiểm tra điều kiện này; CUSTOM configuration không có tồn riêng.
- `INVENTORY_BALANCE.available_quantity` là **số lượng còn có thể bán**; trong MVP không xây dựng cơ chế đặt trước/giữ hàng với `reserved_quantity` hay `quantity_on_hold`. Vì trừ số có thể bán ngay khi đặt, giá trị này **không nhất thiết bằng số vật lý đang nằm trên kệ** khi đơn chưa giao.
- Thêm hàng vào giỏ: **không trừ kho**. **Đặt hàng thành công:** cộng gộp tất cả nhu cầu theo SKU, kiểm tra và **trừ ngay** số có thể bán, đồng thời tạo đơn và `STOCK_MOVEMENT (SALE)`.
- **Bán hộp PRESET:** trừ **duy nhất SKU GIFT_SET thành phẩm** theo số hộp đặt. Không trừ thêm các bánh và hộp nằm bên trong.
- **Bán hộp CUSTOM:** cho mỗi bộ trừ **1 SKU hộp rỗng + từng SKU bánh × `quantity_per_box`**; nhân toàn bộ với số bộ được mua.
- **Bán bánh lẻ/hộp rỗng mua rời:** trừ đúng SKU và số lượng tương ứng.
- Nếu nhiều dòng hàng dùng chung một SKU bánh/hộp, phải **cộng gộp nhu cầu rồi kiểm tra toàn bộ**, không xử lý từng dòng tách rời dẫn đến bán vượt tồn.
- Nếu thiếu bất kỳ SKU nào, **không tạo đơn thành công và không trừ bất kỳ SKU nào** (rollback toàn giao dịch).
- **Hủy hợp lệ:** khách hủy đơn của mình khi `PLACED`, hoặc Admin hủy khi `PLACED`/`PROCESSING`; cộng lại chính xác các SKU từng trừ, **chỉ một lần**, và ghi `STOCK_MOVEMENT (CANCEL_RETURN)`. Chuyển trạng thái, hoàn tồn và ghi movement phải cùng một transaction. Từ `SHIPPING` trở đi không tự hoàn kho; đổi/trả ngoài phạm vi MVP.
- **Ghi nhận đóng sẵn (`PACK`)**: khi cửa hàng chuyển bánh lẻ và hộp rỗng sang hộp PRESET, trừ các SKU thành phần và cộng SKU GIFT_SET hoàn chỉnh trong **một transaction**; ghi rõ lịch sử để không tăng tồn vô căn cứ. Không cần module sản xuất/lô hàng riêng.
- Các thay đổi tồn kho phải **không âm**, có kiểm soát giao dịch đồng thời và chống xử lý lặp. `STOCK_MOVEMENT.quantity_delta`: âm khi bán/xuất; dương khi nhập/hoàn; `PACK` có cả dòng âm và dương.
- Mỗi thao tác phải cộng gộp nhu cầu theo `variant_id` và tạo tối đa một movement cho mỗi SKU/chiều biến động. Backend cập nhật tồn bằng câu lệnh có điều kiện `available_quantity >= need`; tất cả SKU phải thành công trước khi commit.
- `operation_key` cần **duy nhất cho từng dòng biến động**; quy ước MVP:
  - SALE: `SALE:{orderId}:{variantId}`.
  - CANCEL_RETURN: `CANCEL_RETURN:{orderId}:{variantId}`.
  - PACK: `PACK:{packGroupKey}:{variantId}:{IN|OUT}`.
- `operation_group_key` bắt buộc cho PACK để gắn các dòng nhập/xuất trong cùng thao tác; có thể dùng thêm cho các movement của cùng đơn. Không dùng chung một `operation_key` cho nhiều dòng vì SQL có unique index trên cột này.
- Gọi lặp cùng khóa nghiệp vụ không được thay đổi tồn lần hai. Việc cập nhật balance và ghi movement phải ở cùng transaction để lỗi unique key làm rollback cả biến động số lượng.

## 10. Admin và dữ liệu lịch sử

- Quyền Admin thực hiện quản lý danh mục, sản phẩm, SKU, mẫu hộp, tồn kho, đơn hàng và cập nhật giao hàng/COD. `ACCOUNT.role` phân biệt `ADMIN` với `CUSTOMER`.
- Khách có quyền tự hủy đơn `PLACED` của chính mình; Admin không sở hữu một luồng hoàn kho riêng mà dùng chung nghiệp vụ hủy để bảo đảm idempotency và không hoàn tồn hai lần.
- Admin **không phải online để khách đặt hàng**. Một đơn mới tự tạo trong hệ thống và được nhân viên xử lý sau.
- Không xóa cứng sản phẩm, cấu hình, khách hàng, đơn hàng, bản ghi thanh toán, biến động kho khi chúng đã được dữ liệu lịch sử tham chiếu; sử dụng `is_active` hoặc trạng thái phù hợp.
- **Quản lý khách hàng (v1.13).** Admin xem danh sách khách (email, họ tên, số điện thoại, ngày đăng ký, trạng thái, tổng số đơn, số đơn đã hủy), tìm theo email hoặc số điện thoại, và **khóa** (`ACTIVE → DISABLED`) hoặc **mở khóa** (`DISABLED → ACTIVE`) bằng cập nhật có điều kiện. Chỉ áp dụng cho tài khoản `CUSTOMER`; không có thao tác xóa tài khoản, sửa thông tin hộ hay đặt lại mật khẩu hộ.
- **Hiệu lực của khóa:** khách không đăng nhập được; mọi phiên đang mở của khách bị hết hạn ngay và lần truy cập kế tiếp được đưa về `/login?disabled` với thông báo "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ cửa hàng để được hỗ trợ." Bước đặt hàng kiểm tra lại tài khoản còn `ACTIVE` trong cùng transaction để không tạo đơn sau thời điểm khóa. Trang đăng nhập **không** nói riêng là email đã bị khóa khi người nhập chưa đăng nhập được (tránh lộ email nào đã đăng ký); câu báo lỗi chung vẫn nhắc trường hợp tài khoản bị khóa.
- **Đơn đang mở khi khóa** (trang chi tiết khách liệt kê trước khi xác nhận):
  - `PLACED` và payment `UNPAID` (COD chưa thu hoặc VNPay chưa trả): nếu Admin để chọn **"Hủy luôn các đơn chưa thanh toán đang chờ"** (mặc định bật), hệ thống hủy qua đúng luồng hủy của Admin: hoàn tồn đúng một lần; đơn VNPay được hỏi `querydr` trước, nếu VNPay báo đã thanh toán thì **không hủy** và chuyển sang xử lý thủ công.
  - `PLACED` hoặc `PROCESSING` đã có payment `PAID`, hoặc `PROCESSING` COD: **không tự hủy**. Admin liên hệ khách qua số điện thoại trên đơn và quyết định giao hay hủy; hủy đơn đã trả tiền thì hoàn tiền thủ công như mục 6.3.
  - `SHIPPING`: tiếp tục giao, xác nhận giao và thu COD như bình thường; không hủy được.
  - `COMPLETED`, `CANCELLED`: giữ nguyên.
- **Thứ tự khi khóa:** khóa tài khoản trong một transaction trước; sau đó hủy từng đơn đủ điều kiện, **mỗi đơn một transaction**. Một đơn hủy thất bại (VNPay báo đã trả, đơn vừa bị người khác chuyển trạng thái) không làm hủy việc khóa hay các đơn khác; kết quả báo số đơn đã hủy và danh sách mã đơn cần xử lý thủ công.
- Tiền VNPay về sau khi khóa (khách đã mở cổng thanh toán trước đó) vẫn được ghi nhận theo IPN như mục 6.3; nếu đơn đã bị hủy thì hiển thị "Cần hoàn tiền".
- **Mở khóa** chỉ đưa tài khoản về `ACTIVE`; không khôi phục đơn đã hủy và không ảnh hưởng luật tạm khóa đặt đơn 24 giờ của v1.11. Khóa không xóa giỏ hàng của khách.
- Danh sách đơn và trang chi tiết đơn của Admin hiện nhãn **"Khách đã bị khóa"** cho đơn của khách đang `DISABLED`, và có liên kết sang trang chi tiết khách.
- Doanh nghiệp có thể cung cấp thông tin trong `BUSINESS` và `SALES_ORDER.business_id`; chiết khấu số lượng **không phụ thuộc** vào có `BUSINESS` hay không.
- Chưa có các module kế toán, nhà cung cấp, kho đa địa điểm, hóa đơn VAT, vận chuyển qua API, đổi trả sau giao hay sản xuất theo công đoạn.
- Flyway V10–V12 đã thêm góp ý khách hàng, video sản phẩm và theo dõi tồn theo lô (`STOCK_BATCH`, `BATCH_ALLOCATION`). Quy tắc nghiệp vụ chi tiết của lô hàng do Admin owner bổ sung ở phiên bản sau; cho đến khi được chốt, tổng tồn có thể bán vẫn lấy theo `INVENTORY_BALANCE.available_quantity`.

## 11. Ranh giới Flyway / Backend

### Flyway + SQL Server

- V1 đã tạo 19 bảng theo Physical SQL đã được rà soát, gồm PK/FK, `NOT NULL`, `CHECK`, `UNIQUE`, index và kiểu dữ liệu phù hợp SQL Server.
- Ràng buộc được một hàng bằng `CHECK`: ví dụ `quantity > 0`, giá không âm, trạng thái hợp lệ, đúng **một** trong hai FK mua hàng.
- V1 đã có hai filtered unique index để một giỏ không có hai dòng cùng đích mua: duy nhất `(cart_id, variant_id)` khi `variant_id IS NOT NULL`, và duy nhất `(cart_id, configuration_id)` khi `configuration_id IS NOT NULL`.
- Dùng Flyway quản lý migration, **không chạy tay nguyên Physical Schema rồi chạy lại migration** trên cùng database. Database trống cần được tạo trước bằng công cụ/quy trình cấu hình.
- SQL tham chiếu và Flyway V1/V2 đã được đối chiếu, chạy thử trên SQL Server 2019; V3 bổ sung catalog demo, V4 mở bán hai SKU vỏ hộp demo cho CUSTOM, V5 cập nhật dữ liệu tier `GIFT_BOX`, V6 seed ảnh catalog nội bộ, V7 cập nhật ảnh vỏ hộp CUSTOM, V8 thêm snapshot chênh lệch làm tròn COD, V9 thêm `VNPAY_ATTEMPT` và phương thức `VNPAY`, V10 thêm `FEEDBACK`, V11 thêm `PRODUCT_VIDEO`, V12 thêm `STOCK_BATCH` và `BATCH_ALLOCATION`, V13 thêm số sao và trạng thái hiển thị cho `FEEDBACK`, V14 seed thêm 4 ảnh minh họa (mặt trên, góc nghiêng, mặt cắt, cận cảnh nhân) cho 10 loại bánh, V15 thêm 3 ảnh cho mỗi loại Chà Bông Trứng Nghiền và Lạp Xưởng Gà Quay Xốt XO, V16 thêm 3 ảnh minh họa cho mỗi hộp quà Kim Phúc và Lam Phúc; các bản này chỉ là dữ liệu. V17 thêm `PASSWORD_RESET_TOKEN` cho chức năng quên mật khẩu. V18 thêm mốc thời gian và thông tin giao hàng vào `SALES_ORDER`. Flyway hiện ở version 20; schema có 25 bảng. Physical ERD/DDL tham chiếu chưa được cập nhật cho V9–V17. Không sửa checksum migration đã chia sẻ; mọi thay đổi tiếp theo phải qua migration mới.

### Spring Boot Service (triển khai ở các bước sau, không cố nhét vào `CHECK`)

- Xác minh kiểu SKU (`MOONCAKE` / `EMPTY_BOX` / `GIFT_SET`), PRESET/CUSTOM, quyền sở hữu giỏ/config và tổng bánh đúng sức chứa.
- Tạo tài khoản CUSTOMER và hồ sơ CUSTOMER trong cùng transaction; chuẩn hóa email, hash mật khẩu và xử lý xung đột unique email thành lỗi thân thiện.
- Catalog được public; mọi cart/CUSTOM/checkout/order-history/customer-cancellation operation phải yêu cầu principal CUSTOMER ACTIVE và lấy `customer_id` từ server-side authentication.
- Khi thêm lại cùng SKU/config vào giỏ, tăng `quantity` theo transaction và xử lý xung đột unique an toàn; không tạo dòng trùng.
- Tính tiền hộp tự chọn, các bậc giảm theo nhóm và phí vận chuyển hoàn toàn trên backend.
- Checkout transaction: kiểm tra và trừ tồn có điều kiện, tạo đơn/items/components/payment/movements, rollback hoàn toàn nếu thất bại.
- Checkout phải claim giỏ bằng conditional update `WHERE cart_status = 'ACTIVE'` và yêu cầu affected-row count bằng 1 trước mọi thay đổi có tác động; request không claim được phải dừng ngay.
- Các thao tác thêm/xóa/đổi số lượng và sửa CUSTOM configuration cũng phải xác minh giỏ còn `ACTIVE`; unique index chỉ chống dòng trùng, không thay thế kiểm tra trạng thái giỏ.
- Admin/Service không cho sửa PRESET đã `READY`. Chỉ được mở khóa về `DRAFT` khi chưa có tồn, lịch sử kho, giỏ hàng hay đơn tham chiếu (v1.10); ngoài trường hợp đó, thay đổi composition luôn tạo SKU/configuration mới và giữ nguyên recipe cũ.
- Kiểm tra tier active không chồng khoảng; áp dụng công thức làm tròn v1.1 nhất quán trên từng đơn vị và tổng đơn.
- Customer cancellation phải dùng POST có CSRF, xác minh đơn thuộc khách hiện tại và chỉ nhận `PLACED`; Admin cancellation nhận `PLACED`/`PROCESSING`. Cả hai gọi cùng một service/transaction hủy, không sao chép logic hoàn kho.
- Customer delivery update phải dùng POST có CSRF, lấy ownership từ principal và conditional update `WHERE order_status = 'PLACED'`; không được nhận `customer_id` từ form hoặc thay đổi các snapshot tài chính/sản phẩm.
- VNPay: chỉ ghi `PAID` từ IPN/`querydr` đã kiểm chữ ký và số tiền; đối soát attempt hết hạn trước khi hủy hoặc thanh toán lại; xử lý IPN lặp idempotent; không bao giờ tin Return URL.
- Idempotency cho hủy đơn/hoàn kho, ghi nhận COD đúng một lần, thao tác Admin PACK đồng bộ, quản lý chuyển trạng thái đơn hợp lệ.
- Tạo/activate SKU có tồn phải đồng thời bảo đảm tồn tại `INVENTORY_BALANCE`; không cho mở bán SKU thiếu balance.
- Mọi snapshot đơn hàng không được thay đổi khi dữ liệu sản phẩm/cấu hình gốc cập nhật.

## 12. Bộ tình huống kiểm thử tối thiểu

| ID | Tình huống | Kỳ vọng |
|---|---|---|
| BR-01 | Mua 1 hộp rỗng + 5 bánh lẻ | Các dòng riêng, không tự ghép hộp |
| BR-02 | Tạo hộp 4 bánh nhưng mới chọn 3 bánh | Không checkout hộp CUSTOM |
| BR-03 | Mua 10 bộ PRESET | Chỉ giảm tồn 10 SKU GIFT_SET |
| BR-04 | Mua 10 bộ CUSTOM, mỗi bộ 2 Lava + 2 Thập Cẩm | Giảm 10 vỏ hộp + 20 Lava + 20 Thập Cẩm |
| BR-05 | 30 PRESET + 25 CUSTOM | Nhóm `GIFT_BOX` = 55 ⇒ giảm 10% trên hộp quà |
| BR-06 | 60 Lava lẻ + 45 Thập Cẩm lẻ | Nhóm `MOONCAKE` = 105 ⇒ giảm 10% trên bánh lẻ |
| BR-07 | 55 hộp + 105 bánh lẻ + 5 hộp rỗng | Hộp quà giảm 10%, bánh lẻ giảm 10%; hộp rỗng không giảm, phí ship 150.000đ |
| BR-08 | Khách chưa đăng nhập bấm thêm giỏ/mua | Chuyển đến login; chưa tạo/sửa giỏ và không thực hiện POST mua hàng |
| BR-09 | Hai lượt đặt đồng thời khi SKU chỉ còn 1 | Chỉ một lượt thành công, không âm tồn |
| BR-10 | Khách hủy đơn `PLACED` của chính mình; hệ thống nhận hủy lặp | Lần đầu chuyển `CANCELLED` và hoàn tồn; lần lặp bị từ chối, không hoàn lần hai |
| BR-11 | Đơn đang `SHIPPING` bị yêu cầu hủy | Không tự hủy/hoàn tồn theo luồng MVP |
| BR-12 | Admin PACK 10 hộp PRESET | Trừ đúng thành phần, cộng đúng 10 SKU thành phẩm một lần |
| BR-13 | Giao hàng thành công và đã thu COD | Payment `PAID`, ghi `paid_at`; không trừ tồn thêm |
| BR-14 | Đổi giá SKU sau khi đặt và thử sửa configuration đã được đơn tham chiếu | Đơn cũ giữ nguyên tên, giá, thành phần, phí ship; thao tác sửa configuration bị từ chối |
| BR-15 | Thử thêm SKU hộp rỗng làm bánh bên trong CUSTOM | Từ chối vì chỉ chấp nhận SKU `MOONCAKE` |
| BR-16 | Thử tạo đơn thiếu tồn một SKU của hộp CUSTOM | Không đơn nào được tạo thành công, không trừ tồn một phần |
| BR-17 | Thêm cùng một SKU/config vào giỏ hai lần | Chỉ một `CART_ITEM`, `quantity` được tăng đúng |
| BR-18 | Giá SKU thay đổi sau khi đã thêm vào giỏ | Checkout dùng giá mới; đơn thành công snapshot giá mới, không tin giá trong giỏ/frontend |
| BR-19 | SKU bị chuyển inactive sau khi đã ở trong giỏ | Checkout từ chối toàn bộ, chỉ rõ dòng lỗi, không tự bỏ dòng và không trừ tồn |
| BR-20 | Xác nhận giao thành công/thu COD hai lần | Chỉ lần đầu chuyển `COMPLETED`/`PAID`; không tạo payment hoặc tác động lặp |
| BR-21 | Checkout thành công rồi tiếp tục mua | Giỏ cũ `CHECKED_OUT` bất biến; hệ thống tạo giỏ `ACTIVE` mới |
| BR-22 | Đơn chỉ có bánh lẻ, không có hộp quà | Phí ship demo là 30.000đ |
| BR-23 | Admin tạo hai tier active chồng khoảng trong cùng nhóm | Từ chối cấu hình tier thứ hai |
| BR-24 | Mua PRESET rồi kiểm tra snapshot và movement | Có snapshot BOX + CAKE; chỉ có SALE cho SKU GIFT_SET |
| BR-25 | Mua CUSTOM có cùng SKU bánh được chọn nhiều lần | Component được gộp theo SKU; giá và tồn nhân đúng `quantity_per_set × số bộ` |
| BR-26 | Giá 101 VND được giảm 5% | Giảm đơn vị 5 VND theo HALF_UP; giá cuối 96 VND; tổng dòng bằng số lượng × 96 |
| BR-27 | Tạo/activate SKU có quản lý tồn nhưng chưa có balance | Từ chối mở bán cho đến khi có đúng một `INVENTORY_BALANCE` |
| BR-28 | Hai request đồng thời checkout cùng một giỏ `ACTIVE` | Chỉ một request claim được giỏ và tạo đúng một đơn; request còn lại không tạo order/payment/movement và không trừ tồn |
| BR-29 | Admin sửa component của PRESET đã `READY`/đã có tồn | Từ chối; yêu cầu tạo SKU GIFT_SET và PRESET configuration mới, recipe cũ không đổi. PRESET `READY` chưa có tồn, lịch sử kho, giỏ hay đơn thì được mở khóa về `DRAFT` (v1.10) |
| BR-30 | Đăng ký bằng email mới hợp lệ | Tạo đúng một ACCOUNT CUSTOMER ACTIVE và một CUSTOMER liên kết trong cùng transaction; mật khẩu chỉ lưu dạng hash |
| BR-31 | Đăng ký lại email đã tồn tại | Từ chối thân thiện; không tạo ACCOUNT/CUSTOMER rác hoặc bản ghi một nửa |
| BR-32 | Khách đã đăng nhập thêm cùng SKU hai lần | Dùng giỏ ACTIVE của chính khách; một CART_ITEM và quantity tăng đúng |
| BR-33 | Khách A gửi cart_id của khách B | Từ chối quyền truy cập ở service; không đọc hoặc sửa dữ liệu giỏ B |
| BR-34 | CUSTOMER bị DISABLED cố dùng URL cart/checkout trực tiếp | Không xác thực/không cho mua; catalog vẫn xem công khai |
| BR-35 | Đăng ký với số điện thoại sai định dạng hoặc mật khẩu vượt giới hạn BCrypt | Hiển thị lỗi validation; không gọi service và không tạo ACCOUNT/CUSTOMER |
| BR-36 | Khách A gửi yêu cầu hủy đơn `PLACED` của khách B | Từ chối quyền truy cập; đơn và tồn kho không thay đổi |
| BR-37 | Khách yêu cầu hủy đơn của mình khi đã `PROCESSING` | Từ chối tự hủy, hướng dẫn liên hệ cửa hàng; không hoàn tồn |
| BR-38 | Admin hủy đơn `PROCESSING` hợp lệ | Chuyển `CANCELLED`, giữ payment `UNPAID` và hoàn đúng tồn đã trừ một lần |
| BR-39 | Hủy và chuyển `PLACED → PROCESSING` xảy ra đồng thời | Chỉ một conditional transition thành công; không có trạng thái trung gian hoặc hoàn tồn sai/lặp |
| BR-40 | Đặt lần lượt 4/5/19/20/49/50 hộp quà | Mức giảm `GIFT_BOX` tương ứng 0%/5%/5%/8%/8%/10% |
| BR-41 | Đặt lần lượt 99/100/199/200/499/500 hộp quà | Mức giảm `GIFT_BOX` tương ứng 10%/12%/12%/15%/15%/18% |
| BR-42 | Đặt 20 bánh lẻ nhưng không có hộp quà | Bánh lẻ chưa giảm; chính sách sáu bậc hộp quà không áp dụng nhầm cho `MOONCAKE` |
| BR-43 | Khách sửa thông tin nhận hàng của đơn `PLACED` thuộc chính mình | Cập nhật tên, điện thoại, địa chỉ, ghi chú; sản phẩm, tiền, payment và tồn kho giữ nguyên |
| BR-44 | Khách A sửa thông tin nhận hàng của đơn khách B | Từ chối; không tiết lộ và không cập nhật đơn |
| BR-45 | Đơn đồng thời chuyển `PLACED → PROCESSING` khi khách lưu địa chỉ | Chỉ một thao tác thắng; update địa chỉ thất bại nếu trạng thái không còn `PLACED` |
| BR-46 | Checkout chọn VNPay | Tạo đơn `PLACED`, payment `VNPAY`/`UNPAID`, một attempt `PENDING` 15 phút, trừ tồn một lần, chuyển sang cổng Sandbox |
| BR-47 | IPN thành công hợp lệ, sau đó IPN lặp | Lần đầu payment `PAID` + `paid_at`; lần lặp không thay đổi dữ liệu |
| BR-48 | IPN sai chữ ký, sai số tiền hoặc sai `TmnCode` | Từ chối; payment vẫn `UNPAID` |
| BR-49 | Khách quay về từ Return URL nhưng chưa có IPN | Chỉ hiển thị thông báo; payment không đổi |
| BR-50 | Khách hủy đơn VNPay có attempt mở, `querydr` báo chưa thanh toán | Attempt `FAILED`; đơn `CANCELLED` ngay, hoàn tồn một lần, payment giữ `UNPAID` |
| BR-51 | Đơn VNPay đã hủy, sau đó IPN thành công hợp lệ đến | Payment `PAID`, đơn giữ `CANCELLED`; Admin thấy "Cần hoàn tiền" |
| BR-52 | Attempt hết hạn, `querydr` xác nhận đã thanh toán; khách hủy | Ghi `PAID`; từ chối hủy |
| BR-53 | Hủy đơn VNPay khi `querydr` lỗi kết nối hoặc trả mã lỗi | Vẫn hủy ngay; attempt `FAILED`; tiền về muộn (nếu có) được ghi nhận để hoàn |
| BR-54 | Khách thanh toán lại khi attempt cũ đã hết hạn và chưa trả | Attempt cũ `FAILED`, tạo attempt mới; vẫn chỉ một payment cho đơn |
| BR-55 | Admin chuyển đơn VNPay chưa `PAID` sang `PROCESSING` | Từ chối |
| BR-56 | Admin hủy đơn VNPay đã `PAID` khi `PROCESSING` | Đơn `CANCELLED`, hoàn tồn một lần, payment giữ `PAID`; hiển thị cần hoàn tiền thủ công |
| BR-57 | Đơn COD | Không có nút thanh toán VNPay; không thể đổi sang VNPay |
| BR-58 | Hoàn tất đơn VNPay đã `PAID` | `SHIPPING → COMPLETED`; không thu COD, payment không đổi |
| BR-59 | Đơn VNPay `PLACED`/`UNPAID` quá 15 phút (từ v1.11), không còn attempt trong hạn | Lần kiểm tra kế tiếp hủy đơn, attempt `FAILED`, hoàn tồn một lần |
| BR-60 | Đơn VNPay quá 15 phút nhưng khách vừa mở attempt mới còn hạn | Chưa hủy; hạn hủy lùi tới lúc attempt hết hạn |
| BR-61 | Đến hạn hủy nhưng `querydr` xác nhận đã thanh toán | Ghi `PAID`; giữ đơn `PLACED` |
| BR-62 | Đến hạn hủy nhưng không kết nối được VNPay | Không hủy; lần kiểm tra sau thử lại |
| BR-63 | Đơn COD chưa thanh toán lâu ngày | Không bao giờ tự hủy |
| BR-64 | Giỏ có 90 bánh, kho còn 96, khách thêm 10 | Từ chối "Vượt quá số lượng sản phẩm trong kho"; giỏ giữ 90 |
| BR-65 | Dòng giỏ 500 nhưng kho còn 96 | Nút thanh toán bị khóa; mở thẳng `/checkout` bị đưa về giỏ với số thiếu; giảm số lượng vẫn được |
| BR-66 | Đơn tổng 49.343.000 VND chọn COD | Từ chối; trang thanh toán khóa COD và chọn sẵn VNPay |
| BR-67 | Khách đã có 2 đơn COD chờ xử lý đặt thêm đơn COD | Từ chối, yêu cầu chờ xử lý hoặc hủy bớt |
| BR-68 | Khách đã có 1 đơn VNPay chưa thanh toán đặt thêm đơn VNPay | Từ chối, yêu cầu thanh toán hoặc hủy đơn cũ |
| BR-69 | Khách có 3 đơn VNPay bị hủy khi chưa thanh toán trong 24 giờ | Không mở được trang thanh toán và không đặt được đơn mới, kể cả COD; tự mở lại khi qua 24 giờ |
| BR-70 | Khách sửa họ tên thành "  Trần Thị B  " và số điện thoại 0912345678 | Lưu "Trần Thị B" và 0912345678; đơn cũ giữ tên cũ; trang thanh toán đơn mới điền tên mới |
| BR-71 | Khách sửa số điện thoại thành 091234 hoặc +84912345678 | Báo lỗi ở ô số điện thoại; không lưu gì |
| BR-72 | Form sửa thông tin bị chèn thêm `customerId` của người khác | Bị bỏ qua; chỉ sửa khách đang đăng nhập |
| BR-73 | Đổi mật khẩu với mật khẩu hiện tại sai | Báo "Mật khẩu hiện tại không đúng."; mật khẩu không đổi |
| BR-74 | Mật khẩu mới trùng mật khẩu hiện tại, hoặc xác nhận không khớp | Báo lỗi ở ô tương ứng; mật khẩu không đổi |
| BR-75 | Đổi mật khẩu thành công khi đang có link quên mật khẩu chưa dùng | Đăng nhập được bằng mật khẩu mới, không bằng mật khẩu cũ; link cũ không dùng được nữa |
| BR-76 | Khách chưa đăng nhập hoặc Admin mở `/account` | Khách chưa đăng nhập về trang đăng nhập; Admin bị từ chối |
| BR-77 | Admin khóa khách đang đăng nhập | Lần bấm kế tiếp của khách về `/login?disabled` kèm thông báo khóa; không thêm giỏ hay đặt hàng được |
| BR-78 | Khách bị khóa nhập đúng email và mật khẩu ở trang đăng nhập | Không đăng nhập được; hiện câu báo lỗi chung |
| BR-79 | Khóa khách có 1 đơn COD `PLACED`, 1 đơn VNPay chưa trả, ô hủy được bật | Cả hai đơn bị hủy, hoàn tồn đúng một lần; kết quả báo "Đã hủy 2 đơn" |
| BR-80 | Khóa khách có đơn VNPay `PLACED` mà `querydr` báo đã trả | Đơn không bị hủy, ghi `PAID`; nằm trong danh sách cần xử lý thủ công; tài khoản vẫn bị khóa |
| BR-81 | Khóa khách có đơn `PROCESSING` hoặc `SHIPPING` | Đơn không bị hủy; đơn `SHIPPING` vẫn xác nhận giao và thu COD được |
| BR-82 | Khóa khách và bỏ chọn ô hủy | Tài khoản bị khóa; mọi đơn giữ nguyên |
| BR-83 | Khách bấm Đặt hàng đúng lúc Admin khóa | Nếu khóa đã commit thì không tạo đơn; nếu đơn tạo trước thì đơn xuất hiện trong danh sách đơn đang mở |
| BR-84 | Mở khóa khách | Đăng nhập lại được; đơn đã hủy vẫn `CANCELLED` |
| BR-85 | Đơn của khách bị khóa trong danh sách đơn Admin | Hiện nhãn "Khách đã bị khóa" và liên kết sang trang khách |
| BR-86 | Người dùng `CUSTOMER` mở `/admin/customers` | Bị từ chối |
| BR-87 | Admin hủy đơn COD chưa thanh toán; khách mở chi tiết đơn | Hiện "Đơn hàng đã được cửa hàng hủy. Nếu cần hỗ trợ, vui lòng liên hệ cửa hàng." |
| BR-88 | Admin hủy đơn VNPay đã thanh toán | Câu trên kèm "Cửa hàng sẽ liên hệ để hoàn tiền cho bạn." |
| BR-89 | Khách tự hủy đơn của mình | Hiện "Bạn đã hủy đơn hàng này." |
| BR-90 | Đơn VNPay tự hủy do quá 15 phút | Hiện câu chung "Đơn hàng này đã bị hủy và không còn được xử lý." |
| BR-91 | Admin bấm Nhận xử lý rồi Bàn giao vận chuyển | Trang đơn của khách hiện giờ đặt hàng, giờ bắt đầu xử lý và giờ bàn giao |
| BR-92 | Bàn giao bằng Giao Hàng Nhanh nhưng bỏ trống mã vận đơn | Từ chối, yêu cầu nhập mã vận đơn; đơn vẫn Đang xử lý |
| BR-93 | Bàn giao bằng Cửa hàng tự giao, không có mã | Chuyển Đang giao; khách thấy "Giao bởi Cửa hàng tự giao" và ngày dự kiến |
| BR-94 | Chọn ngày giao dự kiến trước hôm nay | Từ chối; đơn không đổi |
| BR-95 | Gửi yêu cầu chuyển thẳng sang SHIPPING không kèm thông tin giao hàng | Từ chối |
| BR-96 | Admin đăng nhập | Vào thẳng trang Tổng quan quản trị |
| BR-97 | Nhập kho hoặc sửa lô với ngày sản xuất sau hôm nay | Từ chối "Ngày sản xuất không được sau hôm nay." |
| BR-98 | Admin bấm "Đã giao hàng" cho đơn COD đang giao | Đơn sang Đã giao, ghi giờ giao; thanh toán COD thành Đã thanh toán trong cùng transaction |
| BR-99 | Khách bấm "Đã nhận được hàng" khi đơn Đã giao | Đơn sang Hoàn thành; ghi giờ hoàn thành và giờ khách xác nhận |
| BR-100 | Khách bấm "Đã nhận được hàng" khi đơn còn Đang giao, hoặc đơn của người khác | Bị từ chối; đơn không đổi |
| BR-101 | Đơn Đã giao quá 3 ngày, khách chưa bấm | Lần chạy job kế tiếp tự chuyển Hoàn thành; giờ khách xác nhận để trống |
| BR-102 | Khách và job cùng hoàn tất một đơn cùng lúc | Chỉ một bên thành công (cập nhật có điều kiện); đơn hoàn thành đúng một lần |
| BR-103 | Admin hoặc khách cố hủy đơn Đã giao | Bị từ chối |
| BR-104 | Dashboard có đơn Đã giao trong kỳ | Doanh thu và món bán chạy tính cả đơn Đã giao |
| BR-105 | Lưu sản phẩm bánh hoặc hộp quà không có thời hạn sử dụng | Từ chối, yêu cầu nhập 1–90 ngày |
| BR-106 | Nhập kho bánh có thời hạn 30 ngày với NSX 01/09/2026 | Lô có HSD 01/10/2026 |
| BR-107 | Nhập kho bánh không nhập ngày sản xuất | Từ chối trước khi đổi tồn |
| BR-108 | Nhập kho vỏ hộp không có ngày sản xuất | Được; lô không có HSD |
| BR-109 | Lúc 06:00 giờ Việt Nam chọn NSX là hôm nay | Được chấp nhận |
| BR-110 | Đóng hộp quà thời hạn 20 ngày từ bánh có HSD sau 40 ngày | HSD hộp = ngày đóng hộp + 20 ngày |
| BR-111 | Khách mở trang bánh có lô còn hàng | Thấy "Lô đang bán: NSX … · HSD …" giống trang sửa sản phẩm của Admin |
| BR-112 | SKU có 10 cái, trong đó 4 cái thuộc lô đã hết hạn | Khách thấy còn 6; không thêm vào giỏ quá 6 |
| BR-113 | SKU có tồn nhưng mọi lô đều hết hạn | Khách thấy "Tạm hết hàng" |
| BR-114 | Nhập kho bánh thời hạn 30 ngày với NSX 60 ngày trước | Từ chối "Lô này đã hết hạn …"; tồn không đổi |
| BR-115 | Admin bấm "Hủy hàng hết hạn" cho SKU có 4 cái hết hạn | Tồn giảm 4, lô hết hạn về 0, lịch sử kho ghi điều chỉnh -4 lý do "Hủy hàng hết hạn" |
| BR-116 | Lô vừa hết hạn trong lúc khách đang ở trang thanh toán | Bước đặt hàng báo "<tên sản phẩm>: vượt quá số lượng sản phẩm trong kho…"; không tạo đơn |

## 13. Hồ sơ xác minh V1/V2 và checklist cho migration tiếp theo

1. V1/V2 đã được đối chiếu với `Physical SQL`, `Physical ERD`, `Logical V5` và tài liệu này; xác nhận đủ **19 bảng**. Khi tạo migration tiếp theo vẫn phải đọc lại các nguồn này và không tự đổi nghiệp vụ theo suy luận.
2. Kiểm tra source hiện có: `pom.xml`, SQL Server driver, Flyway SQL Server support, cấu hình Spring Boot, Entity/JPA hiện hữu; **không tạo bảng trùng** hay xóa code tùy tiện.
3. V1/V2 đã được kiểm tra trên **database trống** với FK kép giữa `CART_ITEM`–`BOX_CONFIGURATION`, hai unique index chống dòng giỏ trùng, filtered unique indexes khác, `CHECK`, kiểu Unicode và FK ordering. Luôn phân biệt bản SQL tham chiếu với file migration chạy thật.
4. Kiểm tra trường hợp **`GIFT_SET` / `GIFT_BOX`** để không nhầm loại sản phẩm với nhóm chiết khấu; phân biệt PRESET có tồn độc lập và CUSTOM trừ thành phần.
5. Kiểm tra tính nhất quán của `STOCK_MOVEMENT.operation_key`/`operation_group_key` khi có nhiều dòng biến động cho một đơn hoặc một lần PACK.
6. Rà soát cách lưu snapshot `ORDER_ITEM_COMPONENT` và quan hệ tồn tại của `BOX_CONFIGURATION` khi giỏ đã checkout, tránh mất lịch sử.
7. Kiểm tra dữ liệu tier hiện hành: `GIFT_BOX` có sáu bậc 5/20/50/100/200/500 và `MOONCAKE` giữ hai bậc 50/100; thực hiện bằng Flyway V5, không sửa V2 và không chạy seed phá dữ liệu mỗi lần ứng dụng khởi động.
8. Không đưa payOS/VietQR, webhook, reservation, duyệt đơn, quản lý lô hay nghiệp vụ ngoài MVP vào migration đầu tiên.
9. Khi phát hiện khác biệt giữa Physical DDL và quy tắc ở đây: **liệt kê điểm mâu thuẫn và đề xuất cách xử lý, chờ xác nhận rồi mới sửa schema/migration**.
10. Kết thúc phải báo cáo: file đã tạo/sửa, lệnh test đã chạy, bảng/FK/index tạo thành công hay chưa, lỗi còn tồn tại. Không khẳng định thành công nếu chưa chạy SQL Server thật.
11. Xác nhận các quyết định v1.7 đã được thể hiện nhất quán: đăng nhập trước khi mua, đăng ký ACCOUNT + CUSTOMER nguyên tử, một dòng giỏ cho mỗi target, PRESET bất biến sau READY, snapshot BOX + CAKE cho PRESET/CUSTOM, rounding HALF_UP theo đơn vị, state machine đơn, COD atomic, phương thức thanh toán cố định từ checkout, VNPay chỉ `PAID` qua IPN/`querydr` hợp lệ và đối soát trước khi hủy, checkout một lần bằng chuyển giỏ `ACTIVE → CHECKED_OUT` có điều kiện, khách chỉ sửa thông tin nhận hàng của đơn `PLACED` thuộc mình, quyền hủy đơn phân biệt giữa Customer/Admin và sáu bậc `GIFT_BOX` độc lập với hai bậc `MOONCAKE`.

---

**Nguyên tắc cuối:** *MVP đơn giản nhưng đúng dữ liệu*: catalog công khai nhưng khách phải đăng ký/đăng nhập trước khi mua; giỏ hàng không giữ/trừ kho; mỗi giỏ checkout thành công đúng một lần; đặt hàng tự động với COD hoặc VNPay, phương thức cố định từ lúc đặt; checkout trừ tồn một lần; VNPay chỉ được coi là đã trả khi VNPay xác nhận hợp lệ, và luôn đối soát trước khi hủy; khách chỉ sửa thông tin nhận hàng hoặc tự hủy đơn `PLACED` chưa thanh toán của mình, Admin có thể hủy `PLACED`/`PROCESSING`, và mọi hủy hợp lệ hoàn đúng một lần; PRESET đã READY là bất biến và có luồng xuất kho khác CUSTOM; giá/chiết khấu/phí giao của đơn cũ không thay đổi. Hoàn tiền tự động và các cổng thanh toán khác để cho giai đoạn sau.
