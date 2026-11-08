document.querySelectorAll('[data-gallery-src]').forEach(button => button.addEventListener('click', () => {
 const main = document.getElementById('mainImageDisplay');
 if (main) { main.src = button.dataset.gallerySrc; main.alt = button.dataset.galleryAlt || 'Ảnh sản phẩm'; }
 document.querySelectorAll('[data-gallery-src]').forEach(b => b.setAttribute('aria-pressed', String(b === button)));
}));
