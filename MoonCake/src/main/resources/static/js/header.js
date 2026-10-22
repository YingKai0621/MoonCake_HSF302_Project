document.addEventListener('DOMContentLoaded', function () {
  var header = document.querySelector('[data-site-header]');
  if (!header) {
    return;
  }

  var menuButton = header.querySelector('[data-header-menu-toggle]');
  var panel = header.querySelector('[data-header-panel]');
  var dropdowns = header.querySelectorAll('[data-header-dropdown]');

  function closeDropdowns(exceptDropdown) {
    dropdowns.forEach(function (dropdown) {
      if (dropdown !== exceptDropdown) {
        dropdown.removeAttribute('open');
      }
    });
  }

  function setMobileMenuOpen(isOpen) {
    if (!menuButton || !panel) {
      return;
    }

    menuButton.setAttribute('aria-expanded', String(isOpen));
    menuButton.setAttribute('aria-label', isOpen ? 'Đóng menu' : 'Mở menu');
    panel.classList.toggle('site-header__panel--open', isOpen);

    if (!isOpen) {
      closeDropdowns(null);
    }
  }

  if (menuButton && panel) {
    menuButton.addEventListener('click', function () {
      var isOpen = menuButton.getAttribute('aria-expanded') === 'true';
      setMobileMenuOpen(!isOpen);
    });

    panel.addEventListener('click', function (event) {
      if (event.target instanceof Element && event.target.closest('a')) {
        setMobileMenuOpen(false);
      }
    });
  }

  dropdowns.forEach(function (dropdown) {
    dropdown.addEventListener('toggle', function () {
      if (dropdown.open) {
        closeDropdowns(dropdown);
      }
    });
  });

  document.addEventListener('click', function (event) {
    if (event.target instanceof Node && !header.contains(event.target)) {
      closeDropdowns(null);
      setMobileMenuOpen(false);
    }
  });

  document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape') {
      closeDropdowns(null);
      setMobileMenuOpen(false);
      if (menuButton && window.innerWidth <= 960) {
        menuButton.focus();
      }
    }
  });

  window.addEventListener('resize', function () {
    if (window.innerWidth > 960) {
      setMobileMenuOpen(false);
    }
  });
});
