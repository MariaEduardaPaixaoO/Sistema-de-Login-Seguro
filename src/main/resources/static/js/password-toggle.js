
document.addEventListener('DOMContentLoaded', function () {
    'use strict';

    document.querySelectorAll('[data-password-toggle]').forEach(function (button) {
        const inputId = button.dataset.passwordToggle;
        const passwordInput = document.getElementById(inputId);

        if (!passwordInput) {
            return;
        }

        button.addEventListener('click', function () {
            const showPassword = passwordInput.type === 'password';

            passwordInput.type = showPassword ? 'text' : 'password';
            button.setAttribute('aria-pressed', String(showPassword));
            button.setAttribute(
                'aria-label',
                showPassword ? 'Ocultar senha' : 'Mostrar senha'
            );
        });
    });
});