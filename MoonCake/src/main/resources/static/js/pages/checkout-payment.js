function findCheckedPaymentOption(options) {
  for (let i = 0; i < options.length; i++) {
    if (options[i].checked) {
      return options[i];
    }
  }
  return null;
}

function updateSubmitLabel() {
  const submitButton = document.getElementById("checkoutSubmit");
  const options = document.querySelectorAll(".payment-option__radio");
  const checkedOption = findCheckedPaymentOption(options);
  if (submitButton === null || checkedOption === null) {
    return;
  }
  submitButton.textContent = checkedOption.dataset.submitLabel;
  submitButton.dataset.loadingText = checkedOption.dataset.loadingLabel;
}

function setUpPaymentOptions() {
  const options = document.querySelectorAll(".payment-option__radio");
  for (let i = 0; i < options.length; i++) {
    options[i].addEventListener("change", updateSubmitLabel);
  }
  updateSubmitLabel();
}

document.addEventListener("DOMContentLoaded", setUpPaymentOptions);
