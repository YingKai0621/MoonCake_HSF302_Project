# MoonCake MVP — Business Rules

**Dự án:** HSF302 — MoonCake
**Phiên bản tài liệu:** 1.2.1 — 21/10/2026

**Trạng thái:** Quy tắc nghiệp vụ MVP đã cập nhật theo yêu cầu đăng nhập trước khi mua; schema 19 bảng hiện tại vẫn đủ và không cần migration mới

**Database:** Microsoft SQL Server 2019+
**Nguồn đối chiếu:** `MoonCake_Logical_V5 (1).drawio`, `MoonCake_Physical_ERD_SQLServer.drawio`, `MoonCake_Physical_Schema_SQLServer.sql`, `MoonCake_Physical_ERD_Design_Notes.md`.

> **Đọc trước khi triển khai:** File này mô tả **quy tắc nghiệp vụ**; Physical SQL/ERD mô tả **cấu trúc dữ liệu**. Nếu tài liệu, SQL và source code hiện tại mâu thuẫn, **báo cáo và hỏi người phụ trách**, không tự thay đổi nghiệp vụ, thêm bảng hoặc sửa migration đã áp dụng. Physical SQL đã được chuyển thành Flyway V1 và kiểm thử trên SQL Server 2019; từ thời điểm migration được chia sẻ, không sửa V1 mà tạo migration mới.

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

MVP hỗ trợ bán bánh Trung Thu trên web (Spring Boot MVC + Thymeleaf), đăng ký/đăng nhập khách hàng, giỏ hàng, checkout, đặt hàng, COD, chiết khấu theo số lượng và trang Admin quản lý sản phẩm/đơn hàng/tồn kho. Khách chưa đăng nhập được xem catalog; khách **phải đăng nhập bằng tài khoản CUSTOMER đang ACTIVE** trước khi thêm vào giỏ, tạo hộp CUSTOM hoặc đặt hàng.

**Bốn cách mua hàng:**

1. **Bánh lẻ:** mua `PRODUCT` loại `MOONCAKE` qua SKU trong `PRODUCT_VARIANT`.
2. **Hộp rỗng:** mua `PRODUCT` loại `EMPTY_BOX` qua SKU. Hộp rỗng có thông tin sức chứa tương ứng trong `BOX`.
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
- PRESET chỉ được chỉnh trong giai đoạn chuẩn bị trước khi chuyển `READY`. Khi PRESET đã `READY`, SKU GIFT_SET đã được mở bán, đã có tồn thành phẩm hoặc đã được lịch sử tham chiếu, cấu hình và các `BOX_CONFIGURATION_ITEM` của nó là **bất biến**; không chuyển ngược về `DRAFT` để sửa thành phần.
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

- Catalog, trang chi tiết sản phẩm và tài nguyên tĩnh là công khai. Xem giỏ, thêm/sửa/xóa dòng giỏ, tạo/sửa CUSTOM configuration, checkout và xem lịch sử đơn yêu cầu tài khoản `CUSTOMER` đang `ACTIVE`.
- Khách được tự đăng ký bằng email, mật khẩu, họ tên và số điện thoại. Backend luôn gán `ACCOUNT.role = CUSTOMER`; không nhận role/status/account ID từ form đăng ký.
- `ACCOUNT.email` là tên đăng nhập; MVP không có username riêng. Backend loại bỏ khoảng trắng hai đầu và chuyển email về chữ thường trước khi kiểm tra trùng, lưu hoặc đăng nhập.
- Mật khẩu đăng ký có từ 8 đến 64 ký tự và phải nhập xác nhận khớp. Đăng ký thành công chuyển khách về trang đăng nhập; không tự động đăng nhập.
- Số điện thoại đăng ký gồm đúng 10 chữ số và bắt đầu bằng `0`. MVP không nhận khoảng trắng, dấu phân cách hoặc mã quốc gia `+84` trong trường này.
- Do sử dụng BCrypt, mật khẩu đăng ký không được vượt quá 72 byte khi mã hóa UTF-8; backend phải từ chối bằng lỗi validation thay vì để quá trình hash thất bại.
- Đăng ký phải chuẩn hóa email nhất quán, từ chối email đã tồn tại, hash mật khẩu bằng `PasswordEncoder`, rồi tạo `ACCOUNT` và `CUSTOMER` trong **cùng một transaction**. Nếu một bước thất bại thì không lưu bản ghi còn lại.
- Do MVP không có xác minh email hay duyệt tài khoản, đăng ký thành công tạo `ACCOUNT.status = ACTIVE`. Password reset, social login, MFA, email verification và account approval vẫn ngoài phạm vi.
- `CUSTOMER.account_id` và `CUSTOMER.email` được backend gắn với tài khoản vừa đăng ký. Trong luồng mua MVP v1.2, mọi `CUSTOMER` được dùng làm chủ giỏ phải có `account_id`; mọi đơn thành công phải có `SALES_ORDER.customer_id`.
- Khi khách chưa đăng nhập bấm “Thêm vào giỏ”, “Mua ngay” hoặc bắt đầu hộp CUSTOM, ứng dụng đưa khách đến `/login` cùng một return URL nội bộ an toàn. Không tạo giỏ, không lưu pending cart mutation và không tự phát lại POST trước khi xác thực; sau đăng nhập, khách quay lại trang phù hợp và xác nhận thao tác mua.
- `CART` của luồng MVP ứng dụng thuộc về đúng một `CUSTOMER`: `customer_id` có giá trị và `guest_token IS NULL`. Một khách có tối đa một giỏ `ACTIVE` ở một thời điểm.
- Schema V1 vẫn cho phép owner bằng `guest_token` để giữ checksum và thiết kế 19 bảng đã áp dụng. Đây là khả năng cấu trúc chưa dùng; backend MVP v1.2 không tạo, đọc như giỏ hiện hành, gộp hoặc checkout guest cart. Không cần migration chỉ để thu hẹp hành vi ứng dụng.
- `CART_ITEM` lưu **đúng một** loại đích mua: `variant_id` (bánh lẻ, hộp rỗng, GIFT_SET) **hoặc** `configuration_id` (CUSTOM); `quantity` là số nguyên dương.
- Trong cùng một giỏ, mỗi `variant_id` hoặc `configuration_id` chỉ xuất hiện ở **một `CART_ITEM`**. Thêm lại cùng đích mua phải tăng `quantity`, không tạo dòng trùng. Backend vẫn phải cộng gộp theo SKU khi kiểm tra tồn để phòng dữ liệu cũ hoặc lỗi đồng thời.
- Thêm, xóa, đổi số lượng trong giỏ **không trừ tồn kho**. Tồn kho được kiểm tra lại tại checkout; số lượng còn bán trên trang sản phẩm chỉ để tham khảo.
- Checkout thành công phải chuyển giỏ hiện tại sang `CHECKED_OUT`. Giỏ đó và các CUSTOM configuration của nó không được dùng lại hoặc sửa tiếp; lần mua sau hệ thống tạo một giỏ `ACTIVE` mới khi cần.
- Checkout thất bại không đổi trạng thái giỏ và không xóa lựa chọn của khách.
- Một `CART` chỉ được checkout thành công **một lần**. Checkout phải bắt đầu bằng thao tác chuyển có điều kiện `ACTIVE → CHECKED_OUT`; chỉ request cập nhật được đúng một dòng mới được phép tạo đơn và trừ tồn.
- Việc claim giỏ, kiểm tra dữ liệu, tạo order/items/components/payment, trừ tồn và ghi movements phải nằm trong **cùng transaction**. Nếu transaction rollback thì giỏ trở lại `ACTIVE`; nếu commit thì mọi request lặp hoặc đồng thời sau đó phải thất bại an toàn, không tạo đơn và không trừ tồn lần hai.
- Không có luồng đưa giỏ `CHECKED_OUT` trở lại `ACTIVE`. Request checkout lặp có thể trả kết quả “giỏ đã được checkout”/conflict, nhưng không được mô phỏng một lần đặt hàng mới.
- Mọi thao tác giỏ/checkout lấy `customer_id` từ principal phía server. `cart_id` hoặc `customer_id` do trình duyệt gửi không chứng minh quyền sở hữu; service phải đối chiếu giỏ với khách hiện tại.

## 5. Checkout, đơn hàng và trạng thái

- Khi khách `CUSTOMER` đã đăng nhập nhấn **Đặt hàng (COD)**, backend tự kiểm tra quyền sở hữu giỏ, SKU/cấu hình đang hợp lệ, đủ tồn kho, tính giá, chiết khấu, phí giao hàng và thông tin người nhận.
- Nếu hợp lệ, hệ thống tự động tạo `SALES_ORDER`, `ORDER_ITEM`, `ORDER_ITEM_COMPONENT`, `PAYMENT` và cập nhật kho **trong cùng một giao dịch nhất quán**; sau đó hiển thị đặt hàng thành công.
- **Không cần Admin duyệt đơn.** Admin không online thì khách vẫn đặt hàng bình thường.
- Chuyển trạng thái đơn hợp lệ trong MVP:
  - `PLACED` → `PROCESSING` hoặc `CANCELLED`.
  - `PROCESSING` → `SHIPPING` hoặc `CANCELLED`.
  - `SHIPPING` → `COMPLETED`.
  - `COMPLETED` và `CANCELLED` là trạng thái kết thúc; không chuyển ngược hoặc chuyển sang trạng thái khác.
- Chỉ `PLACED`/`PROCESSING` được hủy theo luồng MVP. Yêu cầu hủy từ `SHIPPING` trở đi bị từ chối và không tự hoàn tồn.
- Đơn `COMPLETED` là giao thành công theo xác nhận vận hành. Không tự chuyển `COMPLETED` khi khách chỉ nhấn đặt hàng.
- Chưa hỗ trợ hoàn tiền, đổi trả sau giao, chỉnh sửa đơn đã đặt, nhiều kiện giao hàng hoặc nhiều địa chỉ giao cho cùng một đơn trong MVP.
- `SALES_ORDER` lưu **snapshot**: người mua, người nhận, địa chỉ, phí vận chuyển, tổng tiền và trạng thái. `ORDER_ITEM` lưu snapshot SKU/tên/giá/phần trăm giảm/thành tiền; `ORDER_ITEM_COMPONENT` lưu chi tiết hộp. Đơn cũ không thay đổi khi sản phẩm hoặc chính sách giảm giá được cập nhật.
- `subtotal_amount` là tổng giá gốc của tất cả dòng trước giảm; `discount_amount` là tổng giảm thực tế của các dòng đủ điều kiện; phí giao hàng không tham gia giảm giá.
- Kiểm tra tổng tiền: **`total_amount = subtotal_amount - discount_amount + shipping_fee`**. Tổng tiền và chiết khấu tính trên server; **không tin số tiền frontend gửi lên**.
- Mọi bước tạo đơn, snapshot, payment, trừ kho, ghi movement và chuyển giỏ sang `CHECKED_OUT` phải commit cùng nhau. Nếu bất kỳ bước nào thất bại thì rollback toàn bộ và không hiển thị thành công.

## 6. Thanh toán — MVP chỉ COD

- Chỉ cho phép `PAYMENT.payment_method = COD` ở MVP; khi tạo đơn, tạo bản ghi `PAYMENT` ở trạng thái `UNPAID`, số tiền phải thu bằng tổng tiền đơn.
- Chỉ chuyển sang `PAID` và ghi `paid_at` **khi có xác nhận hàng đã giao và tiền COD thực sự được thu**. Không ghi `PAID` tại checkout.
- Xác nhận giao thành công và thu COD là một thao tác nghiệp vụ nguyên tử: chuyển đơn `SHIPPING → COMPLETED`, chuyển payment `UNPAID → PAID` và ghi `paid_at` trong cùng transaction. Nếu một bước thất bại thì không cập nhật bước còn lại.
- Gọi lặp thao tác xác nhận giao/thu COD không được tạo payment mới, ghi đè `paid_at` hoặc thực hiện lại tác động khác; lần sau phải trả về kết quả đã xử lý hoặc bị từ chối an toàn.
- Không có bước **duyệt đơn** hoặc **duyệt tiền trước khi chấp nhận đơn**.
- Không chuyển khoản thủ công, không upload biên lai, không giữ hàng chờ chuyển khoản, không timeout 24 giờ và không tích hợp gateway trong MVP.
- Thiết kế quan hệ cho phép `SALES_ORDER` → nhiều `PAYMENT` về sau (các lần thử thanh toán), nhưng MVP thông thường có **một bản ghi COD/đơn**; cấm ghi nhận nhiều khoản `PAID` cho cùng một đơn.
- **Tương lai, không triển khai hiện tại:** payOS/VietQR, QR code, thông tin giao dịch nhà cung cấp, webhook, kiểm tra chữ ký, các trạng thái giao dịch online — thêm bằng migration mới khi thật sự triển khai.

## 7. Chiết khấu số lượng — hai nhóm tách biệt

Chiết khấu **tự động, áp dụng cho mọi khách hàng đã đăng nhập** (cá nhân hoặc mua theo thông tin doanh nghiệp), không yêu cầu xác minh doanh nghiệp để nhận giảm giá số lượng.

| Tổng số lượng trong **mỗi nhóm** | `GIFT_BOX` | `MOONCAKE` |
|---|---:|---:|
| 1–49 | 0% | 0% |
| 50–99 | 5% | 5% |
| Từ 100 | 10% | 10% |

- **Nhóm `GIFT_BOX`:** cộng **số bộ** hộp quà `GIFT_SET` đóng sẵn và hộp `CUSTOM` trong **cùng một đơn**.
- **Nhóm `MOONCAKE`:** cộng **số bánh lẻ** thuộc các SKU bánh trong cùng đơn.
- `EMPTY_BOX` mua rời không tính vào hai nhóm, không được giảm theo chính sách này.
- **Không đếm bánh nằm bên trong hộp quà** vào nhóm bánh lẻ (tránh giảm hai lần).
- Xét **bậc cao nhất đạt được của từng nhóm** rồi áp dụng phần trăm đó lên **tiền các dòng hàng thuộc chính nhóm đó**, không giảm cả đơn hàng hoặc phí vận chuyển.
- Có thể có **hai bậc giảm khác nhau trong cùng một đơn**. Ví dụ 30 PRESET + 25 CUSTOM = 55 hộp ⇒ giảm 5% trên các dòng hộp quà; 60 Lava lẻ + 45 Thập Cẩm lẻ = 105 bánh ⇒ giảm 10% trên các dòng bánh lẻ.
- `VOLUME_DISCOUNT_TIER` là nguồn cấu hình các ngưỡng; lưu tỷ lệ và số tiền giảm đã áp dụng trong snapshot của `ORDER_ITEM`. Không tự tạo thêm mã giảm giá hoặc cộng dồn khuyến mãi khác ở MVP.
- Seed MVP gồm đúng 4 tier active: `GIFT_BOX` 50–99 = 5%, `GIFT_BOX` từ 100 = 10%, `MOONCAKE` 50–99 = 5%, `MOONCAKE` từ 100 = 10%. Dưới 50 không cần tạo tier 0%; không tìm thấy tier phù hợp nghĩa là 0%.
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
- **Hủy hợp lệ trước `SHIPPING`:** cộng lại chính xác các SKU từng trừ, **chỉ một lần**; ghi `STOCK_MOVEMENT (CANCEL_RETURN)`. Sau `SHIPPING` không tự hoàn kho; đổi/trả ngoài phạm vi MVP.
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
- Admin **không phải online để khách đặt hàng**. Một đơn mới tự tạo trong hệ thống và được nhân viên xử lý sau.
- Không xóa cứng sản phẩm, cấu hình, khách hàng, đơn hàng, bản ghi thanh toán, biến động kho khi chúng đã được dữ liệu lịch sử tham chiếu; sử dụng `is_active` hoặc trạng thái phù hợp.
- Doanh nghiệp có thể cung cấp thông tin trong `BUSINESS` và `SALES_ORDER.business_id`; chiết khấu số lượng **không phụ thuộc** vào có `BUSINESS` hay không.
- Chưa có các module kế toán, nhà cung cấp, kho đa địa điểm, lô sản xuất, hóa đơn VAT, vận chuyển qua API, đổi trả sau giao hay sản xuất theo công đoạn.

## 11. Ranh giới Flyway / Backend

### Flyway + SQL Server

- V1 đã tạo 19 bảng theo Physical SQL đã được rà soát, gồm PK/FK, `NOT NULL`, `CHECK`, `UNIQUE`, index và kiểu dữ liệu phù hợp SQL Server.
- Ràng buộc được một hàng bằng `CHECK`: ví dụ `quantity > 0`, giá không âm, trạng thái hợp lệ, đúng **một** trong hai FK mua hàng.
- V1 đã có hai filtered unique index để một giỏ không có hai dòng cùng đích mua: duy nhất `(cart_id, variant_id)` khi `variant_id IS NOT NULL`, và duy nhất `(cart_id, configuration_id)` khi `configuration_id IS NOT NULL`.
- Dùng Flyway quản lý migration, **không chạy tay nguyên Physical Schema rồi chạy lại migration** trên cùng database. Database trống cần được tạo trước bằng công cụ/quy trình cấu hình.
- SQL tham chiếu và Flyway V1/V2 đã được đối chiếu, chạy thử trên SQL Server 2019 và xác nhận schema ở version 2; V3 bổ sung catalog demo và V4 mở bán hai SKU vỏ hộp demo cho CUSTOM, đều không đổi schema. Không sửa checksum migration đã chia sẻ; mọi thay đổi tiếp theo phải qua migration mới.

### Spring Boot Service (triển khai ở các bước sau, không cố nhét vào `CHECK`)

- Xác minh kiểu SKU (`MOONCAKE` / `EMPTY_BOX` / `GIFT_SET`), PRESET/CUSTOM, quyền sở hữu giỏ/config và tổng bánh đúng sức chứa.
- Tạo tài khoản CUSTOMER và hồ sơ CUSTOMER trong cùng transaction; chuẩn hóa email, hash mật khẩu và xử lý xung đột unique email thành lỗi thân thiện.
- Catalog được public; mọi cart/CUSTOM/checkout/order-history operation phải yêu cầu principal CUSTOMER ACTIVE và lấy `customer_id` từ server-side authentication.
- Khi thêm lại cùng SKU/config vào giỏ, tăng `quantity` theo transaction và xử lý xung đột unique an toàn; không tạo dòng trùng.
- Tính tiền hộp tự chọn, các bậc giảm theo nhóm và phí vận chuyển hoàn toàn trên backend.
- Checkout transaction: kiểm tra và trừ tồn có điều kiện, tạo đơn/items/components/payment/movements, rollback hoàn toàn nếu thất bại.
- Checkout phải claim giỏ bằng conditional update `WHERE cart_status = 'ACTIVE'` và yêu cầu affected-row count bằng 1 trước mọi thay đổi có tác động; request không claim được phải dừng ngay.
- Các thao tác thêm/xóa/đổi số lượng và sửa CUSTOM configuration cũng phải xác minh giỏ còn `ACTIVE`; unique index chỉ chống dòng trùng, không thay thế kiểm tra trạng thái giỏ.
- Admin/Service không cho sửa PRESET đã `READY`; thay đổi composition luôn tạo SKU/configuration mới và giữ nguyên recipe cũ.
- Kiểm tra tier active không chồng khoảng; áp dụng công thức làm tròn v1.1 nhất quán trên từng đơn vị và tổng đơn.
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
| BR-05 | 30 PRESET + 25 CUSTOM | Nhóm `GIFT_BOX` = 55 ⇒ giảm 5% trên hộp quà |
| BR-06 | 60 Lava lẻ + 45 Thập Cẩm lẻ | Nhóm `MOONCAKE` = 105 ⇒ giảm 10% trên bánh lẻ |
| BR-07 | 55 hộp + 105 bánh lẻ + 5 hộp rỗng | Hai mức giảm riêng; hộp rỗng không giảm, phí ship 150.000đ |
| BR-08 | Khách chưa đăng nhập bấm thêm giỏ/mua | Chuyển đến login; chưa tạo/sửa giỏ và không thực hiện POST mua hàng |
| BR-09 | Hai lượt đặt đồng thời khi SKU chỉ còn 1 | Chỉ một lượt thành công, không âm tồn |
| BR-10 | Đơn `PLACED` bị hủy; hệ thống nhận hủy lặp | Hoàn tồn đúng một lần |
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
| BR-29 | Admin sửa component của PRESET đã `READY`/đã có tồn | Từ chối; yêu cầu tạo SKU GIFT_SET và PRESET configuration mới, recipe cũ không đổi |
| BR-30 | Đăng ký bằng email mới hợp lệ | Tạo đúng một ACCOUNT CUSTOMER ACTIVE và một CUSTOMER liên kết trong cùng transaction; mật khẩu chỉ lưu dạng hash |
| BR-31 | Đăng ký lại email đã tồn tại | Từ chối thân thiện; không tạo ACCOUNT/CUSTOMER rác hoặc bản ghi một nửa |
| BR-32 | Khách đã đăng nhập thêm cùng SKU hai lần | Dùng giỏ ACTIVE của chính khách; một CART_ITEM và quantity tăng đúng |
| BR-33 | Khách A gửi cart_id của khách B | Từ chối quyền truy cập ở service; không đọc hoặc sửa dữ liệu giỏ B |
| BR-34 | CUSTOMER bị DISABLED cố dùng URL cart/checkout trực tiếp | Không xác thực/không cho mua; catalog vẫn xem công khai |
| BR-35 | Đăng ký với số điện thoại sai định dạng hoặc mật khẩu vượt giới hạn BCrypt | Hiển thị lỗi validation; không gọi service và không tạo ACCOUNT/CUSTOMER |

## 13. Hồ sơ xác minh V1/V2 và checklist cho migration tiếp theo

1. V1/V2 đã được đối chiếu với `Physical SQL`, `Physical ERD`, `Logical V5` và tài liệu này; xác nhận đủ **19 bảng**. Khi tạo migration tiếp theo vẫn phải đọc lại các nguồn này và không tự đổi nghiệp vụ theo suy luận.
2. Kiểm tra source hiện có: `pom.xml`, SQL Server driver, Flyway SQL Server support, cấu hình Spring Boot, Entity/JPA hiện hữu; **không tạo bảng trùng** hay xóa code tùy tiện.
3. V1/V2 đã được kiểm tra trên **database trống** với FK kép giữa `CART_ITEM`–`BOX_CONFIGURATION`, hai unique index chống dòng giỏ trùng, filtered unique indexes khác, `CHECK`, kiểu Unicode và FK ordering. Luôn phân biệt bản SQL tham chiếu với file migration chạy thật.
4. Kiểm tra trường hợp **`GIFT_SET` / `GIFT_BOX`** để không nhầm loại sản phẩm với nhóm chiết khấu; phân biệt PRESET có tồn độc lập và CUSTOM trừ thành phần.
5. Kiểm tra tính nhất quán của `STOCK_MOVEMENT.operation_key`/`operation_group_key` khi có nhiều dòng biến động cho một đơn hoặc một lần PACK.
6. Rà soát cách lưu snapshot `ORDER_ITEM_COMPONENT` và quan hệ tồn tại của `BOX_CONFIGURATION` khi giỏ đã checkout, tránh mất lịch sử.
7. Kiểm tra dữ liệu khởi tạo bậc chiết khấu 50/100 (5%/10%) cho **cả hai nhóm**: thực hiện bằng seed được quản lý rõ ràng, không chạy seed phá dữ liệu mỗi lần ứng dụng khởi động.
8. Không đưa payOS/VietQR, webhook, reservation, duyệt đơn, quản lý lô hay nghiệp vụ ngoài MVP vào migration đầu tiên.
9. Khi phát hiện khác biệt giữa Physical DDL và quy tắc ở đây: **liệt kê điểm mâu thuẫn và đề xuất cách xử lý, chờ xác nhận rồi mới sửa schema/migration**.
10. Kết thúc phải báo cáo: file đã tạo/sửa, lệnh test đã chạy, bảng/FK/index tạo thành công hay chưa, lỗi còn tồn tại. Không khẳng định thành công nếu chưa chạy SQL Server thật.
11. Xác nhận các quyết định v1.2 đã được thể hiện nhất quán: đăng nhập trước khi mua, đăng ký ACCOUNT + CUSTOMER nguyên tử, một dòng giỏ cho mỗi target, PRESET bất biến sau READY, snapshot BOX + CAKE cho PRESET/CUSTOM, rounding HALF_UP theo đơn vị, state machine đơn, COD atomic và checkout một lần bằng chuyển giỏ `ACTIVE → CHECKED_OUT` có điều kiện.

---

**Nguyên tắc cuối:** *MVP đơn giản nhưng đúng dữ liệu*: catalog công khai nhưng khách phải đăng ký/đăng nhập trước khi mua; giỏ hàng không giữ/trừ kho; mỗi giỏ checkout thành công đúng một lần; đặt hàng tự động COD; checkout trừ tồn một lần; hủy hợp lệ hoàn đúng một lần; PRESET đã READY là bất biến và có luồng xuất kho khác CUSTOM; giá/chiết khấu/phí giao của đơn cũ không thay đổi. Các chức năng online payment được để cho giai đoạn sau.
