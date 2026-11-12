const PANEL_BY_HASH = {
  info: "info",
  danger: "info",
  recipe: "recipe",
  pack: "sale",
  sale: "sale"
};

function panelForHash(hash) {
  const name = hash.replace("#", "");
  if (name.startsWith("sku-")) {
    return "info";
  }
  return PANEL_BY_HASH[name] || null;
}

function showPanel(tabs, name) {
  const target = tabs.querySelector('[data-panel="' + name + '"]');
  if (target === null || target.disabled) {
    name = tabs.dataset.default;
  }
  const panels = document.querySelectorAll("[data-step-panel]");
  for (let i = 0; i < panels.length; i++) {
    panels[i].hidden = panels[i].dataset.stepPanel !== name;
  }
  const buttons = tabs.querySelectorAll("[data-panel]");
  for (let i = 0; i < buttons.length; i++) {
    const active = buttons[i].dataset.panel === name;
    buttons[i].classList.toggle("is-current", active);
    buttons[i].setAttribute("aria-pressed", active ? "true" : "false");
  }
}

function setUpStepTabs() {
  const tabs = document.querySelector("[data-step-tabs]");
  if (tabs === null) {
    return;
  }
  const fromHash = panelForHash(location.hash);
  showPanel(tabs, fromHash || tabs.dataset.default);

  const buttons = tabs.querySelectorAll("[data-panel]");
  for (let i = 0; i < buttons.length; i++) {
    buttons[i].addEventListener("click", function () {
      const name = buttons[i].dataset.panel;
      showPanel(tabs, name);
      history.replaceState(null, "", "#" + name);
    });
  }
}

function setUpCakePicker(picker) {
  const capacity = Number(picker.dataset.capacity);
  const inputs = picker.querySelectorAll("[data-cake-input]");
  const bar = picker.querySelector("[data-fill-bar]");
  const text = picker.querySelector("[data-fill-text]");
  const hint = picker.querySelector("[data-fill-hint]");
  const meter = picker.querySelector("[data-fill-meter]");
  const lockInput = picker.querySelector("[data-lock-input]");
  const saveLock = picker.querySelector("[data-save-lock]");
  const saveDraft = picker.querySelector("[data-save-draft]");
  const costText = picker.querySelector("[data-cost-text]");
  const boxPrice = Number(picker.dataset.boxPrice) || 0;
  const giftPrice = Number(picker.dataset.giftPrice) || 0;
  let changed = false;

  function money(value) {
    return value.toLocaleString("vi-VN") + "đ";
  }

  function retailCost() {
    let sum = boxPrice;
    for (let i = 0; i < inputs.length; i++) {
      const card = inputs[i].closest(".pick-card");
      const count = Math.max(0, Number(inputs[i].value) || 0);
      sum += count * (Number(card.dataset.price) || 0);
    }
    return sum;
  }

  function total() {
    let sum = 0;
    for (let i = 0; i < inputs.length; i++) {
      sum += Math.max(0, Number(inputs[i].value) || 0);
    }
    return sum;
  }

  function refresh() {
    const sum = total();
    const percent = capacity > 0 ? Math.min(100, Math.round(sum * 100 / capacity)) : 0;
    bar.style.width = percent + "%";
    text.textContent = sum + "/" + capacity + " bánh";
    meter.classList.toggle("is-full", sum === capacity);
    meter.classList.toggle("is-over", sum > capacity);
    if (sum < capacity) {
      hint.textContent = "Còn thiếu " + (capacity - sum) + " bánh";
    } else if (sum > capacity) {
      hint.textContent = "Thừa " + (sum - capacity) + " bánh";
    } else {
      hint.textContent = changed ? "Đủ bánh. Hãy lưu lại." : "Đủ bánh";
    }
    for (let i = 0; i < inputs.length; i++) {
      const card = inputs[i].closest(".pick-card");
      const value = Math.max(0, Number(inputs[i].value) || 0);
      card.classList.toggle("is-picked", value > 0);
      card.querySelector("[data-cake-count]").textContent = value;
      const unavailable = card.classList.contains("is-unavailable");
      card.querySelector('[data-step="1"]').disabled = unavailable || sum >= capacity;
      card.querySelector('[data-step="-1"]').disabled = value <= 0;
    }
    costText.textContent = "Giá lẻ vỏ + bánh: " + money(retailCost()) + " · Giá bán: " + money(giftPrice);
    saveLock.disabled = sum !== capacity;
    if (sum === capacity) {
      saveLock.removeAttribute("title");
    } else {
      saveLock.title = "Cần đủ " + capacity + " bánh để khóa";
    }
  }

  saveDraft.addEventListener("click", function () {
    lockInput.value = "false";
    delete picker.dataset.confirmMessage;
  });
  function pickedCakes() {
    const names = [];
    for (let i = 0; i < inputs.length; i++) {
      const value = Math.max(0, Number(inputs[i].value) || 0);
      if (value > 0) {
        const card = inputs[i].closest(".pick-card");
        const label = card.querySelector(".pick-card__text strong").textContent.trim();
        const detail = card.querySelector(".pick-card__text small").textContent.trim();
        names.push(value + " " + label + " (" + detail + ")");
      }
    }
    return names.join(", ");
  }

  saveLock.addEventListener("click", function () {
    lockInput.value = "true";
    picker.dataset.confirmMessage = "Chốt danh sách bánh: " + pickedCakes() + "? " + saveLock.dataset.lockMessage;
  });

  const steppers = picker.querySelectorAll("[data-step]");
  for (let i = 0; i < steppers.length; i++) {
    steppers[i].addEventListener("click", function () {
      const input = steppers[i].closest(".pick-card").querySelector("[data-cake-input]");
      const next = Math.max(0, (Number(input.value) || 0) + Number(steppers[i].dataset.step));
      input.value = next;
      changed = true;
      refresh();
    });
  }
  for (let i = 0; i < inputs.length; i++) {
    inputs[i].addEventListener("input", function () {
      changed = true;
      refresh();
    });
  }
  picker.addEventListener("submit", function (event) {
    if (lockInput.value === "true" && total() !== capacity) {
      event.preventDefault();
      event.stopImmediatePropagation();
      hint.textContent = "Cần đủ " + capacity + " bánh để khóa.";
      return;
    }
    if (total() > capacity) {
      event.preventDefault();
      event.stopImmediatePropagation();
      hint.textContent = "Hộp chỉ chứa " + capacity + " bánh. Hãy bớt " + (total() - capacity) + " bánh.";
    }
  });
  refresh();
}

document.addEventListener("DOMContentLoaded", function () {
  setUpStepTabs();
  const pickers = document.querySelectorAll("[data-cake-picker]");
  for (let i = 0; i < pickers.length; i++) {
    setUpCakePicker(pickers[i]);
  }
});
