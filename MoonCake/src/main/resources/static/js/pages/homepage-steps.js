function setActiveStep(section, stepNumber) {
  const steps = section.querySelectorAll("[data-step]");
  for (let i = 0; i < steps.length; i++) {
    const isActive = steps[i].dataset.step === stepNumber;
    steps[i].classList.toggle("is-active", isActive);
  }

  const images = section.querySelectorAll("[data-step-image]");
  for (let i = 0; i < images.length; i++) {
    const isActive = images[i].dataset.stepImage === stepNumber;
    images[i].classList.toggle("is-active", isActive);
  }
}

function setUpBoxSteps() {
  const section = document.querySelector("[data-box-steps]");
  if (section === null || !("IntersectionObserver" in window)) {
    return;
  }

  const observer = new IntersectionObserver(function (entries) {
    for (let i = 0; i < entries.length; i++) {
      if (entries[i].isIntersecting) {
        setActiveStep(section, entries[i].target.dataset.step);
      }
    }
  }, {
    rootMargin: "-45% 0px -45% 0px"
  });

  const steps = section.querySelectorAll("[data-step]");
  for (let i = 0; i < steps.length; i++) {
    observer.observe(steps[i]);
  }
}

document.addEventListener("DOMContentLoaded", setUpBoxSteps);
