# Kế hoạch tối ưu hiệu năng MoonCake

**Trạng thái:** chưa làm. Làm sau khi xong đợt chỉnh code theo luật chấm (bước B).
**Ngày ghi:** 09/11/2026

## Số đo ban đầu (máy local, 09/11/2026)

- Server trả HTML rất nhanh: trang chủ khoảng 25 ms. Database không phải chỗ chậm.
- Trang chủ tải khoảng 10 MB ảnh. Các file nặng nhất đều là PNG khoảng 2–2,5 MB:
  `boxes/box-kim-phuc-4.png`, `boxes/box-lam-phuc-6.png`,
  `business/*.png` (4 file), `branding/bannerthanks.png`, `branding/bannhr.png`,
  `homepage/lunar_mooncake_gift_set_under_moonlight.png`.
- Mọi file tĩnh (ảnh, CSS, JS) đều trả `Cache-Control: no-store` (mặc định của Spring Security),
  nên mỗi lần chuyển trang trình duyệt tải lại toàn bộ.
- Tailwind dùng bản CDN (`cdn.tailwindcss.com`): trình duyệt tự biên dịch CSS mỗi lần mở trang.
  Tailwind ghi rõ bản này không dùng cho production.
- Lucide tải `@latest` (không cố định phiên bản) chỉ để dùng 4 icon trên trang chủ.
- Material Symbols tải nguyên bộ font icon.
- Dải ảnh nhỏ 80 px trên trang sản phẩm đang tải ảnh gốc 1200 px.

## Mục tiêu (Core Web Vitals, đo trên mobile)

| Chỉ số | Mục tiêu |
|---|---|
| LCP (nội dung chính xuất hiện) | ≤ 2,5 s |
| INP (phản hồi khi bấm) | ≤ 200 ms |
| CLS (giao diện bị nhảy) | ≤ 0,1 |
| TTFB (server phản hồi) | ≤ 800 ms (đã đạt) |

## Các bước

1. **Đo trước khi sửa:** chạy Lighthouse (Chrome DevTools → Lighthouse → Mobile) cho trang chủ và một trang
   sản phẩm, lưu số liệu để so sánh.
2. **Cache và nén:**
   - Bật `spring.web.resources.chain.strategy.content.enabled=true` để các link `@{...}` tới CSS/JS có
     mã hash trong tên file, rồi cache dài hạn: `public, max-age=31536000, immutable`.
   - Cache vừa phải cho `/images/**` (URL ảnh lưu trong database không có hash).
   - Trang có dữ liệu cá nhân (giỏ hàng, đơn hàng, tài khoản) giữ `no-store`.
   - Bật `server.compression.enabled=true` (gzip).
3. **Ảnh:**
   - Chuyển các PNG lớn sang WebP vừa kích thước, mục tiêu 100–250 KB mỗi ảnh. Giữ nguyên tên đường dẫn hoặc
     đổi đồng bộ trong template, không sửa migration đã chạy.
   - Tạo thêm bản 320 px cho ảnh nhỏ và thẻ sản phẩm, dùng `srcset` / `sizes`.
   - Ảnh lớn đầu trang (ảnh LCP) dùng `fetchpriority="high"` và **không** lazy. Ảnh phía dưới dùng
     `loading="lazy"` và `decoding="async"`.
   - Bổ sung `width` / `height` cho ảnh để tránh nhảy giao diện.
   - Cần công cụ chuyển ảnh (ví dụ Pillow cho Python, cài ở thư mục tạm ngoài dự án).
4. **Icon và font:** bỏ Lucide (thay 4 icon bằng SVG inline). Material Symbols chỉ tải đúng icon dùng tới
   (tham số `icon_names` của Google Fonts).
5. **Tailwind:** build sẵn thành một file CSS tĩnh bằng Tailwind CLI, thay script CDN ở mọi trang.
6. **Đo lại** bằng Lighthouse và ghi kết quả "trước / sau" vào file này.

## Không áp dụng cho MoonCake

- SSG, ISR, hydration, code splitting: là khái niệm của Next.js / Nuxt, Thymeleaf không dùng.
- Cache toàn bộ HTML hoặc CDN cho HTML: mọi trang có menu hiển thị tài khoản và giỏ hàng, nên cache chung
  sẽ lộ dữ liệu người khác.
- Redis hoặc cache dữ liệu sản phẩm: server đang trả trong khoảng 25 ms, chưa cần.
- Kiểm tra truy vấn N+1 làm kèm đợt chuyển repository sang `JpaRepository` (bước B), không phải hạng mục riêng.
