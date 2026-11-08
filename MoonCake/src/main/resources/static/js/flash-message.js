document.addEventListener('DOMContentLoaded', function () {
  var messages = document.querySelectorAll('[data-flash-message]');

  messages.forEach(function (message) {
    addDismissButton(message);
    removeHandledQueryParameter(message);

    var timeout = Number(message.getAttribute('data-flash-timeout'));
    if (Number.isFinite(timeout) && timeout > 0) {
      window.setTimeout(function () {
        dismissMessage(message);
      }, timeout);
    }
  });
});

function addDismissButton(message) {
  var button = document.createElement('button');
  button.type = 'button';
  button.className = 'flash-message__dismiss';
  button.setAttribute('aria-label', 'Đóng thông báo');
  button.textContent = '×';
  button.addEventListener('click', function () {
    dismissMessage(message);
  });
  message.appendChild(button);
}

function dismissMessage(message) {
  if (message.classList.contains('flash-message--leaving')) {
    return;
  }

  message.classList.add('flash-message--leaving');
  window.setTimeout(function () {
    message.remove();
  }, 220);
}

function removeHandledQueryParameter(message) {
  var parameterName = message.getAttribute('data-flash-query-param');
  if (!parameterName || !window.history || !window.history.replaceState) {
    return;
  }

  var url = new URL(window.location.href);
  if (!url.searchParams.has(parameterName)) {
    return;
  }

  url.searchParams.delete(parameterName);
  window.history.replaceState({}, document.title, url.pathname + url.search + url.hash);
}
