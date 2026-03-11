(function () {
    "use strict";

    const API = {
        LOGIN: "/api/v1/auth/login",
        REGISTER: "/api/v1/auth/register",
    };

    // --- DOM refs ---
    const tabs = document.querySelectorAll(".tab");
    const loginForm = document.getElementById("login-form");
    const registerForm = document.getElementById("register-form");
    const messageEl = document.getElementById("message");
    const tokenResult = document.getElementById("token-result");
    const accessTokenEl = document.getElementById("access-token");
    const refreshTokenEl = document.getElementById("refresh-token");
    const tokenTypeEl = document.getElementById("token-type");

    // --- Tab switching ---
    tabs.forEach(function (tab) {
        tab.addEventListener("click", function () {
            var target = tab.dataset.tab;
            tabs.forEach(function (t) { t.classList.remove("active"); });
            tab.classList.add("active");

            loginForm.classList.toggle("hidden", target !== "login");
            registerForm.classList.toggle("hidden", target !== "register");

            clearMessages();
            clearFieldErrors(loginForm);
            clearFieldErrors(registerForm);
        });
    });

    // --- Login ---
    loginForm.addEventListener("submit", function (e) {
        e.preventDefault();
        clearMessages();
        clearFieldErrors(loginForm);

        var data = {
            email: val("login-email"),
            password: val("login-password"),
        };

        submitForm(API.LOGIN, data, loginForm, function (json) {
            showTokens(json);
            showMessage("Login successful.", "success");
        });
    });

    // --- Register ---
    registerForm.addEventListener("submit", function (e) {
        e.preventDefault();
        clearMessages();
        clearFieldErrors(registerForm);

        var data = {
            firstName: val("reg-firstName"),
            lastName: val("reg-lastName"),
            email: val("reg-email"),
            phoneNumber: val("reg-phoneNumber"),
            password: val("reg-password"),
            confirmPassword: val("reg-confirmPassword"),
        };

        // Client-side password match check
        if (data.password !== data.confirmPassword) {
            showFieldError(registerForm, "confirmPassword", "Passwords do not match.");
            return;
        }

        submitForm(API.REGISTER, data, registerForm, function () {
            showMessage("Registration successful! You can now log in.", "success");
            // Switch to login tab after short delay
            setTimeout(function () {
                tabs.forEach(function (t) {
                    t.classList.toggle("active", t.dataset.tab === "login");
                });
                registerForm.classList.add("hidden");
                loginForm.classList.remove("hidden");
            }, 1500);
        });
    });

    // --- API call ---
    function submitForm(url, data, form, onSuccess) {
        var btn = form.querySelector(".btn");
        btn.disabled = true;

        fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data),
        })
            .then(function (res) {
                if (res.status === 201) {
                    onSuccess(null);
                    return;
                }
                return res.json().then(function (json) {
                    if (res.ok) {
                        onSuccess(json);
                    } else {
                        handleError(json, form);
                    }
                });
            })
            .catch(function () {
                showMessage("Network error. Is the server running?", "error");
            })
            .finally(function () {
                btn.disabled = false;
            });
    }

    // --- Error handling ---
    function handleError(err, form) {
        // Backend returns { message, code, validationErrorList }
        if (err.validationErrorList && err.validationErrorList.length > 0) {
            err.validationErrorList.forEach(function (ve) {
                showFieldError(form, ve.field, ve.message);
            });
        }

        var msg = err.message || "An unexpected error occurred.";
        showMessage(msg, "error");
    }

    function showFieldError(form, fieldName, message) {
        var input = form.querySelector('[name="' + fieldName + '"]');
        if (!input) return;
        input.classList.add("input-error");
        var errorSpan = input.parentElement.querySelector(".field-error");
        if (errorSpan) errorSpan.textContent = message;
    }

    function clearFieldErrors(form) {
        form.querySelectorAll(".input-error").forEach(function (el) {
            el.classList.remove("input-error");
        });
        form.querySelectorAll(".field-error").forEach(function (el) {
            el.textContent = "";
        });
    }

    // --- Messages ---
    function showMessage(text, type) {
        messageEl.textContent = text;
        messageEl.className = "message " + type;
        messageEl.classList.remove("hidden");
    }

    function clearMessages() {
        messageEl.classList.add("hidden");
        messageEl.className = "message hidden";
        messageEl.textContent = "";
        tokenResult.classList.add("hidden");
    }

    // --- Tokens ---
    function showTokens(json) {
        accessTokenEl.value = json.access_token || "";
        refreshTokenEl.value = json.refresh_token || "";
        tokenTypeEl.value = json.token_type || "";
        tokenResult.classList.remove("hidden");
    }

    // --- Helpers ---
    function val(id) {
        return document.getElementById(id).value.trim();
    }
})();
