/**
 * MoonCake Shop Catalog - Search, filtering and sorting.
 * Supports both Gift Boxes and Individual Mooncakes categories.
 */
function initializeCatalogPage() {
  const catalogMain = document.getElementById('catalog-main');
  if (!catalogMain) {
    return;
  }

  const category = catalogMain.getAttribute('data-category');
  const productGrid = document.getElementById('product-grid');
  const visibleCountEl = document.getElementById('visible-count');
  const noProductsMsg = document.getElementById('no-products-msg');
  const sortSelect = document.getElementById('catalog-sort');
  const applyBtn = document.getElementById('apply-filters-btn');
  const resetBtn = document.getElementById('reset-filters-btn');
  const clearLink = document.getElementById('clear-all-filters-link');
  const searchInput = document.getElementById('catalog-search');

  if (!productGrid) {
    return;
  }

  const productCards = Array.from(productGrid.querySelectorAll('.product-card'));
  for (let index = 0; index < productCards.length; index++) {
    productCards[index].setAttribute('data-original-index', index);
  }

  function getSelectedPriceRange() {
    const checked = document.querySelector('input[name="price_range"]:checked');
    if (checked) {
      return checked.value;
    }
    return 'all';
  }

  function getSelectedCheckboxes(name) {
    const selector = 'input[name="' + name + '"]:checked';
    const checkedInputs = document.querySelectorAll(selector);
    const selectedValues = [];

    for (let index = 0; index < checkedInputs.length; index++) {
      selectedValues.push(checkedInputs[index].value);
    }

    return selectedValues;
  }

  function matchesPrice(price, rangeValue) {
    if (!rangeValue || rangeValue === 'all') {
      return true;
    }
    if (rangeValue === 'under-1m') {
      return price < 1000000;
    }
    if (rangeValue === '1m-2m') {
      return price >= 1000000 && price <= 2000000;
    }
    if (rangeValue === 'over-2m') {
      return price > 2000000;
    }
    if (rangeValue === 'under-130k') {
      return price < 130000;
    }
    if (rangeValue === '130k-150k') {
      return price >= 130000 && price <= 150000;
    }
    if (rangeValue === 'over-150k') {
      return price > 150000;
    }

    return true;
  }

  function matchesAnySelected(valueList, selectedValues) {
    if (selectedValues.length === 0) {
      return true;
    }

    let valuesText = valueList;
    if (!valuesText) {
      valuesText = '';
    }
    const availableValues = valuesText.split('|');

    for (let index = 0; index < selectedValues.length; index++) {
      const selectedValue = selectedValues[index];
      if (availableValues.includes(selectedValue)) {
        return true;
      }
    }

    return false;
  }

  function readCardPrice(card) {
    let priceText = card.getAttribute('data-price');
    if (!priceText) {
      priceText = '0';
    }
    return parseInt(priceText, 10);
  }

  function readCardNumber(card, attributeName) {
    let value = card.getAttribute(attributeName);
    if (!value) {
      value = '0';
    }
    return parseInt(value, 10);
  }

  function normalizeSearchText(value) {
    if (!value) {
      return '';
    }
    return value
      .toLocaleLowerCase('vi')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim();
  }

  function matchesSearch(card) {
    if (!searchInput) {
      return true;
    }

    const keyword = normalizeSearchText(searchInput.value);
    if (!keyword) {
      return true;
    }

    const searchText = card.getAttribute('data-search-text');
    return normalizeSearchText(searchText).includes(keyword);
  }

  function compareProductCards(firstCard, secondCard) {
    let sortValue = 'default';
    if (sortSelect) {
      sortValue = sortSelect.value;
    }
    const firstPrice = readCardPrice(firstCard);
    const secondPrice = readCardPrice(secondCard);
    const firstName = (firstCard.getAttribute('data-name') || '').trim();
    const secondName = (secondCard.getAttribute('data-name') || '').trim();

    if (sortValue === 'price-asc') {
      return firstPrice - secondPrice;
    }
    if (sortValue === 'price-desc') {
      return secondPrice - firstPrice;
    }
    if (sortValue === 'name-az') {
      return firstName.localeCompare(secondName, 'vi');
    }

    const firstIndex = readCardNumber(firstCard, 'data-original-index');
    const secondIndex = readCardNumber(secondCard, 'data-original-index');
    return firstIndex - secondIndex;
  }

  function filterAndSortProducts() {
    const priceRange = getSelectedPriceRange();
    const selectedBoxCounts = getSelectedCheckboxes('box_count');
    const selectedFillings = getSelectedCheckboxes('filling_filter');
    const selectedWeights = getSelectedCheckboxes('weight_filter');
    const visibleCards = [];

    for (let index = 0; index < productCards.length; index++) {
      const card = productCards[index];
      const priceMatches = matchesPrice(readCardPrice(card), priceRange);
      const searchMatches = matchesSearch(card);
      let detailsMatch = true;

      if (category === 'gift-boxes') {
        const boxCount = card.getAttribute('data-box-count');
        detailsMatch = matchesAnySelected(boxCount, selectedBoxCounts);
      } else {
        const filling = card.getAttribute('data-filling');
        const weight = card.getAttribute('data-weight');
        const fillingMatches = matchesAnySelected(filling, selectedFillings);
        const weightMatches = matchesAnySelected(weight, selectedWeights);
        detailsMatch = fillingMatches && weightMatches;
      }

      if (priceMatches && detailsMatch && searchMatches) {
        card.classList.remove('hidden');
        visibleCards.push(card);
      } else {
        card.classList.add('hidden');
      }
    }

    visibleCards.sort(compareProductCards);
    for (let index = 0; index < visibleCards.length; index++) {
      productGrid.appendChild(visibleCards[index]);
    }

    if (visibleCountEl) {
      visibleCountEl.textContent = visibleCards.length;
    }

    if (noProductsMsg) {
      if (visibleCards.length === 0) {
        noProductsMsg.classList.remove('hidden');
      } else {
        noProductsMsg.classList.add('hidden');
      }
    }
  }

  function resetFilters() {
    const defaultRadio = document.querySelector('input[name="price_range"][value="all"]');
    if (defaultRadio) {
      defaultRadio.checked = true;
    }

    const checkboxes = document.querySelectorAll('aside input[type="checkbox"]');
    for (let index = 0; index < checkboxes.length; index++) {
      checkboxes[index].checked = false;
    }

    if (sortSelect) {
      sortSelect.value = 'default';
    }
    if (searchInput) {
      searchInput.value = '';
    }

    filterAndSortProducts();
  }

  function clearAllFilters(event) {
    event.preventDefault();
    resetFilters();
  }

  if (applyBtn) {
    applyBtn.addEventListener('click', filterAndSortProducts);
  }

  if (resetBtn) {
    resetBtn.addEventListener('click', resetFilters);
  }

  if (clearLink) {
    clearLink.addEventListener('click', clearAllFilters);
  }

  if (sortSelect) {
    sortSelect.addEventListener('change', filterAndSortProducts);
  }

  if (searchInput) {
    searchInput.addEventListener('input', filterAndSortProducts);
  }

  const filterInputs = document.querySelectorAll('aside input[type="radio"], aside input[type="checkbox"]');
  for (let index = 0; index < filterInputs.length; index++) {
    filterInputs[index].addEventListener('change', filterAndSortProducts);
  }

  filterAndSortProducts();
}

document.addEventListener('DOMContentLoaded', initializeCatalogPage);
