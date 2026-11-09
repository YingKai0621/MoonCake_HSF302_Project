function confirmationMessage(form) {
  if (form.dataset.confirmMessage) {
    return form.dataset.confirmMessage;
  }
  const targetInput = form.querySelector('input[name="target"]');
  const target = targetInput === null ? "" : targetInput.value;
  const action = form.action;

  if (target === "CANCELLED") {
    return "Hủy đơn và hoàn lại tồn kho? Thao tác này không thể hoàn tác.";
  }
  if (target === "COMPLETED") {
    return "Xác nhận đã giao hàng thành công và đã nhận đủ thanh toán?";
  }
  if (action.endsWith("/remove")) {
    return action.includes("/media/") ? "Xóa video này khỏi sản phẩm?" : "Xóa ảnh này khỏi sản phẩm?";
  }
  if (action.endsWith("/activate")) {
    return "Khóa công thức READY? Sau bước này, thay thành phần sẽ cần tạo SKU mới.";
  }
  return "";
}

function createConfirmDialog() {
  const dialog = document.createElement("dialog");
  dialog.className = "confirm-dialog";
  dialog.innerHTML =
    '<h2>Xác nhận thao tác</h2>' +
    '<p data-confirm-message></p>' +
    '<div class="confirm-dialog__actions">' +
    '<button type="button" class="secondary" data-confirm-cancel>Quay lại</button>' +
    '<button type="button" class="danger" data-confirm-ok>Đồng ý</button>' +
    "</div>";
  document.body.appendChild(dialog);
  return dialog;
}

function setUpConfirmations() {
  const dialog = createConfirmDialog();
  const message = dialog.querySelector("[data-confirm-message]");
  let pendingForm = null;

  dialog.querySelector("[data-confirm-cancel]").addEventListener("click", function () {
    pendingForm = null;
    dialog.close();
  });

  dialog.querySelector("[data-confirm-ok]").addEventListener("click", function () {
    const form = pendingForm;
    pendingForm = null;
    dialog.close();
    if (form !== null) {
      form.dataset.confirmed = "true";
      form.requestSubmit();
    }
  });

  document.addEventListener("submit", function (event) {
    if (event.defaultPrevented) {
      return;
    }
    const form = event.target;
    if (!(form instanceof HTMLFormElement)) {
      return;
    }
    if (form.dataset.confirmed === "true") {
      delete form.dataset.confirmed;
      return;
    }
    const text = confirmationMessage(form);
    if (text === "") {
      return;
    }
    event.preventDefault();
    pendingForm = form;
    message.textContent = text;
    dialog.showModal();
  });
}

document.addEventListener("DOMContentLoaded", setUpConfirmations);
