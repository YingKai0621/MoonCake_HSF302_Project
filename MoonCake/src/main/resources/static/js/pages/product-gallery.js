function setUpProductGallery() {
  const mainImage = document.getElementById("mainImageDisplay");
  const thumbnails = Array.from(document.querySelectorAll("[data-gallery-src]"));
  if (mainImage === null || thumbnails.length === 0) {
    return;
  }

  const frame = document.querySelector("[data-gallery-frame]");
  const counter = document.querySelector("[data-gallery-counter]");
  let currentIndex = 0;

  function showImage(index) {
    currentIndex = (index + thumbnails.length) % thumbnails.length;
    const thumbnail = thumbnails[currentIndex];

    const nextSource = thumbnail.dataset.gallerySrc;
    const nextAlt = thumbnail.dataset.galleryAlt || "Ảnh sản phẩm";
    const loader = new Image();
    loader.onload = function () {
      if (thumbnails[currentIndex].dataset.gallerySrc !== nextSource) {
        return;
      }
      mainImage.src = nextSource;
      mainImage.alt = nextAlt;
      mainImage.classList.remove("is-switching");
    };
    mainImage.classList.add("is-switching");
    loader.src = nextSource;

    for (let i = 0; i < thumbnails.length; i++) {
      const active = i === currentIndex;
      thumbnails[i].classList.toggle("is-active", active);
      thumbnails[i].setAttribute("aria-pressed", String(active));
    }
    thumbnail.scrollIntoView({ block: "nearest", inline: "nearest" });

    if (counter !== null) {
      counter.textContent = (currentIndex + 1) + " / " + thumbnails.length;
    }
  }

  for (let i = 0; i < thumbnails.length; i++) {
    thumbnails[i].addEventListener("click", function () {
      showImage(i);
    });
  }

  const previousButton = document.querySelector("[data-gallery-prev]");
  const nextButton = document.querySelector("[data-gallery-next]");
  if (previousButton !== null) {
    previousButton.addEventListener("click", function () {
      showImage(currentIndex - 1);
    });
  }
  if (nextButton !== null) {
    nextButton.addEventListener("click", function () {
      showImage(currentIndex + 1);
    });
  }

  document.addEventListener("keydown", function (event) {
    const typing = event.target.closest("input, textarea, select, [contenteditable]");
    const lightbox = document.getElementById("lightboxModal");
    const lightboxOpen = lightbox !== null && !lightbox.classList.contains("hidden");
    if (typing !== null || lightboxOpen || thumbnails.length < 2) {
      return;
    }
    if (event.key === "ArrowLeft") {
      showImage(currentIndex - 1);
    }
    if (event.key === "ArrowRight") {
      showImage(currentIndex + 1);
    }
  });

  if (frame !== null && thumbnails.length > 1) {
    let touchStartX = 0;
    frame.addEventListener("touchstart", function (event) {
      touchStartX = event.changedTouches[0].clientX;
    }, { passive: true });
    frame.addEventListener("touchend", function (event) {
      const distance = event.changedTouches[0].clientX - touchStartX;
      if (distance > 40) {
        showImage(currentIndex - 1);
      }
      if (distance < -40) {
        showImage(currentIndex + 1);
      }
    }, { passive: true });
  }

  for (let i = 0; i < thumbnails.length; i++) {
    const preload = new Image();
    preload.src = thumbnails[i].dataset.gallerySrc;
  }

  thumbnails[0].classList.add("is-active");
  thumbnails[0].setAttribute("aria-pressed", "true");
}

document.addEventListener("DOMContentLoaded", setUpProductGallery);
