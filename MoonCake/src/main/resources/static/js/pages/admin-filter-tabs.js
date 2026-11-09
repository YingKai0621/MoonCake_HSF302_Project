function setUpFilterTabs() {
  const tabs = document.querySelector("[data-filter-tabs]");
  if (tabs === null) {
    return;
  }
  const items = document.querySelectorAll("[data-group]");
  const empty = document.querySelector("[data-filter-empty]");
  const links = tabs.querySelectorAll("[data-filter]");

  function apply(filter) {
    let visible = 0;
    for (let i = 0; i < items.length; i++) {
      const groups = items[i].dataset.group.split(" ");
      const show = filter === "all" || groups.includes(filter);
      items[i].hidden = !show;
      if (show) {
        visible++;
      }
    }
    for (let i = 0; i < links.length; i++) {
      links[i].classList.toggle("is-active", links[i].dataset.filter === filter);
    }
    if (empty !== null) {
      empty.hidden = visible > 0;
    }
  }

  for (let i = 0; i < links.length; i++) {
    links[i].addEventListener("click", function (event) {
      event.preventDefault();
      apply(links[i].dataset.filter);
    });
  }
  apply(tabs.querySelector(".is-active").dataset.filter);
}

document.addEventListener("DOMContentLoaded", setUpFilterTabs);
