document.addEventListener('DOMContentLoaded', initializeCustomBoxBuilder);

function initializeCustomBoxBuilder() {
  const form = document.querySelector('[data-custom-box-form]');
  if (!form) {
    return;
  }

  const boxInputs = form.querySelectorAll('input[name="boxId"]');

  for (let index = 0; index < boxInputs.length; index += 1) {
    boxInputs[index].addEventListener('change', function () {
      resetBoxQuantity(form);
      updateCustomBoxSummary(form);
    });
  }

  const boxQuantityInput = form.querySelector('[data-box-quantity]');
  if (boxQuantityInput) {
    boxQuantityInput.addEventListener('input', function () {
      updateCustomBoxSummary(form);
    });
    const boxQuantityStepper = boxQuantityInput.closest('[data-quantity-stepper]');
    const boxQuantityButtons = boxQuantityStepper.querySelectorAll('[data-quantity-action]');
    for (let index = 0; index < boxQuantityButtons.length; index += 1) {
      boxQuantityButtons[index].addEventListener('click', function () {
        updateCustomBoxSummary(form);
      });
    }
  }

  const stepperButtons = form.querySelectorAll('[data-custom-cake-stepper] button');
  for (let index = 0; index < stepperButtons.length; index += 1) {
    stepperButtons[index].addEventListener('click', function () {
      updateCustomBoxSummary(form);
    });
  }

  updateCustomBoxSummary(form);
}

function updateCustomBoxSummary(form) {
  const selectedBox = form.querySelector('input[name="boxId"]:checked');
  const selectedCountElement = form.querySelector('[data-selected-count]');
  const requiredCountElement = form.querySelector('[data-required-count]');
  const capacitySummary = form.querySelector('[data-capacity-summary]');
  const boxPriceSummary = form.querySelector('[data-box-price-summary]');
  const cakePriceSummary = form.querySelector('[data-cake-price-summary]');
  const priceSummary = form.querySelector('[data-unit-price-summary]');
  const totalSummary = form.querySelector('[data-total-price-summary]');
  const boxQuantityInput = form.querySelector('[data-box-quantity]');
  const submitButton = form.querySelector('[data-custom-submit]');
  const capacityWarning = form.querySelector('[data-capacity-warning]');

  let capacity = 0;
  let boxPrice = 0;
  if (selectedBox) {
    capacity = Number(selectedBox.getAttribute('data-box-capacity'));
    boxPrice = Number(selectedBox.getAttribute('data-box-price'));
  }

  const cakeInputs = form.querySelectorAll('[data-cake-quantity]');
  let selectedCount = 0;
  let cakePrice = 0;
  for (let index = 0; index < cakeInputs.length; index += 1) {
    const input = cakeInputs[index];
    const quantity = readNonNegativeNumber(input.value);
    selectedCount += quantity;

    const cakeRow = input.closest('[data-cake-price]');
    if (cakeRow) {
      const pricePerCake = Number(cakeRow.getAttribute('data-cake-price'));
      cakePrice += pricePerCake * quantity;
    }
  }

  selectedCountElement.textContent = String(selectedCount);
  requiredCountElement.textContent = String(capacity);
  capacitySummary.textContent = capacity + ' bánh';
  const unitPrice = boxPrice + cakePrice;
  let boxQuantity = 1;
  if (boxQuantityInput) {
    boxQuantity = readNonNegativeNumber(boxQuantityInput.value);
  }
  boxPriceSummary.textContent = formatVnd(boxPrice);
  cakePriceSummary.textContent = formatVnd(cakePrice);
  priceSummary.textContent = formatVnd(unitPrice);
  totalSummary.textContent = formatVnd(unitPrice * boxQuantity);
  submitButton.disabled = capacity <= 0 || selectedCount !== capacity;
  updateCustomCakeButtons(form, selectedCount, capacity);
  updateCapacityWarning(
    capacityWarning,
    selectedCountElement,
    selectedCount,
    capacity
  );
}

function resetBoxQuantity(form) {
  const boxQuantityInput = form.querySelector('[data-box-quantity]');
  if (!boxQuantityInput) {
    return;
  }
  boxQuantityInput.value = '1';
  boxQuantityInput.dispatchEvent(new Event('input', { bubbles: true }));
}

function updateCustomCakeButtons(form, selectedCount, capacity) {
  const cakeSteppers = form.querySelectorAll('[data-custom-cake-stepper]');
  const capacityIsFull = capacity > 0 && selectedCount >= capacity;

  for (let index = 0; index < cakeSteppers.length; index += 1) {
    const stepper = cakeSteppers[index];
    const input = stepper.querySelector('[data-cake-quantity]');
    const decreaseButton = stepper.querySelector('[data-quantity-action="decrease"]');
    const increaseButton = stepper.querySelector('[data-quantity-action="increase"]');

    if (decreaseButton && input) {
      decreaseButton.disabled = readNonNegativeNumber(input.value) <= 0;
    }
    if (increaseButton) {
      increaseButton.disabled = capacity <= 0 || capacityIsFull;
    }
  }
}

function updateCapacityWarning(
  capacityWarning,
  selectedCountElement,
  selectedCount,
  capacity
) {
  if (!capacityWarning) {
    return;
  }

  if (capacity > 0 && selectedCount > capacity) {
    const excess = selectedCount - capacity;
    capacityWarning.textContent = 'Đang dư ' + excess + ' bánh, vui lòng giảm số lượng.';
    capacityWarning.classList.remove('hidden');
    selectedCountElement.classList.remove('text-gold');
    selectedCountElement.classList.add('text-amber-300');
  } else {
    capacityWarning.textContent = '';
    capacityWarning.classList.add('hidden');
    selectedCountElement.classList.remove('text-amber-300');
    selectedCountElement.classList.add('text-gold');
  }
}

function readNonNegativeNumber(value) {
  const number = Number(value);
  if (!Number.isFinite(number) || number < 0) {
    return 0;
  }
  return number;
}

function formatVnd(amount) {
  return new Intl.NumberFormat('vi-VN').format(amount) + 'đ';
}
