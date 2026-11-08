document.addEventListener('submit', event => {
 const form = event.target;
 if (!(form instanceof HTMLFormElement)) return;
 const target = form.querySelector('input[name="target"]')?.value;
 let message = '';
 if (target === 'CANCELLED') message = 'Hủy đơn và hoàn lại tồn kho? Thao tác này không thể hoàn tác.';
 else if (target === 'COMPLETED') message = 'Xác nhận đã giao hàng thành công và đã nhận đủ thanh toán?';
 else if (form.action.endsWith('/remove')) message = form.action.includes('/media/') ? 'Xóa video này khỏi sản phẩm?' : 'Xóa ảnh này khỏi sản phẩm?';
 else if (form.action.endsWith('/activate')) message = 'Khóa công thức READY? Sau bước này, thay thành phần sẽ cần tạo SKU mới.';
 if (message && !window.confirm(message)) event.preventDefault();
});
