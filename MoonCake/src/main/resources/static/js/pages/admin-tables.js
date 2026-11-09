const ROWS_PER_PAGE = 15;

function normalizeText(value) {
  return value
    .toLocaleLowerCase("vi")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/đ/g, "d");
}

function createButton(text) {
  const button = document.createElement("button");
  button.type = "button";
  button.textContent = text;
  return button;
}

function setUpSearchableTable(table) {
  const rows = Array.from(table.tBodies[0].rows);

  const bar = document.createElement("div");
  bar.className = "table-tools";

  const label = document.createElement("label");
  label.textContent = "Tìm trong bảng";
  const input = document.createElement("input");
  input.type = "search";
  input.placeholder = "Tên, mã, trạng thái…";
  label.append(input);

  const status = document.createElement("span");
  status.setAttribute("role", "status");
  const previousButton = createButton("← Trước");
  const nextButton = createButton("Sau →");

  bar.append(label, status, previousButton, nextButton);
  table.before(bar);

  let page = 0;

  function render() {
    const query = normalizeText(input.value.trim());
    const matched = rows.filter(function (row) {
      return normalizeText(row.textContent).includes(query);
    });
    const pageCount = Math.max(1, Math.ceil(matched.length / ROWS_PER_PAGE));
    page = Math.min(page, pageCount - 1);

    const visible = new Set(matched.slice(page * ROWS_PER_PAGE, (page + 1) * ROWS_PER_PAGE));
    for (let i = 0; i < rows.length; i++) {
      rows[i].hidden = !visible.has(rows[i]);
    }

    if (matched.length === 0) {
      status.textContent = "Không có bản ghi phù hợp";
    } else {
      status.textContent = matched.length + " bản ghi · Trang " + (page + 1) + "/" + pageCount;
    }
    previousButton.disabled = page === 0;
    nextButton.disabled = page >= pageCount - 1;
  }

  input.addEventListener("input", function () {
    page = 0;
    render();
  });
  previousButton.addEventListener("click", function () {
    page--;
    render();
  });
  nextButton.addEventListener("click", function () {
    page++;
    render();
  });
  render();
}

function setUpDialogButtons() {
  const openButtons = document.querySelectorAll("[data-batch-dialog]");
  for (let i = 0; i < openButtons.length; i++) {
    openButtons[i].addEventListener("click", function () {
      document.getElementById(openButtons[i].dataset.batchDialog).showModal();
    });
  }
  const closeButtons = document.querySelectorAll("[data-dialog-close]");
  for (let i = 0; i < closeButtons.length; i++) {
    closeButtons[i].addEventListener("click", function () {
      closeButtons[i].closest("dialog").close();
    });
  }
}

function setUpAdminTables() {
  const tables = document.querySelectorAll("table[data-search-table]");
  for (let i = 0; i < tables.length; i++) {
    setUpSearchableTable(tables[i]);
  }
  setUpDialogButtons();
}

document.addEventListener("DOMContentLoaded", setUpAdminTables);
