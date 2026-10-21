/**
 * MoonCake Luxury Mid-Autumn E-Commerce
 * Main Shared Client Interactions
 */

document.addEventListener('DOMContentLoaded', function () {
  initializeQuantitySteppers();

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

  // Lightweight Scroll-Triggered Reveal for "Khám phá hương vị"
  const flavorSection = document.getElementById('kham-pha-huong-vi');
  if (flavorSection) {
    if ('IntersectionObserver' in window) {
      const flavorObserver = new IntersectionObserver(function (entries, observer) {
        entries.forEach(function (entry) {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-revealed');
            observer.unobserve(entry.target);
          }
        });
      }, {
        threshold: 0.12,
        rootMargin: '0px 0px -40px 0px'
      });
      flavorObserver.observe(flavorSection);
    } else {
      flavorSection.classList.add('is-revealed');
    }
  }
});

function initializeQuantitySteppers() {
  const steppers = document.querySelectorAll('[data-quantity-stepper]');

  steppers.forEach(function (stepper) {
    const input = stepper.querySelector('input[type="number"]');
    const decreaseButton = stepper.querySelector('[data-quantity-action="decrease"]');
    const buttons = stepper.querySelectorAll('[data-quantity-action]');

    if (!input) {
      return;
    }

    updateDecreaseButton(input, decreaseButton);

    buttons.forEach(function (button) {
      button.addEventListener('click', function () {
        const previousValue = input.value;
        const action = button.getAttribute('data-quantity-action');

        if (action === 'decrease') {
          input.stepDown();
        } else {
          input.stepUp();
        }

        updateDecreaseButton(input, decreaseButton);
        if (input.value !== previousValue) {
          submitQuantityIfRequired(stepper, input);
        }
      });
    });

    input.addEventListener('input', function () {
      updateDecreaseButton(input, decreaseButton);
    });

    input.addEventListener('change', function () {
      if (input.checkValidity()) {
        submitQuantityIfRequired(stepper, input);
      }
    });
  });
}

function updateDecreaseButton(input, decreaseButton) {
  if (!decreaseButton) {
    return;
  }

  const minimum = Number(input.min || 1);
  const quantity = Number(input.value);
  decreaseButton.disabled = !Number.isFinite(quantity) || quantity <= minimum;
}

function submitQuantityIfRequired(stepper, input) {
  const shouldSubmit = stepper.getAttribute('data-auto-submit') === 'true';
  if (shouldSubmit && input.form) {
    input.form.requestSubmit();
  }
}
