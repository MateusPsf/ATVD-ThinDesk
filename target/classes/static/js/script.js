// Alternar sidebar em telas pequenas
document.querySelector('.toggle-btn')?.addEventListener('click', (event) => {
    const sidebar = document.getElementById('sidebar');
    if (!sidebar) return;
    const aberto = sidebar.classList.toggle('active');
    event.currentTarget.setAttribute('aria-expanded', String(aberto));
});

// Alternar submenu
function toggleSubmenu(event, element) {
    event.preventDefault();
    element.parentElement.classList.toggle('open');
}
