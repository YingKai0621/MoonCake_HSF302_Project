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

function setUpImagePreview() {
  const input = document.querySelector("[data-image-input]");
  const preview = document.querySelector("[data-image-preview]");
  if (input === null || preview === null) {
    return;
  }
  const image = preview.querySelector("img");
  const hint = preview.querySelector("span");

  function showHint(text) {
    image.hidden = true;
    hint.hidden = false;
    hint.textContent = text;
  }

  image.addEventListener("load", function () {
    image.hidden = false;
    hint.hidden = true;
  });
  image.addEventListener("error", function () {
    showHint("Không tải được ảnh, hãy kiểm tra lại đường dẫn");
  });
  input.addEventListener("input", function () {
    const url = input.value.trim();
    if (url === "") {
      showHint("Dán đường dẫn ảnh để xem trước");
      return;
    }
    image.src = url;
  });
}

document.addEventListener("DOMContentLoaded", function () {
  setUpImagePreview();
  setUpSlugFields();
  openSectionFromHash();
  setUpBrokenImages();
});
