/**
 * MoonCake Luxury Cart Page Logic
 */

// Initial product state
const products = {
  1: { name: "Hộp Dạ Nguyệt Đoàn Viên", price: 880000, qty: 1 },
  2: { name: "Bánh Lava Trứng Chảy Kim Sa", price: 135000, qty: 1 }
};

function formatVND(amount) {
  return new Intl.NumberFormat('vi-VN').format(amount) + 'đ';
}

function calculateCart() {
  let subtotal = 0;
  let totalQty = 0;
  let distinctCount = 0;

  for (const id in products) {
    if (products[id]) {
      distinctCount++;
      totalQty += products[id].qty;
      subtotal += products[id].price * products[id].qty;
    }
  }

  // Update Header Cart Badge
  const badge = document.getElementById('headerCartBadge');
  if (badge) badge.innerText = totalQty;

  // Check if empty
  if (distinctCount === 0) {
    const activeView = document.getElementById('activeCartView');
    const emptyView = document.getElementById('emptyCartView');
    if (activeView) activeView.classList.add('hidden');
    if (emptyView) emptyView.classList.remove('hidden');
    return;
  }

  // Update Headings and Order Summary
  const countText = document.getElementById('productCountText');
  if (countText) countText.innerText = distinctCount + ' sản phẩm';

  const summaryItemCount = document.getElementById('summaryItemCount');
  if (summaryItemCount) summaryItemCount.innerText = 'Tạm tính (' + distinctCount + ' sản phẩm)';

  const summarySubtotal = document.getElementById('summarySubtotal');
  if (summarySubtotal) summarySubtotal.innerText = formatVND(subtotal);

  const summaryGrandTotal = document.getElementById('summaryGrandTotal');
  if (summaryGrandTotal) summaryGrandTotal.innerText = formatVND(subtotal);
}

function updateItemQty(id, delta) {
  if (!products[id]) return;
  const newQty = products[id].qty + delta;
  if (newQty < 1) return; // Minimum 1

  products[id].qty = newQty;
  const qtyElem = document.getElementById('qty-val-' + id);
  if (qtyElem) qtyElem.innerText = newQty;

  const lineTotal = document.getElementById('line-total-' + id);
  if (lineTotal) lineTotal.innerText = formatVND(products[id].price * newQty);

  calculateCart();
}

function removeItem(id) {
  if (!products[id]) return;
  const row = document.getElementById('item-row-' + id);
  if (row) {
    row.style.opacity = '0';
    setTimeout(() => {
      row.remove();
      delete products[id];
      calculateCart();
    }, 200);
  }
}
