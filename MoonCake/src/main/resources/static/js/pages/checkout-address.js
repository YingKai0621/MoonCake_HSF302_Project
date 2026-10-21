document.addEventListener('DOMContentLoaded', function () {
  var provinceSelect = document.getElementById('provinceCode');
  var wardSelect = document.getElementById('wardCode');

  if (!provinceSelect || !wardSelect) {
    return;
  }

  var wardOptions = copyWardOptions(wardSelect);
  var selectedWard = wardSelect.value;

  showMatchingWards(
    wardSelect,
    wardOptions,
    provinceSelect.value,
    selectedWard
  );

  provinceSelect.addEventListener('change', function () {
    showMatchingWards(
      wardSelect,
      wardOptions,
      provinceSelect.value,
      ''
    );
  });
});

function copyWardOptions(wardSelect) {
  var options = [];
  var childOptions = wardSelect.querySelectorAll('option[data-parent-code]');

  for (var index = 0; index < childOptions.length; index++) {
    options.push(childOptions[index].cloneNode(true));
  }
  return options;
}

function showMatchingWards(
  wardSelect,
  allWardOptions,
  provinceCode,
  selectedWardCode
) {
  wardSelect.innerHTML = '';

  var placeholderOption = document.createElement('option');
  placeholderOption.value = '';
  placeholderOption.textContent = '-- Chọn Phường/Xã/Đặc khu --';
  wardSelect.appendChild(placeholderOption);

  for (var index = 0; index < allWardOptions.length; index++) {
    var option = allWardOptions[index];
    if (option.dataset.parentCode === provinceCode) {
      var copiedOption = option.cloneNode(true);
      copiedOption.selected = copiedOption.value === selectedWardCode;
      wardSelect.appendChild(copiedOption);
    }
  }

  wardSelect.disabled = provinceCode === '';
}
