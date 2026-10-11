/**
 * MoonCake Luxury Product Detail Page Scripts
 */

// Tab Switching Logic
function switchTab(tabKey, btnElement) {
  document.querySelectorAll('.tab-panel').forEach(panel => {
    panel.classList.add('hidden');
  });
  document.querySelectorAll('.tab-link').forEach(link => {
    link.classList.remove('text-gold', 'border-gold');
    link.classList.add('text-sand', 'border-transparent');
  });

  const activePanel = document.getElementById('tab-' + tabKey);
  if (activePanel) {
    activePanel.classList.remove('hidden');
  }
  if (btnElement) {
    btnElement.classList.add('text-gold', 'border-gold');
    btnElement.classList.remove('text-sand', 'border-transparent');
  }
}

// Gallery Thumbnail Switcher
function changeGalleryImage(srcUrl, btnElement) {
  const mainImg = document.getElementById('mainImageDisplay');
  if (mainImg) {
    mainImg.style.opacity = '0.4';
    setTimeout(() => {
      mainImg.src = srcUrl;
      mainImg.style.opacity = '1';
    }, 150);
  }

  document.querySelectorAll('.thumb-btn').forEach(btn => {
    btn.classList.remove('border-2', 'border-gold', 'shadow-[0_0_12px_rgba(209,168,90,0.3)]');
    btn.classList.add('border', 'border-gold/25', 'opacity-75');
  });

  if (btnElement) {
    btnElement.classList.remove('opacity-75', 'border-gold/25');
    btnElement.classList.add('border-2', 'border-gold', 'shadow-[0_0_12px_rgba(209,168,90,0.3)]');
  }
}

// Quantity Counter
let currentQty = 1;
function adjustQty(amount) {
  currentQty = Math.max(1, currentQty + amount);
  const qtyElem = document.getElementById('qtyDisplay');
  if (qtyElem) {
    qtyElem.innerText = currentQty;
  }
}
