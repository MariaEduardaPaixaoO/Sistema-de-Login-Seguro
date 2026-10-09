// Validação de conveniência no navegador: avisa quando a confirmação de senha não confere.
// O servidor valida tudo novamente; este script nunca é a única barreira.
(function () {
    'use strict';

    var password = document.getElementById('password');
    var confirmation = document.getElementById('confirmPassword');
    if (!password || !confirmation) {
        return;
    }

    function checkMatch() {
        var mismatch = confirmation.value !== '' && confirmation.value !== password.value;
        confirmation.setCustomValidity(mismatch ? 'As senhas não conferem.' : '');
    }

    password.addEventListener('input', checkMatch);
    confirmation.addEventListener('input', checkMatch);
})();
