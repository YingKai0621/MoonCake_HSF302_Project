const CELEBRATE_COLORS = ["#EDC565", "#F5D68E", "#D1A85A", "#F8F4EB"];

function fireFromSide(originX, angle) {
  window.confetti({
    particleCount: 70,
    angle: angle,
    spread: 60,
    startVelocity: 55,
    origin: { x: originX, y: 0.75 },
    colors: CELEBRATE_COLORS,
    disableForReducedMotion: true
  });
}

function celebrate() {
  if (typeof window.confetti !== "function") {
    return;
  }
  fireFromSide(0, 60);
  fireFromSide(1, 120);
  setTimeout(function () {
    fireFromSide(0.15, 70);
    fireFromSide(0.85, 110);
  }, 250);
}

document.addEventListener("DOMContentLoaded", celebrate);
