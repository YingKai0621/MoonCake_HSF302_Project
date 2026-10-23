document.addEventListener('DOMContentLoaded', function () {
  var toggleButtons = document.querySelectorAll('[data-password-toggle]');

  toggleButtons.forEach(function (button) {
    var targetId = button.getAttribute('data-target');
    var passwordName = button.getAttribute('data-password-name') || 'mật khẩu';
    var passwordInput = document.getElementById(targetId);

    if (!passwordInput) {
      return;
    }

    button.setAttribute('aria-pressed', 'false');

    button.addEventListener('click', function () {
      var passwordIsVisible = passwordInput.type === 'text';

      passwordInput.type = passwordIsVisible ? 'password' : 'text';
      button.textContent = passwordIsVisible ? 'Hiện' : 'Ẩn';
      button.setAttribute('aria-pressed', String(!passwordIsVisible));
      button.setAttribute(
        'aria-label',
        (passwordIsVisible ? 'Hiện ' : 'Ẩn ') + passwordName
      );
    });
  });
});
