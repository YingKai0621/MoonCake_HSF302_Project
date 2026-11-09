function openModal(trigger) {
  const dialog = document.getElementById(trigger.dataset.openModal);
  if (dialog === null) {
    return;
  }
  const form = dialog.querySelector("form");
  if (form !== null) {
    form.reset();
    const errors = form.querySelectorAll(".field-error");
    for (let i = 0; i < errors.length; i++) {
      errors[i].remove();
    }
    const invalid = form.querySelectorAll("[aria-invalid]");
    for (let i = 0; i < invalid.length; i++) {
      invalid[i].removeAttribute("aria-invalid");
    }
  }
  const variant = dialog.querySelector("[data-modal-variant]");
  if (variant !== null) {
    variant.value = trigger.dataset.variantId;
  }
  const subject = dialog.querySelector("[data-modal-subject]");
  if (subject !== null && trigger.dataset.variantLabel) {
    subject.textContent = trigger.dataset.variantLabel;
  }
  dialog.showModal();
  const firstField = dialog.querySelector("input:not([type=hidden]), select");
  if (firstField !== null) {
    firstField.focus();
  }
}

function closeOnBackdrop(event) {
  const dialog = event.target;
  if (dialog.tagName === "DIALOG" && dialog.classList.contains("modal")) {
    dialog.close();
  }
}

document.addEventListener("DOMContentLoaded", function () {
  const triggers = document.querySelectorAll("[data-open-modal]");
  for (let i = 0; i < triggers.length; i++) {
    triggers[i].addEventListener("click", function () {
      openModal(triggers[i]);
    });
  }
  const dialogs = document.querySelectorAll("dialog.modal");
  for (let i = 0; i < dialogs.length; i++) {
    dialogs[i].addEventListener("click", closeOnBackdrop);
  }
});
