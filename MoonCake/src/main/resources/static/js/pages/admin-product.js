function toSlug(text) {
  return text
    .toLocaleLowerCase("vi")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/đ/g, "d")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

function setUpSlugFields() {
  const sources = document.querySelectorAll("[data-slug-source]");
  for (let i = 0; i < sources.length; i++) {
    const source = sources[i];
    const target = source.form.querySelector("[data-slug-target]");
    if (target === null) {
      continue;
    }
    let followName = target.value === "";
    target.addEventListener("input", function () {
      followName = false;
    });
    source.addEventListener("input", function () {
      if (followName) {
        target.value = toSlug(source.value);
      }
    });
  }
}

function openSectionFromHash() {
  if (location.hash === "") {
    return;
  }
  const target = document.getElementById(location.hash.substring(1));
  if (target !== null && target.tagName === "DETAILS") {
    target.open = true;
  }
}

function setUpBrokenImages() {
  const images = document.querySelectorAll(".product-thumb img, .image-tile img");
  for (let i = 0; i < images.length; i++) {
    const image = images[i];
    image.addEventListener("error", function () {
      image.hidden = true;
      image.parentElement.classList.add("is-broken");
    });
    if (image.complete && image.naturalWidth === 0) {
      image.hidden = true;
      image.parentElement.classList.add("is-broken");
    }
  }
}

document.addEventListener("DOMContentLoaded", function () {
  setUpSlugFields();
  openSectionFromHash();
  setUpBrokenImages();
});
