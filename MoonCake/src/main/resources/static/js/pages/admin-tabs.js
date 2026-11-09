function moveIndicator(tabs, item, animate) {
  const indicator = tabs.querySelector(".tabs__indicator");
  if (indicator === null || item === null) {
    return;
  }
  indicator.style.transition = animate ? "" : "none";
  indicator.style.width = item.offsetWidth + "px";
  indicator.style.transform = "translateX(" + item.offsetLeft + "px)";
}

function setUpTabs() {
  const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const groups = document.querySelectorAll("[data-tabs]");
  for (let i = 0; i < groups.length; i++) {
    const tabs = groups[i];
    moveIndicator(tabs, tabs.querySelector(".is-active"), false);

    const items = tabs.querySelectorAll(".tabs__item");
    for (let j = 0; j < items.length; j++) {
      items[j].addEventListener("click", function (event) {
        if (items[j].classList.contains("is-active") || reducedMotion) {
          return;
        }
        event.preventDefault();
        moveIndicator(tabs, items[j], true);
        setTimeout(function () {
          window.location.href = items[j].href;
        }, 180);
      });
    }

    window.addEventListener("resize", function () {
      moveIndicator(tabs, tabs.querySelector(".is-active"), false);
    });
  }
}

document.addEventListener("DOMContentLoaded", setUpTabs);
