const REDUCED_MOTION = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
const NUMBER_FORMAT = new Intl.NumberFormat("vi-VN");

function setUpNumberTickers() {
  const numbers = document.querySelectorAll("[data-count-to]");
  for (let i = 0; i < numbers.length; i++) {
    const element = numbers[i];
    const target = Number(element.dataset.countTo);
    if (REDUCED_MOTION || target === 0) {
      element.textContent = NUMBER_FORMAT.format(target);
      continue;
    }
    const duration = 900;
    const startTime = performance.now();
    element.textContent = "0";

    function step(now) {
      const progress = Math.min(1, (now - startTime) / duration);
      const eased = 1 - Math.pow(1 - progress, 3);
      element.textContent = NUMBER_FORMAT.format(Math.round(target * eased));
      if (progress < 1) {
        requestAnimationFrame(step);
      }
    }
    requestAnimationFrame(step);
  }
}

function setUpTextGenerate() {
  const heading = document.querySelector("[data-text-generate]");
  if (heading === null || REDUCED_MOTION) {
    return;
  }
  const words = heading.textContent.trim().split(/\s+/);
  heading.textContent = "";
  for (let i = 0; i < words.length; i++) {
    const word = document.createElement("span");
    word.className = "text-generate__word";
    word.style.animationDelay = (i * 60) + "ms";
    word.textContent = words[i];
    heading.append(word);
    if (i < words.length - 1) {
      heading.append(" ");
    }
  }
}

function placePill(group, item, animate) {
  const pill = group.querySelector(".segmented__pill");
  if (pill === null || item === null) {
    return;
  }
  pill.style.transition = animate ? "" : "none";
  pill.style.width = item.offsetWidth + "px";
  pill.style.transform = "translateX(" + item.offsetLeft + "px)";
}

function setUpSegmentedControls() {
  const groups = document.querySelectorAll("[data-segmented]");
  for (let i = 0; i < groups.length; i++) {
    const group = groups[i];
    placePill(group, group.querySelector(".is-active"), false);
    const items = group.querySelectorAll(".segmented__item");
    for (let j = 0; j < items.length; j++) {
      items[j].addEventListener("click", function (event) {
        if (items[j].classList.contains("is-active") || REDUCED_MOTION) {
          return;
        }
        event.preventDefault();
        placePill(group, items[j], true);
        setTimeout(function () {
          window.location.href = items[j].href;
        }, 180);
      });
    }
    window.addEventListener("resize", function () {
      placePill(group, group.querySelector(".is-active"), false);
    });
  }
}

function setUpSpotlight() {
  const cards = document.querySelectorAll("[data-spotlight]");
  for (let i = 0; i < cards.length; i++) {
    cards[i].addEventListener("mousemove", function (event) {
      const box = cards[i].getBoundingClientRect();
      cards[i].style.setProperty("--spot-x", (event.clientX - box.left) + "px");
      cards[i].style.setProperty("--spot-y", (event.clientY - box.top) + "px");
    });
  }
}

let revenueChart = null;

function cssColor(name) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

function drawChart() {
  const canvas = document.getElementById("revenueChart");
  const data = window.dashboardChart;
  if (canvas === null || data === undefined || typeof Chart === "undefined") {
    return;
  }
  if (revenueChart !== null) {
    revenueChart.destroy();
  }

  const accent = cssColor("--accent");
  const text = cssColor("--text-muted");
  const grid = cssColor("--border");
  const info = cssColor("--info");

  revenueChart = new Chart(canvas, {
    data: {
      labels: data.labels,
      datasets: [
        {
          type: "bar",
          label: "Doanh thu (đ)",
          data: data.revenue,
          backgroundColor: accent,
          borderRadius: 6,
          maxBarThickness: 36,
          order: 2,
          yAxisID: "money"
        },
        {
          type: "line",
          label: "Số đơn",
          data: data.orders,
          borderColor: info,
          backgroundColor: info,
          cubicInterpolationMode: "monotone",
          pointRadius: 4,
          pointHoverRadius: 6,
          pointBorderColor: "#fff",
          pointBorderWidth: 2,
          order: 1,
          yAxisID: "count"
        }
      ]
    },
    options: {
      maintainAspectRatio: false,
      animation: REDUCED_MOTION ? false : { duration: 600, easing: "easeOutQuart" },
      interaction: { mode: "index", intersect: false },
      plugins: {
        legend: { labels: { color: text, usePointStyle: true } },
        tooltip: {
          callbacks: {
            label: function (context) {
              const value = NUMBER_FORMAT.format(context.parsed.y);
              return context.dataset.label + ": " + value;
            }
          }
        }
      },
      scales: {
        x: { ticks: { color: text }, grid: { display: false } },
        money: {
          position: "left",
          beginAtZero: true,
          ticks: {
            color: text,
            callback: function (value) {
              return NUMBER_FORMAT.format(value);
            }
          },
          grid: { color: grid }
        },
        count: {
          position: "right",
          beginAtZero: true,
          ticks: { color: text, precision: 0 },
          grid: { display: false }
        }
      }
    }
  });
}

function setUpDashboard() {
  setUpTextGenerate();
  setUpNumberTickers();
  setUpSegmentedControls();
  setUpSpotlight();
  drawChart();
  document.addEventListener("admin-theme-change", drawChart);
}

document.addEventListener("DOMContentLoaded", setUpDashboard);
