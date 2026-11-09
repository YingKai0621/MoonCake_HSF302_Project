const RING_LENGTH = 175.93;
const URGENT_SECONDS = 300;

function formatRemaining(totalSeconds) {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return String(minutes).padStart(2, "0") + ":" + String(seconds).padStart(2, "0");
}

function updateCountdown(ring, deadline, start) {
  const now = Date.now();
  const remainingSeconds = Math.max(0, Math.round((deadline - now) / 1000));
  const totalSeconds = Math.max(1, Math.round((deadline - start) / 1000));
  const fraction = Math.min(1, remainingSeconds / totalSeconds);

  ring.querySelector("[data-countdown-time]").textContent = formatRemaining(remainingSeconds);
  ring.querySelector("[data-countdown-progress]").style.strokeDashoffset = String(RING_LENGTH * (1 - fraction));
  ring.classList.toggle("is-urgent", remainingSeconds <= URGENT_SECONDS);

  return remainingSeconds > 0;
}

function lockPayment(messageText) {
  const message = document.querySelector("[data-countdown-message]");
  if (message !== null) {
    message.textContent = messageText;
  }
  const payButton = document.querySelector("[data-pay-button]");
  if (payButton !== null) {
    payButton.disabled = true;
    payButton.classList.add("opacity-50", "cursor-not-allowed");
  }
}

function expireWhileWatching() {
  lockPayment("Đã hết thời gian thanh toán. Đang cập nhật đơn hàng…");
  setTimeout(function () {
    window.location.reload();
  }, 2000);
}

function setUpCountdown() {
  const ring = document.querySelector("[data-countdown]");
  if (ring === null) {
    return;
  }
  const deadline = Number(ring.dataset.deadline);
  const start = Number(ring.dataset.start);

  if (!updateCountdown(ring, deadline, start)) {
    lockPayment("Đã hết thời gian thanh toán. Hệ thống đang hủy đơn, vui lòng tải lại trang sau ít phút.");
    return;
  }
  const timer = setInterval(function () {
    if (!updateCountdown(ring, deadline, start)) {
      clearInterval(timer);
      expireWhileWatching();
    }
  }, 1000);
}

document.addEventListener("DOMContentLoaded", setUpCountdown);
