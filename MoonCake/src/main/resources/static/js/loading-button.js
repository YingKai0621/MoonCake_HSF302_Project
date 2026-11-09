function showLoading(button) {
  button.dataset.originalHtml = button.innerHTML;
  button.classList.add("is-loading");
  button.setAttribute("aria-busy", "true");

  const spinner = document.createElement("span");
  spinner.className = "loading-spinner";
  spinner.setAttribute("aria-hidden", "true");

  const label = document.createElement("span");
  label.textContent = button.dataset.loadingText;

  button.innerHTML = "";
  button.append(spinner, label);

  setTimeout(function () {
    button.disabled = true;
  }, 0);
}

function restoreButton(button) {
  if (button.dataset.originalHtml !== undefined) {
    button.innerHTML = button.dataset.originalHtml;
    delete button.dataset.originalHtml;
  }
  button.classList.remove("is-loading");
  button.removeAttribute("aria-busy");
  button.disabled = false;
}

function handleFormSubmit(event) {
  if (event.defaultPrevented) {
    return;
  }

  const form = event.target;
  if (form.dataset.submitting === "true") {
    event.preventDefault();
    return;
  }

  const button = event.submitter;
  if (!button || !button.dataset.loadingText) {
    return;
  }

  form.dataset.submitting = "true";
  showLoading(button);
}

function restoreAfterBackNavigation(event) {
  if (!event.persisted) {
    return;
  }

  const buttons = document.querySelectorAll(".is-loading");
  for (let i = 0; i < buttons.length; i++) {
    restoreButton(buttons[i]);
  }

  const forms = document.querySelectorAll("form[data-submitting]");
  for (let i = 0; i < forms.length; i++) {
    delete forms[i].dataset.submitting;
  }
}

document.addEventListener("submit", handleFormSubmit);
window.addEventListener("pageshow", restoreAfterBackNavigation);
