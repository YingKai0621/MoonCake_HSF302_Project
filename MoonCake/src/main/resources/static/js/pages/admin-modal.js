function applyProductionDate(dialog, trigger) {
  const wrapper = dialog.querySelector("[data-production-date]");
  if (wrapper === null || trigger === null) {
    return;
  }
  const input = wrapper.querySelector("input");
  const needed = trigger.dataset.needsDate === "true";
  wrapper.hidden = !needed;
  input.disabled = !needed;
}

function openModal(trigger) {
  const dialog = document.getElementById(trigger.dataset.openModal);
  if (dialog === null) {
    return;
  }
  const serverError = dialog.querySelector(".modal__error");
  if (serverError !== null) {
    serverError.remove();
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
  if (variant !== null && trigger.dataset.variantId !== undefined) {
    variant.value = trigger.dataset.variantId;
  }
  const subject = dialog.querySelector("[data-modal-subject]");
  if (subject !== null && trigger.dataset.variantLabel) {
    subject.textContent = trigger.dataset.variantLabel;
  }
  applyProductionDate(dialog, trigger);
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

function reopenAfterError() {
  const dialog = document.querySelector("dialog[data-reopen]");
  if (dialog === null) {
    return;
  }
  const parent = dialog.closest("details");
  if (parent !== null) {
    parent.open = true;
  }
  const variant = dialog.querySelector("[data-modal-variant]");
  const subject = dialog.querySelector("[data-modal-subject]");
  if (variant !== null && subject !== null) {
    const trigger = document.querySelector(
      '[data-open-modal="' + dialog.id + '"][data-variant-id="' + variant.value + '"]'
    );
    if (trigger !== null) {
      subject.textContent = trigger.dataset.variantLabel;
    }
    applyProductionDate(dialog, trigger);
  }
  dialog.showModal();
}

document.addEventListener("DOMContentLoaded", function () {
  reopenAfterError();
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
