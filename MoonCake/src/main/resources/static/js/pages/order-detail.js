document.addEventListener('DOMContentLoaded', function () {
  const openButtons = document.querySelectorAll('[data-open-modal]');
  for (let i = 0; i < openButtons.length; i++) {
    const modal = document.getElementById(openButtons[i].dataset.openModal);
    if (modal === null) {
      continue;
    }
    openButtons[i].addEventListener('click', function () {
      modal.showModal();
    });
  }

  const closeButtons = document.querySelectorAll('[data-close-modal]');
  for (let i = 0; i < closeButtons.length; i++) {
    closeButtons[i].addEventListener('click', function () {
      closeButtons[i].closest('dialog').close();
    });
  }

  const modals = document.querySelectorAll('dialog');
  for (let i = 0; i < modals.length; i++) {
    modals[i].addEventListener('click', function (event) {
      if (event.target === modals[i]) {
        modals[i].close();
      }
    });
  }
});
