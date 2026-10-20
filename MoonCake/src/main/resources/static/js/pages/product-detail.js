/**
 * MoonCake Luxury Product Detail Page Scripts
 * Minimal, accessible interactions for the image lightbox and variant selector.
 */

// 1. Lightweight Accessible Image Lightbox
function initLightbox() {
  const modal = document.getElementById('lightboxModal');
  const openBtn = document.getElementById('openLightboxBtn');
  const mainImg = document.getElementById('mainImageDisplay');
  const lightboxImg = document.getElementById('lightboxImg');
  const closeBtn = document.getElementById('closeLightboxBtn');

  if (!modal || !lightboxImg) {
    return;
  }

  function openLightbox() {
    if (mainImg) {
      lightboxImg.src = mainImg.src;
      lightboxImg.alt = mainImg.alt;
    }
    modal.classList.remove('hidden');
    modal.classList.add('is-open');
    document.body.style.overflow = 'hidden';
    if (closeBtn) {
      closeBtn.focus();
    }
  }

  function closeLightbox() {
    modal.classList.add('hidden');
    modal.classList.remove('is-open');
    document.body.style.overflow = '';
    if (openBtn) {
      openBtn.focus();
    }
  }

  if (openBtn) {
    openBtn.addEventListener('click', openLightbox);
  }
  if (mainImg) {
    mainImg.addEventListener('click', openLightbox);
  }
  if (closeBtn) {
    closeBtn.addEventListener('click', closeLightbox);
  }

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

// 2. Keep displayed data and PRESET composition in sync with the selected variant.
function initVariantSelector() {
  const select = document.getElementById('variantSelect');
  if (!select) {
    return;
  }

  const price = document.querySelector('.product-detail-price');
  const mainImage = document.getElementById('mainImageDisplay');
  const presetCards = document.querySelectorAll('.preset-recipe-card');

  function updateText(selector, value) {
    const elements = document.querySelectorAll(selector);
    for (let index = 0; index < elements.length; index++) {
      elements[index].textContent = value;
    }
  }

  function updateOptionalRows(rowSelector, valueSelector, value) {
    const rows = document.querySelectorAll(rowSelector);
    for (let index = 0; index < rows.length; index++) {
      rows[index].classList.toggle('hidden', !value);
    }
    if (value) {
      updateText(valueSelector, value);
    }
  }

  function applySelectedVariant() {
    const option = select.options[select.selectedIndex];
    if (!option) {
      return;
    }

    if (price) {
      price.textContent = option.dataset.formattedPrice || '';
    }
    if (mainImage) {
      mainImage.src = option.dataset.imageUrl || mainImage.src;
      mainImage.alt = option.dataset.imageAlt || mainImage.alt;
    }

    updateText('.selected-sku', option.dataset.sku || '');
    updateOptionalRows(
      '.variant-filling-row',
      '.selected-filling',
      option.dataset.filling || ''
    );

    let weightText = '';
    if (option.dataset.weight) {
      weightText = option.dataset.weight + 'g';
    }
    updateOptionalRows(
      '.variant-weight-row',
      '.selected-weight',
      weightText
    );

    let boxCountText = '';
    let packageText = '';
    if (option.dataset.boxCount) {
      boxCountText = option.dataset.boxCount + ' bánh';
      packageText = 'Hộp ' + option.dataset.boxCount + ' bánh';
    }
    updateOptionalRows(
      '.variant-box-count-row',
      '.selected-box-count',
      boxCountText
    );
    updateOptionalRows(
      '.variant-package-row',
      '.selected-package',
      packageText
    );

    for (let index = 0; index < presetCards.length; index++) {
      const card = presetCards[index];
      const cardVariantId = card.getAttribute('data-preset-variant-id');
      card.classList.toggle('hidden', cardVariantId !== option.value);
    }
  }

  select.addEventListener('change', applySelectedVariant);
  applySelectedVariant();
}

function initializeProductDetailPage() {
  initLightbox();
  initVariantSelector();
}

document.addEventListener('DOMContentLoaded', initializeProductDetailPage);
