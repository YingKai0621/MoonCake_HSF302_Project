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

function pageNumbersToShow(page, pageCount) {
  const numbers = [];
  for (let i = 0; i < pageCount; i++) {
    const isEdge = i === 0 || i === pageCount - 1;
    const isNear = Math.abs(i - page) <= 1;
    if (isEdge || isNear) {
      numbers.push(i);
    } else if (numbers[numbers.length - 1] !== -1) {
      numbers.push(-1);
    }
  }
  return numbers;
}

function setUpSearchableTable(table) {
  const rows = Array.from(table.tBodies[0].rows);
  const pagedByServer = table.hasAttribute("data-server-paged");
  const rowsPerPage = pagedByServer ? rows.length || 1 : ROWS_PER_PAGE;

  const bar = document.createElement("div");
  bar.className = "table-tools";

  const label = document.createElement("label");
  label.textContent = "Tìm trong bảng";
  const input = document.createElement("input");
  input.type = "search";
  input.placeholder = "Tên, mã, trạng thái…";
  label.append(input);
  bar.append(label);
  table.before(bar);

  const pager = document.createElement("div");
  pager.className = "table-pager";
  const status = document.createElement("span");
  status.className = "table-pager__status";
  status.setAttribute("role", "status");
  const pages = document.createElement("div");
  pages.className = "table-pager__pages";
  pager.append(status, pages);
  table.after(pager);

  let page = 0;

  function goTo(newPage) {
    page = newPage;
    render();
    bar.scrollIntoView({ block: "nearest" });
  }

  function renderPageButtons(pageCount) {
    pages.replaceChildren();
    if (pageCount <= 1) {
      return;
    }
    const previousButton = createButton("← Trước");
    previousButton.disabled = page === 0;
    previousButton.addEventListener("click", function () {
      goTo(page - 1);
    });
    pages.append(previousButton);

    const numbers = pageNumbersToShow(page, pageCount);
    for (let i = 0; i < numbers.length; i++) {
      const number = numbers[i];
      if (number === -1) {
        const gap = document.createElement("span");
        gap.className = "table-pager__status";
        gap.textContent = "…";
        pages.append(gap);
        continue;
      }
      const button = createButton(String(number + 1));
      if (number === page) {
        button.classList.add("is-current");
        button.setAttribute("aria-current", "page");
      }
      button.addEventListener("click", function () {
        goTo(number);
      });
      pages.append(button);
    }

    const nextButton = createButton("Sau →");
    nextButton.disabled = page >= pageCount - 1;
    nextButton.addEventListener("click", function () {
      goTo(page + 1);
    });
    pages.append(nextButton);
  }

  function render() {
    const query = normalizeText(input.value.trim());
    const matched = rows.filter(function (row) {
      if (row.dataset.filteredOut === "true") {
        return false;
      }
      return normalizeText(row.textContent).includes(query);
    });
    const pageCount = Math.max(1, Math.ceil(matched.length / rowsPerPage));
    page = Math.min(page, pageCount - 1);

    const visible = new Set(matched.slice(page * rowsPerPage, (page + 1) * rowsPerPage));
    for (let i = 0; i < rows.length; i++) {
      rows[i].hidden = !visible.has(rows[i]);
    }

    pager.hidden = rows.length === 0 || pagedByServer;
    if (matched.length === 0) {
      status.textContent = "Không có kết quả phù hợp";
    } else if (pageCount === 1) {
      status.textContent = matched.length + " kết quả";
    } else {
      const first = page * rowsPerPage + 1;
      const last = Math.min(matched.length, (page + 1) * rowsPerPage);
      status.textContent = "Hiển thị " + first + "–" + last + " trên " + matched.length + " kết quả";
    }
    renderPageButtons(pageCount);
  }

  input.addEventListener("input", function () {
    page = 0;
    render();
  });
  document.addEventListener("filterchange", function () {
    page = 0;
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
