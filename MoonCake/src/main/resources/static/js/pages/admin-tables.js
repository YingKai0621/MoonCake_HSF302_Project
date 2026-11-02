(() => {
 const normalize = value => value.toLocaleLowerCase('vi').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd');
 document.querySelectorAll('table[data-search-table]').forEach((table,index) => {
  const rows = [...table.tBodies[0].rows];
  const bar = document.createElement('div'); bar.className = 'table-tools';
  const label = document.createElement('label'); label.textContent = 'Tìm trong bảng đang hiển thị';
  const input = document.createElement('input'); input.type = 'search'; input.placeholder = 'Tên, mã, trạng thái…'; label.append(input);
  const status = document.createElement('span'); status.setAttribute('role','status');
  const prev = document.createElement('button'); prev.type = 'button'; prev.textContent = '← Trước';
  const next = document.createElement('button'); next.type = 'button'; next.textContent = 'Sau →';
  bar.append(label, status, prev, next); table.before(bar);
  let page=0; const size=15;
  function render(){
   const query=normalize(input.value.trim()); const matched=rows.filter(row=>normalize(row.textContent).includes(query));
   const pages=Math.max(1,Math.ceil(matched.length/size)); page=Math.min(page,pages-1);
   const visible=new Set(matched.slice(page*size,(page+1)*size)); rows.forEach(row=>row.hidden=!visible.has(row));
   status.textContent=matched.length ? `${matched.length} bản ghi · Trang ${page+1}/${pages}` : 'Không có bản ghi phù hợp';
   prev.disabled=page===0; next.disabled=page>=pages-1;
  }
  input.addEventListener('input',()=>{page=0;render();});
  prev.addEventListener('click',()=>{page--;render();}); next.addEventListener('click',()=>{page++;render();}); render();
 });
 document.querySelectorAll('[data-batch-dialog]').forEach(button=>button.addEventListener('click',()=>document.getElementById(button.dataset.batchDialog).showModal()));
 document.querySelectorAll('[data-dialog-close]').forEach(button=>button.addEventListener('click',()=>button.closest('dialog').close()));
})();
