/**
 * MoonCake Shop Catalog - Dynamic Filtering & Sorting
 * Supports both Gift Boxes and Individual Mooncakes categories.
 */
document.addEventListener('DOMContentLoaded', function () {
  const catalogMain = document.getElementById('catalog-main');
  if (!catalogMain) return;

  const category = catalogMain.getAttribute('data-category'); // 'gift-boxes' or 'mooncakes'
  const productGrid = document.getElementById('product-grid');
  const visibleCountEl = document.getElementById('visible-count');
  const noProductsMsg = document.getElementById('no-products-msg');
  const sortSelect = document.getElementById('catalog-sort');
  const applyBtn = document.getElementById('apply-filters-btn');
  const resetBtn = document.getElementById('reset-filters-btn');
  const clearLink = document.getElementById('clear-all-filters-link');

  if (!productGrid) return;

  const productCards = Array.from(productGrid.querySelectorAll('.product-card'));
  
  // Store original index for default sorting
  productCards.forEach((card, idx) => {
    card.setAttribute('data-original-index', idx);
  });

  function getSelectedPriceRange() {
    const checked = document.querySelector('input[name="price_range"]:checked');
    return checked ? checked.value : 'all';
  }

  function getSelectedCheckboxes(name) {
    const checked = document.querySelectorAll(`input[name="${name}"]:checked`);
    return Array.from(checked).map(cb => cb.value);
  }

  function matchesPrice(price, rangeValue) {
    if (!rangeValue || rangeValue === 'all') return true;
    if (rangeValue === 'under-1m') return price < 1000000;
    if (rangeValue === '1m-2m') return price >= 1000000 && price <= 2000000;
    if (rangeValue === 'over-2m') return price > 2000000;

    if (rangeValue === 'under-130k') return price < 130000;
    if (rangeValue === '130k-150k') return price >= 130000 && price <= 150000;
    if (rangeValue === 'over-150k') return price > 150000;

    return true;
  }

  function filterAndSortProducts() {
    const priceRange = getSelectedPriceRange();
    let visibleCards = [];

    if (category === 'gift-boxes') {
      const selectedBoxCounts = getSelectedCheckboxes('box_count');

      productCards.forEach(card => {
        const price = parseInt(card.getAttribute('data-price') || '0', 10);
        const boxCount = card.getAttribute('data-box-count');

        const priceOk = matchesPrice(price, priceRange);
        const boxOk = selectedBoxCounts.length === 0 || selectedBoxCounts.includes(boxCount);

        if (priceOk && boxOk) {
          card.classList.remove('hidden');
          visibleCards.push(card);
        } else {
          card.classList.add('hidden');
        }
      });
    } else {
      // mooncakes
      const selectedFillings = getSelectedCheckboxes('filling_filter');
      const selectedWeights = getSelectedCheckboxes('weight_filter');

      productCards.forEach(card => {
        const price = parseInt(card.getAttribute('data-price') || '0', 10);
        const filling = card.getAttribute('data-filling');
        const weight = card.getAttribute('data-weight');

        const priceOk = matchesPrice(price, priceRange);
        const fillingOk = selectedFillings.length === 0 || selectedFillings.includes(filling);
        const weightOk = selectedWeights.length === 0 || selectedWeights.includes(weight);

        if (priceOk && fillingOk && weightOk) {
          card.classList.remove('hidden');
          visibleCards.push(card);
        } else {
          card.classList.add('hidden');
        }
      });
    }

    // Sort visible cards
    const sortVal = sortSelect ? sortSelect.value : 'default';
    visibleCards.sort((a, b) => {
      const priceA = parseInt(a.getAttribute('data-price') || '0', 10);
      const priceB = parseInt(b.getAttribute('data-price') || '0', 10);
      const nameA = (a.getAttribute('data-name') || '').trim();
      const nameB = (b.getAttribute('data-name') || '').trim();
      const origA = parseInt(a.getAttribute('data-original-index') || '0', 10);
      const origB = parseInt(b.getAttribute('data-original-index') || '0', 10);

      if (sortVal === 'price-asc') {
        return priceA - priceB;
      } else if (sortVal === 'price-desc') {
        return priceB - priceA;
      } else if (sortVal === 'name-az') {
        return nameA.localeCompare(nameB, 'vi');
      } else {
        return origA - origB;
      }
    });

    // Re-append sorted cards in DOM
    visibleCards.forEach(card => {
      productGrid.appendChild(card);
    });

    // Update count display
    if (visibleCountEl) {
      visibleCountEl.textContent = visibleCards.length;
    }

    // Empty state handling
    if (noProductsMsg) {
      if (visibleCards.length === 0) {
        noProductsMsg.classList.remove('hidden');
      } else {
        noProductsMsg.classList.add('hidden');
      }
    }
  }

  function resetFilters() {
    // Reset price radio to 'all'
    const defaultRadio = document.querySelector('input[name="price_range"][value="all"]');
    if (defaultRadio) {
      defaultRadio.checked = true;
    }

    // Uncheck all filter checkboxes
    const checkboxes = document.querySelectorAll('aside input[type="checkbox"]');
    checkboxes.forEach(cb => {
      cb.checked = false;
    });

    // Reset sort
    if (sortSelect) {
      sortSelect.value = 'default';
    }

    // Re-filter
    filterAndSortProducts();
  }

  // Event Listeners
  if (applyBtn) {
    applyBtn.addEventListener('click', filterAndSortProducts);
  }

  if (resetBtn) {
    resetBtn.addEventListener('click', resetFilters);
  }

  if (clearLink) {
    clearLink.addEventListener('click', function (e) {
      e.preventDefault();
      resetFilters();
    });
  }

  if (sortSelect) {
    sortSelect.addEventListener('change', filterAndSortProducts);
  }

  // Trigger dynamic filtering immediately on radio / checkbox change
  const filterInputs = document.querySelectorAll('aside input[type="radio"], aside input[type="checkbox"]');
  filterInputs.forEach(input => {
    input.addEventListener('change', filterAndSortProducts);
  });

  // Initial pass
  filterAndSortProducts();
});
