/**
 * MoonCake Luxury Mid-Autumn E-Commerce
 * Main Shared Client Interactions
 */

document.addEventListener('DOMContentLoaded', function () {
  // Initialize Lucide Icons if loaded on the page
  if (typeof lucide !== 'undefined' && lucide.createIcons) {
    lucide.createIcons();
  }

  // Lightweight Category Filter for Homepage Bánh Lẻ
  const filterBtns = document.querySelectorAll('[data-filter-btn]');
  const productCards = document.querySelectorAll('[data-product-card]');

  if (filterBtns.length > 0 && productCards.length > 0) {
    filterBtns.forEach(function (btn) {
      btn.addEventListener('click', function () {
        const selectedFilter = this.getAttribute('data-filter-btn');

        // Update active filter button styling
        filterBtns.forEach(function (b) {
          b.classList.remove('bg-gold', 'text-dark-navy', 'font-semibold', 'shadow');
          b.classList.add('bg-deep-indigo', 'text-sand', 'font-medium');
        });
        this.classList.remove('bg-deep-indigo', 'text-sand', 'font-medium');
        this.classList.add('bg-gold', 'text-dark-navy', 'font-semibold', 'shadow');

        // Filter cards
        productCards.forEach(function (card) {
          const cardCat = card.getAttribute('data-product-card');
          if (selectedFilter === 'all' || cardCat === selectedFilter) {
            card.style.display = '';
          } else {
            card.style.display = 'none';
          }
        });
      });
    });
  }
});
