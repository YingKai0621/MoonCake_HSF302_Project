const profileForms = document.querySelectorAll('[data-profile-edit-form]');

for (const form of profileForms) {
  const cancelButton = form.querySelector('[data-profile-cancel]');
  const fields = form.querySelectorAll('input:not([type="hidden"]), textarea, select');
  const originalValues = new Map();

  for (const field of fields) {
    originalValues.set(field.name, field.value);
  }

  function updateCancelButton() {
    let changed = false;

    for (const field of fields) {
      if (field.value !== originalValues.get(field.name)) {
        changed = true;
      }
    }

    cancelButton.hidden = !changed;
  }

  form.addEventListener('input', updateCancelButton);
  form.addEventListener('change', updateCancelButton);
  form.addEventListener('reset', () => {
    window.setTimeout(updateCancelButton, 0);
  });
  form.addEventListener('submit', () => {
    cancelButton.hidden = true;
  });
}
