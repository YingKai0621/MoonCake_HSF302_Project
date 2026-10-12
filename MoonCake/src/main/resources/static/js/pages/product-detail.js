/**
 * MoonCake Luxury Product Detail Page Scripts
 * Minimal, accessible interactions for Quantity, Image Lightbox, and Add-to-Cart.
 */

// 1. Quantity Selector Logic
let currentQty = 1;

function adjustQty(amount) {
  currentQty = Math.max(1, currentQty + amount);
  const qtyElem = document.getElementById('qtyDisplay');
  if (qtyElem) {
    qtyElem.textContent = currentQty;
  }
}

// 2. Lightweight Accessible Image Lightbox
function initLightbox() {
  const modal = document.getElementById('lightboxModal');
  const openBtn = document.getElementById('openLightboxBtn');
  const mainImg = document.getElementById('mainImageDisplay');
  const lightboxImg = document.getElementById('lightboxImg');
  const closeBtn = document.getElementById('closeLightboxBtn');

  if (!modal || !lightboxImg) return;

  function openLightbox() {
    if (mainImg) {
      lightboxImg.src = mainImg.src;
      lightboxImg.alt = mainImg.alt;
    }
    modal.classList.remove('hidden');
    modal.classList.add('is-open');
    document.body.style.overflow = 'hidden';
    if (closeBtn) closeBtn.focus();
  }

  function closeLightbox() {
    modal.classList.add('hidden');
    modal.classList.remove('is-open');
    document.body.style.overflow = '';
    if (openBtn) openBtn.focus();
  }

  if (openBtn) openBtn.addEventListener('click', openLightbox);
  if (mainImg) mainImg.addEventListener('click', openLightbox);
  if (closeBtn) closeBtn.addEventListener('click', closeLightbox);

  modal.addEventListener('click', function (e) {
    if (e.target === modal) {
      closeLightbox();
    }
  });

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && !modal.classList.contains('hidden')) {
      closeLightbox();
    }
  });
}

// 3. Add to Cart Hook (Prepared for Real Cart Integration Phase)
function initAddToCart() {
  const addBtn = document.getElementById('addToCartBtn');
  if (!addBtn) return;

  addBtn.addEventListener('click', function () {
    const productId = this.getAttribute('data-product-id');
    const productName = this.getAttribute('data-product-name');
    const productPrice = this.getAttribute('data-product-price');

    // Structural dispatch ready for future cart store integration
    const payload = {
      id: productId,
      name: productName,
      price: productPrice ? parseInt(productPrice, 10) : 0,
      quantity: currentQty
    };

    // Dispatch event without fake alert/mock success notification
    window.dispatchEvent(new CustomEvent('mooncake:addToCart', { detail: payload }));
    console.info('[MoonCake Cart Hook] Add to cart action triggered:', payload);
  });
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', function () {
  initLightbox();
  initAddToCart();
});
