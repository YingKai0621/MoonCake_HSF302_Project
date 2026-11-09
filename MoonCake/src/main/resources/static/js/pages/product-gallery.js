function setUpProductGallery() {
  const mainImage = document.getElementById("mainImageDisplay");
  const allThumbnails = Array.from(document.querySelectorAll("[data-gallery-src]"));
  if (mainImage === null || allThumbnails.length === 0) {
    return;
  }

  const frame = document.querySelector("[data-gallery-frame]");
  const counter = document.querySelector("[data-gallery-counter]");
  const previousArrow = document.querySelector("[data-gallery-prev]");
  const nextArrow = document.querySelector("[data-gallery-next]");
  let thumbnails = allThumbnails;
  let currentIndex = 0;

  function photosOf(variantId) {
    const photos = [];
    for (let i = 0; i < allThumbnails.length; i++) {
      if (allThumbnails[i].dataset.galleryVariant === String(variantId)) {
        photos.push(allThumbnails[i]);
      }
    }
    return photos;
  }

  function showVariant(variantId) {
    let photos = photosOf(variantId);
    if (photos.length === 0) {
      photos = photosOf(allThumbnails[0].dataset.galleryVariant);
    }
    thumbnails = photos;
    for (let i = 0; i < allThumbnails.length; i++) {
      allThumbnails[i].hidden = !photos.includes(allThumbnails[i]);
    }
    const several = photos.length > 1;
    if (previousArrow !== null) {
      previousArrow.hidden = !several;
    }
    if (nextArrow !== null) {
      nextArrow.hidden = !several;
    }
    if (counter !== null) {
      counter.hidden = !several;
    }
    showImage(0);
  }

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

    for (let i = 0; i < allThumbnails.length; i++) {
      const active = allThumbnails[i] === thumbnail;
      allThumbnails[i].classList.toggle("is-active", active);
      allThumbnails[i].setAttribute("aria-pressed", String(active));
    }
    const strip = thumbnail.parentElement;
    const left = thumbnail.offsetLeft - strip.offsetLeft;
    if (left < strip.scrollLeft || left + thumbnail.offsetWidth > strip.scrollLeft + strip.clientWidth) {
      strip.scrollTo({ left: left - 8, behavior: "smooth" });
    }

    if (counter !== null) {
      counter.textContent = (currentIndex + 1) + " / " + thumbnails.length;
    }
  }

  for (let i = 0; i < allThumbnails.length; i++) {
    allThumbnails[i].addEventListener("click", function () {
      showImage(thumbnails.indexOf(allThumbnails[i]));
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

  for (let i = 0; i < allThumbnails.length; i++) {
    const preload = new Image();
    preload.src = allThumbnails[i].dataset.gallerySrc;
  }

  document.addEventListener("variant-change", function (event) {
    showVariant(event.detail.variantId);
  });

  const select = document.getElementById("variantSelect");
  if (select !== null && select.value !== "") {
    showVariant(select.value);
  } else {
    allThumbnails[0].classList.add("is-active");
    allThumbnails[0].setAttribute("aria-pressed", "true");
  }
}

document.addEventListener("DOMContentLoaded", setUpProductGallery);
