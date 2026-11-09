const THEME_KEY = "mc-admin-theme";

function currentTheme() {
  return document.documentElement.getAttribute("data-theme") === "dark" ? "dark" : "light";
}

function updateThemeLabel() {
  const label = document.querySelector("[data-theme-label]");
  if (label !== null) {
    label.textContent = currentTheme() === "dark" ? "Giao diện sáng" : "Giao diện tối";
  }
}

function toggleTheme() {
  const nextTheme = currentTheme() === "dark" ? "light" : "dark";
  document.documentElement.setAttribute("data-theme", nextTheme);
  try {
    localStorage.setItem(THEME_KEY, nextTheme);
  } catch (error) {
    return updateThemeLabel();
  }
  updateThemeLabel();
}

function markActiveLink() {
  const path = window.location.pathname;
  const links = document.querySelectorAll("[data-nav-path]");
  for (let i = 0; i < links.length; i++) {
    const linkPath = links[i].dataset.navPath;
    if (path === linkPath || path.startsWith(linkPath + "/")) {
      links[i].classList.add("is-active");
      links[i].setAttribute("aria-current", "page");
    }
  }
}

function setUpMobileSidebar() {
  const sidebar = document.querySelector("[data-admin-sidebar]");
  const toggle = document.querySelector("[data-sidebar-toggle]");
  const scrim = document.querySelector("[data-sidebar-close]");
  if (sidebar === null || toggle === null) {
    return;
  }

  function setOpen(open) {
    sidebar.classList.toggle("is-open", open);
    toggle.setAttribute("aria-expanded", String(open));
  }

  toggle.addEventListener("click", function () {
    setOpen(!sidebar.classList.contains("is-open"));
  });
  if (scrim !== null) {
    scrim.addEventListener("click", function () {
      setOpen(false);
    });
  }
  document.addEventListener("keydown", function (event) {
    if (event.key === "Escape") {
      setOpen(false);
    }
  });
}

function setUpSidebarCollapse() {
  const button = document.querySelector("[data-sidebar-collapse]");
  if (button === null) {
    return;
  }
  button.addEventListener("click", function () {
    const collapsed = document.documentElement.classList.toggle("sidebar-collapsed");
    button.title = collapsed ? "Mở rộng menu" : "Thu gọn menu";
    try {
      localStorage.setItem("mc-admin-sidebar", collapsed ? "collapsed" : "expanded");
    } catch (error) {
      button.blur();
    }
  });
}

function showPageProgress() {
  const bar = document.querySelector("[data-page-progress]");
  if (bar !== null) {
    bar.classList.remove("is-loading");
    void bar.offsetWidth;
    bar.classList.add("is-loading");
  }
}

function setUpPageProgress() {
  document.addEventListener("click", function (event) {
    const link = event.target.closest("a[href]");
    if (link === null || link.target === "_blank" || event.ctrlKey || event.metaKey) {
      return;
    }
    const url = new URL(link.href, window.location.href);
    const samePage = url.pathname === window.location.pathname && url.hash !== "";
    if (url.origin === window.location.origin && !samePage) {
      showPageProgress();
    }
  });
  document.addEventListener("submit", function (event) {
    setTimeout(function () {
      if (!event.defaultPrevented) {
        showPageProgress();
      }
    }, 0);
  });
  window.addEventListener("pageshow", function () {
    const bar = document.querySelector("[data-page-progress]");
    if (bar !== null) {
      bar.classList.remove("is-loading");
    }
  });
}

function setUpAdminUi() {
  const themeToggle = document.querySelector("[data-theme-toggle]");
  if (themeToggle !== null) {
    themeToggle.addEventListener("click", toggleTheme);
  }
  updateThemeLabel();
  markActiveLink();
  setUpMobileSidebar();
  setUpSidebarCollapse();
  setUpPageProgress();
}

document.addEventListener("DOMContentLoaded", setUpAdminUi);
