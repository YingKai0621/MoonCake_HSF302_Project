(() => {
    document.querySelectorAll('.catalog-thumbnail img').forEach(image => {
        const showFallback = () => {
            image.hidden = true;
            const fallback = image.nextElementSibling;
            fallback.textContent = 'Không tải được ảnh';
            fallback.hidden = false;
        };
        image.addEventListener('error', showFallback);
        if (image.complete && image.naturalWidth === 0) showFallback();
    });
    const panels = [...document.querySelectorAll('.catalog-panel')];
    const links = [...document.querySelectorAll('.catalog-tabs a')];
    function showPanel() {
        const selected = panels.find(p => '#' + p.id === location.hash) || panels[0];
        panels.forEach(p => { p.hidden = p !== selected; });
        links.forEach(a => {
            if (a.hash === '#' + selected.id) a.setAttribute('aria-current', 'page');
            else a.removeAttribute('aria-current');
        });
    }
    window.addEventListener('hashchange', showPanel);
    showPanel();
    document.querySelectorAll('[data-editor]').forEach(button => {
        button.addEventListener('click', () => {
            document.getElementById(button.dataset.editor).showModal();
        });
    });
    document.querySelectorAll('[data-close]').forEach(button => {
        button.addEventListener('click', () => button.closest('dialog').close());
    });
    panels.forEach(panel => {
        const rows = [...panel.querySelectorAll('.catalog-row')];
        const normalize = value => value.toLocaleLowerCase('vi').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd');
        const filter = () => {
            const query = normalize(panel.querySelector('input[type="search"]').value.trim());
            rows.forEach(row => { row.hidden = !normalize(row.querySelector('.row-label').textContent).includes(query); });
            panel.querySelector('.catalog-empty').hidden = rows.some(row => !row.hidden);
        };
        panel.querySelector('input[type="search"]').addEventListener('input', filter);
        filter();
    });
})();
