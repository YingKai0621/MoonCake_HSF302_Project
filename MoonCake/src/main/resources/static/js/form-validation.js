function errorMessage(field) {
  const state = field.validity;
  if (state.valueMissing) {
    if (field.tagName === "SELECT") {
      return "Vui lòng chọn một mục.";
    }
    if (field.type === "file") {
      return "Vui lòng chọn ảnh.";
    }
    if (field.type === "checkbox") {
      return "Vui lòng tích vào ô này để tiếp tục.";
    }
    return "Vui lòng nhập thông tin này.";
  }
  if (state.badInput) {
    return "Vui lòng nhập số.";
  }
  if (state.typeMismatch && field.type === "email") {
    return "Email chưa đúng, ví dụ: ten@gmail.com.";
  }
  if (state.patternMismatch) {
    return field.dataset.error || "Giá trị chưa đúng định dạng.";
  }
  if (state.tooShort) {
    return "Cần ít nhất " + field.minLength + " ký tự.";
  }
  if (state.tooLong) {
    return "Tối đa " + field.maxLength + " ký tự.";
  }
  if (state.rangeUnderflow) {
    return field.dataset.minError || "Giá trị nhỏ nhất là " + field.min + ".";
  }
  if (state.rangeOverflow) {
    return field.dataset.maxError || "Giá trị lớn nhất là " + field.max + ".";
  }
  if (state.stepMismatch) {
    return field.dataset.stepError || "Vui lòng nhập số nguyên.";
  }
  if (state.customError) {
    return field.validationMessage;
  }
  return "Giá trị chưa hợp lệ.";
}

function errorAnchor(field) {
  const stepper = field.closest("[data-quantity-stepper]");
  if (stepper !== null) {
    return stepper.parentElement;
  }
  const label = field.closest("label");
  if (label === null) {
    return field;
  }
  const outside = label.classList.contains("dropzone")
    || label.classList.contains("switch-card")
    || label.classList.contains("checkbox")
    || field.type === "checkbox";
  if (outside) {
    return label;
  }
  return null;
}

function findError(field) {
  if (field.id === "") {
    return null;
  }
  return document.getElementById(field.id + "-error");
}

let errorCounter = 0;

function showFieldError(field) {
  if (field.id === "") {
    errorCounter++;
    field.id = "field-" + errorCounter;
  }
  let error = findError(field);
  if (error === null) {
    error = document.createElement("p");
    error.className = "field-error";
    error.id = field.id + "-error";
    error.setAttribute("role", "alert");
    const anchor = errorAnchor(field);
    if (anchor === null) {
      field.closest("label").append(error);
    } else {
      anchor.after(error);
    }
  }
  error.textContent = errorMessage(field);
  field.setAttribute("aria-invalid", "true");
  field.setAttribute("aria-describedby", error.id);
}

function clearFieldError(field) {
  const error = findError(field);
  if (error !== null) {
    error.remove();
  }
  field.removeAttribute("aria-invalid");
  field.removeAttribute("aria-describedby");
}

function applyCustomRules(field) {
  if (field.dataset.notZero === undefined) {
    return;
  }
  if (field.value !== "" && Number(field.value) === 0) {
    field.setCustomValidity(field.dataset.notZero);
  } else {
    field.setCustomValidity("");
  }
}

function checkField(field) {
  applyCustomRules(field);
  if (field.checkValidity()) {
    clearFieldError(field);
    return true;
  }
  showFieldError(field);
  return false;
}

function isCheckable(element) {
  if (!element.willValidate) {
    return false;
  }
  return element.tagName === "INPUT" || element.tagName === "SELECT" || element.tagName === "TEXTAREA";
}

function validateForm(event) {
  const form = event.target;
  if (!(form instanceof HTMLFormElement)) {
    return;
  }
  let firstInvalid = null;
  for (let i = 0; i < form.elements.length; i++) {
    const element = form.elements[i];
    if (!isCheckable(element)) {
      continue;
    }
    if (!checkField(element) && firstInvalid === null) {
      firstInvalid = element;
    }
  }
  if (firstInvalid === null) {
    return;
  }
  event.preventDefault();
  event.stopImmediatePropagation();
  const details = firstInvalid.closest("details");
  if (details !== null) {
    details.open = true;
  }
  firstInvalid.focus({ preventScroll: true });
  firstInvalid.scrollIntoView({ block: "center", behavior: "smooth" });
}

function recheckAfterChange(event) {
  const field = event.target;
  if (field.getAttribute("aria-invalid") === "true" && isCheckable(field)) {
    checkField(field);
  }
}

function recheckOnLeave(event) {
  const field = event.target;
  if (!isCheckable(field) || field.value === "") {
    return;
  }
  checkField(field);
}

document.addEventListener("DOMContentLoaded", function () {
  const forms = document.querySelectorAll("form");
  for (let i = 0; i < forms.length; i++) {
    forms[i].noValidate = true;
  }
});
document.addEventListener("submit", validateForm, true);
document.addEventListener("input", recheckAfterChange);
document.addEventListener("change", recheckAfterChange);
document.addEventListener("focusout", recheckOnLeave);
