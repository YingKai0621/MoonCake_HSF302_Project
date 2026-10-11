/**
 * MoonCake Luxury Checkout Page Logic & Validation
 */

const subtotal = 1015000;
let shippingFee = 0;

// Province to Ward mapping for administrative consistency
const wardsData = {
  hcm: [
    { val: "ben-nghe", text: "Phường Bến Nghé (Quận 1)" },
    { val: "ben-thanh", text: "Phường Bến Thành (Quận 1)" },
    { val: "da-kao", text: "Phường Đa Kao (Quận 1)" },
    { val: "thao-dien", text: "Phường Thảo Điền (TP. Thủ Đức)" },
    { val: "vo-thi-sau", text: "Phường Võ Thị Sáu (Quận 3)" },
    { val: "phu-my-hung", text: "Phường Tân Phong (Quận 7)" }
  ],
  hn: [
    { val: "trang-tien", text: "Phường Tràng Tiền (Quận Hoàn Kiếm)" },
    { val: "hang-bac", text: "Phường Hàng Bạc (Quận Hoàn Kiếm)" },
    { val: "phan-chu-trinh", text: "Phường Phan Chu Trinh (Quận Hoàn Kiếm)" },
    { val: "dich-vong-hau", text: "Phường Dịch Vọng Hậu (Quận Cầu Giấy)" },
    { val: "lang-ha", text: "Phường Láng Hạ (Quận Đống Đa)" }
  ],
  dn: [
    { val: "hai-chau-1", text: "Phường Hải Châu 1 (Quận Hải Châu)" },
    { val: "thach-thang", text: "Phường Thạch Thang (Quận Hải Châu)" },
    { val: "an-hai-bac", text: "Phường An Hải Bắc (Quận Sơn Trà)" },
    { val: "khue-trung", text: "Phường Khuê Trung (Quận Cẩm Lệ)" }
  ],
  bd: [
    { val: "phu-cuong", text: "Phường Phú Cường (TP. Thủ Dầu Một)" },
    { val: "lai-thieu", text: "Phường Lái Thiêu (TP. Thuận An)" },
    { val: "di-an", text: "Phường Dĩ An (TP. Dĩ An)" }
  ],
  other: [
    { val: "trung-tam", text: "Khu vực trung tâm hành chính" },
    { val: "ngoai-thanh", text: "Khu vực ngoại thành / Huyện xã" }
  ]
};

function formatVND(amount) {
  return new Intl.NumberFormat('vi-VN').format(amount) + 'đ';
}

// Dynamic ward population and shipping fee calculation based on selected province
function handleLocationChange() {
  const citySelect = document.getElementById('citySelect');
  const wardSelect = document.getElementById('wardSelect');
  const feeText = document.getElementById('shippingFeeText');
  const totalText = document.getElementById('grandTotalText');
  const subtext = document.getElementById('shippingNoteSubtext');
  if (!citySelect || !wardSelect) return;

  const city = citySelect.value;

  // Populate wards dynamically
  wardSelect.innerHTML = '';
  if (city && wardsData[city]) {
    wardSelect.disabled = false;
    
    const defaultOption = document.createElement('option');
    defaultOption.value = '';
    defaultOption.disabled = true;
    defaultOption.selected = true;
    defaultOption.textContent = '-- Chọn Phường / Xã --';
    wardSelect.appendChild(defaultOption);

    wardsData[city].forEach(item => {
      const opt = document.createElement('option');
      opt.value = item.val;
      opt.textContent = item.text;
      wardSelect.appendChild(opt);
    });

    // Set demo shipping fee
    shippingFee = 30000;
    if (feeText) {
      feeText.classList.remove('italic');
      feeText.classList.add('font-medium', 'text-[#F8F4EB]');
      feeText.innerText = formatVND(shippingFee);
    }

    const grandTotal = subtotal + shippingFee;
    if (totalText) totalText.innerText = formatVND(grandTotal);
    if (subtext) subtext.innerText = '(đã bao gồm phí vận chuyển ước tính)';
    
    // Remove error state if present
    citySelect.classList.remove('input-error');
    const errCity = document.getElementById('errorCitySelect');
    if (errCity) errCity.classList.add('hidden');
  } else {
    wardSelect.disabled = true;
    const defaultOption = document.createElement('option');
    defaultOption.value = '';
    defaultOption.disabled = true;
    defaultOption.selected = true;
    defaultOption.textContent = '-- Vui lòng chọn Tỉnh / Thành trước --';
    wardSelect.appendChild(defaultOption);

    shippingFee = 0;
    if (feeText) {
      feeText.classList.add('italic');
      feeText.classList.remove('font-medium', 'text-[#F8F4EB]');
      feeText.innerText = 'Tính sau khi chọn địa chỉ';
    }
    if (totalText) totalText.innerText = formatVND(subtotal);
    if (subtext) subtext.innerText = '(chưa gồm phí vận chuyển)';
  }
}

// Payment Radio Selector
function selectPayment(type) {
  const codLabel = document.getElementById('paymentLabelCod');
  const bankLabel = document.getElementById('paymentLabelBank');
  const codRadio = document.getElementById('payCod');
  const bankRadio = document.getElementById('payBank');

  if (type === 'cod') {
    if (codRadio) codRadio.checked = true;
    if (codLabel) {
      codLabel.classList.add('border-[#D1A85A]', 'bg-[#162842]');
      codLabel.classList.remove('border-[#D1A85A]/20', 'bg-[#0C1626]');
    }
    if (bankLabel) {
      bankLabel.classList.remove('border-[#D1A85A]', 'bg-[#162842]');
      bankLabel.classList.add('border-[#D1A85A]/20', 'bg-[#0C1626]');
    }
  } else {
    if (bankRadio) bankRadio.checked = true;
    if (bankLabel) {
      bankLabel.classList.add('border-[#D1A85A]', 'bg-[#162842]');
      bankLabel.classList.remove('border-[#D1A85A]/20', 'bg-[#0C1626]');
    }
    if (codLabel) {
      codLabel.classList.remove('border-[#D1A85A]', 'bg-[#162842]');
      codLabel.classList.add('border-[#D1A85A]/20', 'bg-[#0C1626]');
    }
  }
}

// Client-side Form Validation
function validateForm() {
  let isValid = true;

  const fullName = document.getElementById('fullName');
  const phone = document.getElementById('phone');
  const email = document.getElementById('email');
  const city = document.getElementById('citySelect');
  const ward = document.getElementById('wardSelect');
  const address = document.getElementById('streetAddress');

  // 1. Check Full Name
  if (!fullName || !fullName.value.trim()) {
    if (fullName) fullName.classList.add('input-error');
    const err = document.getElementById('errorFullName');
    if (err) err.classList.remove('hidden');
    isValid = false;
  } else {
    fullName.classList.remove('input-error');
    const err = document.getElementById('errorFullName');
    if (err) err.classList.add('hidden');
  }

  // 2. Check Phone
  const phoneRegex = /^[0-9\s+()-]{9,15}$/;
  if (!phone || !phone.value.trim() || !phoneRegex.test(phone.value.trim())) {
    if (phone) phone.classList.add('input-error');
    const err = document.getElementById('errorPhone');
    if (err) err.classList.remove('hidden');
    isValid = false;
  } else {
    phone.classList.remove('input-error');
    const err = document.getElementById('errorPhone');
    if (err) err.classList.add('hidden');
  }

  // 3. Check Optional Email (validate format only if entered)
  if (email) {
    const emailVal = email.value.trim();
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (emailVal !== '' && !emailRegex.test(emailVal)) {
      email.classList.add('input-error');
      const err = document.getElementById('errorEmail');
      if (err) err.classList.remove('hidden');
      isValid = false;
    } else {
      email.classList.remove('input-error');
      const err = document.getElementById('errorEmail');
      if (err) err.classList.add('hidden');
    }
  }

  // 4. Check City
  if (!city || !city.value) {
    if (city) city.classList.add('input-error');
    const err = document.getElementById('errorCitySelect');
    if (err) err.classList.remove('hidden');
    isValid = false;
  } else {
    city.classList.remove('input-error');
    const err = document.getElementById('errorCitySelect');
    if (err) err.classList.add('hidden');
  }

  // 5. Check Ward
  if (!ward || !ward.value) {
    if (ward) ward.classList.add('input-error');
    const err = document.getElementById('errorWardSelect');
    if (err) err.classList.remove('hidden');
    isValid = false;
  } else {
    ward.classList.remove('input-error');
    const err = document.getElementById('errorWardSelect');
    if (err) err.classList.add('hidden');
  }

  // 6. Check Street Address
  if (!address || !address.value.trim()) {
    if (address) address.classList.add('input-error');
    const err = document.getElementById('errorStreetAddress');
    if (err) err.classList.remove('hidden');
    isValid = false;
  } else {
    address.classList.remove('input-error');
    const err = document.getElementById('errorStreetAddress');
    if (err) err.classList.add('hidden');
  }

  if (!isValid) {
    const firstError = document.querySelector('.input-error');
    if (firstError) {
      firstError.scrollIntoView({ behavior: 'smooth', block: 'center' });
      firstError.focus();
    }
  }

  return isValid;
}

// Handle Form Submit (Frontend Demo Prototype)
function handlePlaceOrder(event) {
  if (event) event.preventDefault();
  if (!validateForm()) return;

  const totalFormatted = formatVND(subtotal + shippingFee);
  alert(`[Xác nhận Front-end Prototype]\nĐơn hàng đã được kiểm tra tính hợp lệ!\nTổng thanh toán: ${totalFormatted}\n\nChuyển hướng đến màn hình xác nhận đơn hàng mẫu.`);
  window.location.href = '/order-success';
}

document.addEventListener('DOMContentLoaded', function () {
  const errorMap = {
    fullName: 'errorFullName',
    phone: 'errorPhone',
    email: 'errorEmail',
    streetAddress: 'errorStreetAddress'
  };

  Object.keys(errorMap).forEach(fieldId => {
    const el = document.getElementById(fieldId);
    if (el) {
      el.addEventListener('input', () => {
        el.classList.remove('input-error');
        const errEl = document.getElementById(errorMap[fieldId]);
        if (errEl) errEl.classList.add('hidden');
      });
    }
  });

  const citySelect = document.getElementById('citySelect');
  if (citySelect) {
    citySelect.addEventListener('change', () => {
      citySelect.classList.remove('input-error');
      const errCity = document.getElementById('errorCitySelect');
      if (errCity) errCity.classList.add('hidden');
    });
  }

  const wardSelect = document.getElementById('wardSelect');
  if (wardSelect) {
    wardSelect.addEventListener('change', () => {
      wardSelect.classList.remove('input-error');
      const errWard = document.getElementById('errorWardSelect');
      if (errWard) errWard.classList.add('hidden');
    });
  }
});
