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

const MAX_IMAGE_BYTES = 5 * 1024 * 1024;
const IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];

function setUpDropzone(zone) {
  const input = zone.querySelector("[data-dropzone-input]");
  const preview = zone.querySelector(".dropzone__preview");
  const previewImage = preview.querySelector("img");
  const empty = zone.querySelector(".dropzone__empty");
  const error = zone.querySelector("[data-dropzone-error]");

  function showError(text) {
    input.value = "";
    preview.hidden = true;
    empty.hidden = false;
    error.textContent = text;
    error.hidden = false;
  }

  function showFile() {
    const file = input.files[0];
    if (file === undefined) {
      preview.hidden = true;
      empty.hidden = false;
      return;
    }
    if (!IMAGE_TYPES.includes(file.type)) {
      showError("Chỉ nhận ảnh JPG, PNG hoặc WebP.");
      return;
    }
    if (file.size > MAX_IMAGE_BYTES) {
      showError("Ảnh quá lớn. Hãy chọn ảnh tối đa 5MB.");
      return;
    }
    error.hidden = true;
    previewImage.src = URL.createObjectURL(file);
    preview.hidden = false;
    empty.hidden = true;
  }

  input.addEventListener("change", showFile);

  zone.addEventListener("dragover", function (event) {
    event.preventDefault();
    zone.classList.add("is-dragging");
  });
  zone.addEventListener("dragleave", function () {
    zone.classList.remove("is-dragging");
  });
  zone.addEventListener("drop", function (event) {
    event.preventDefault();
    zone.classList.remove("is-dragging");
    if (event.dataTransfer.files.length > 0) {
      input.files = event.dataTransfer.files;
      showFile();
    }
  });
}

function setUpDropzones() {
  const zones = document.querySelectorAll("[data-dropzone]");
  for (let i = 0; i < zones.length; i++) {
    setUpDropzone(zones[i]);
  }
}

function setUpCakeOnlyFields() {
  const typeSelect = document.querySelector('select[name="productType"]');
  const cakeFields = document.querySelectorAll("[data-cake-only]");
  if (typeSelect === null || cakeFields.length === 0) {
    return;
  }

  function update() {
    const isCake = typeSelect.value === "MOONCAKE";
    for (let i = 0; i < cakeFields.length; i++) {
      const wrapper = cakeFields[i];
      const input = wrapper.querySelector("input");
      wrapper.hidden = !isCake;
      input.required = isCake;
      input.disabled = !isCake;
    }
  }

  typeSelect.addEventListener("change", update);
  update();
}

function setUpUppercaseFields() {
  const fields = document.querySelectorAll("[data-uppercase]");
  for (let i = 0; i < fields.length; i++) {
    const field = fields[i];
    field.addEventListener("input", function () {
      const start = field.selectionStart;
      field.value = field.value.toUpperCase();
      field.setSelectionRange(start, start);
    });
  }
}

document.addEventListener("DOMContentLoaded", function () {
  setUpCakeOnlyFields();
  setUpUppercaseFields();
  setUpDropzones();
  setUpSlugFields();
  openSectionFromHash();
  setUpBrokenImages();
});
