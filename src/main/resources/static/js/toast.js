function showToast(message, type = 'error') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    // Normalise boolean arguments from older script calls to string types
    if (type === true) type = 'success';
    if (type === false) type = 'error';

    const toast = document.createElement('div');
    toast.className = `toast ${type}`; 

    const icons = {
        success: '✅',
        error: '❌',
        warning: '⚠️',
        info: 'ℹ️'
    };

    // Grab matching icon or default to info
    const icon = icons[type] || icons['info'];

    // Inject text and structure
    toast.innerHTML = `
        <span style="margin-right: 8px;">${icon}</span>
        <span>${message}</span>
    `;

    // Append to container
    container.appendChild(toast);

    // Auto-remove element after 4 seconds
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transition = 'opacity 0.5s ease';
        setTimeout(() => toast.remove(), 500);
    }, 4000);
}
