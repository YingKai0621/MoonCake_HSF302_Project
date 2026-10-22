document.addEventListener('DOMContentLoaded', function () {
  const modal = document.getElementById('cancel-order-modal');
  const openButton = document.querySelector('[data-open-cancel-modal]');
  const closeButton = document.querySelector('[data-close-cancel-modal]');

  if (!modal || !openButton || !closeButton) {
    return;
  }

  openButton.addEventListener('click', function () {
    modal.showModal();
  });

  closeButton.addEventListener('click', function () {
    modal.close();
  });

  modal.addEventListener('click', function (event) {
    if (event.target === modal) {
      modal.close();
    }
  });
});
