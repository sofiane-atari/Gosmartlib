package edu.ap.bib_backend.service.mock;

import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.service.smartschool.SmartschoolOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping
@Profile("dev")
public class MockOAuthController {

    private static final Logger log = LoggerFactory.getLogger(MockOAuthController.class);

    private final MockSchoolRegistry registry;
    private final JwtService jwtService;
    private final TokenStorageService tokenStorageService;

    public MockOAuthController(MockSchoolRegistry registry, JwtService jwtService, TokenStorageService tokenStorageService) {
        this.registry = registry;
        this.jwtService = jwtService;
        this.tokenStorageService = tokenStorageService;
    }

    private static final String CSS = """
            *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

            :root {
                --main-color: #8a1e41;
                --main-color-hover: #6e1733;
                --sec-color: #c4a3a8;
                --main-light: #fff5f8;
                --sec-light: #ead3c1;
                --error-color: #ad4440;
                --success-color: #4c9a4c;
                --text-dark: #1a0a10;
                --text-muted: #7a5560;
                --border-color: #f0e4e8;
                --bg-surface: #ffffff;
                --font: 'calibri', 'Open Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            }

            html { font-size: 16px; }

            body {
                font-family: var(--font);
                background: linear-gradient(135deg, var(--main-light) 0%%, var(--sec-light) 100%%);
                min-height: 100vh;
                display: flex;
                align-items: center;
                justify-content: center;
                padding: 1.5rem;
                color: var(--text-dark);
            }

            .login-wrapper {
                display: flex;
                flex-direction: column;
                align-items: center;
                gap: 1.5rem;
                width: 100%%;
                max-width: 680px;
            }

            .login-eyebrow {
                color: var(--main-color);
                font-weight: 700;
                font-size: 0.85rem;
                letter-spacing: 0.08em;
                text-align: center;
            }

            .login-title {
                color: var(--main-color);
                font-size: clamp(1.6rem, 3vw, 2.2rem);
                font-weight: bold;
                text-align: center;
                line-height: 1.15;
            }

            .login-card {
                background: var(--bg-surface);
                border-radius: 16px;
                padding: 2rem 2.25rem;
                box-shadow: 0 8px 32px rgba(0,0,0,0.08);
                max-width: 480px;
                width: 100%%;
                border-top: 4px solid var(--main-color);
                position: relative;
            }

            .dev-badge {
                position: absolute;
                top: 12px;
                right: 12px;
                background: #ff9800;
                color: #fff;
                padding: 3px 10px;
                border-radius: 4px;
                font-size: 10px;
                font-weight: 700;
                letter-spacing: 0.5px;
                text-transform: uppercase;
            }

            /* Step indicator */
            .step-indicator {
                display: flex;
                align-items: center;
                justify-content: center;
                gap: 8px;
                margin-bottom: 1.5rem;
            }
            .step-dot {
                width: 10px;
                height: 10px;
                border-radius: 50%%;
                background: var(--border-color);
                transition: all 0.3s ease;
            }
            .step-dot.active {
                background: var(--main-color);
                transform: scale(1.2);
            }
            .step-line {
                width: 32px;
                height: 2px;
                background: var(--border-color);
            }

            /* Platform input */
            .platform-input-wrap {
                position: relative;
                margin-bottom: 1rem;
            }

            .platform-input {
                width: 100%%;
                padding: 14px 44px 14px 14px;
                border: 2px solid var(--border-color);
                border-radius: 8px;
                font-size: 1rem;
                font-family: var(--font);
                outline: none;
                transition: border-color 0.25s ease, box-shadow 0.25s ease;
                background: #fafafa;
            }
            .platform-input:focus {
                border-color: var(--main-color);
                background: #fff;
                box-shadow: 0 0 0 3px rgba(138,30,65,0.1);
            }
            .platform-input.valid {
                border-color: var(--success-color);
            }
            .platform-input.invalid {
                border-color: var(--error-color);
            }

            .platform-check {
                position: absolute;
                right: 14px;
                top: 16px;
                width: 20px;
                height: 20px;
                display: flex;
                align-items: center;
                justify-content: center;
                opacity: 0;
                transition: opacity 0.2s ease;
                pointer-events: none;
            }
            .platform-check.visible { opacity: 1; }
            .platform-check.valid svg { color: var(--success-color); }
            .platform-check.invalid svg { color: var(--error-color); }

            .platform-hint {
                font-size: 0.82rem;
                color: var(--text-muted);
                margin-bottom: 1.25rem;
                min-height: 1.2em;
                transition: color 0.2s;
            }
            .platform-hint.error { color: var(--error-color); }
            .platform-hint.success { color: var(--success-color); }

            /* Autocomplete dropdown */
            .autocomplete-dropdown {
                position: absolute;
                top: calc(100%% + 4px);
                left: 0;
                right: 0;
                background: #fff;
                border: 1px solid var(--border-color);
                border-radius: 8px;
                box-shadow: 0 8px 24px rgba(0,0,0,0.12);
                max-height: 200px;
                overflow-y: auto;
                z-index: 100;
                display: none;
            }
            .autocomplete-dropdown.open { display: block; }
            .autocomplete-item {
                padding: 10px 14px;
                cursor: pointer;
                display: flex;
                flex-direction: column;
                gap: 2px;
                transition: background 0.15s;
                border-bottom: 1px solid var(--border-color);
            }
            .autocomplete-item:last-child { border-bottom: none; }
            .autocomplete-item:hover, .autocomplete-item.highlighted {
                background: var(--main-light);
            }
            .autocomplete-item__name {
                font-size: 0.9rem;
                font-weight: 600;
                color: var(--text-dark);
            }
            .autocomplete-item__domain {
                font-size: 0.78rem;
                color: var(--text-muted);
            }
            .autocomplete-item mark {
                background: rgba(138,30,65,0.15);
                color: var(--main-color);
                border-radius: 2px;
                padding: 0 1px;
            }

            /* Buttons */
            .btn-primary {
                width: 100%%;
                padding: 14px;
                background: var(--main-color);
                color: #fff;
                border: none;
                border-radius: 8px;
                font-size: 1rem;
                font-weight: 600;
                font-family: var(--font);
                cursor: pointer;
                transition: all 0.3s ease;
                box-shadow: 0 4px 12px rgba(138,30,65,0.2);
            }
            .btn-primary:hover:not(:disabled) {
                background: var(--main-color-hover);
                box-shadow: 0 6px 20px rgba(138,30,65,0.3);
                transform: translateY(-2px);
            }
            .btn-primary:active:not(:disabled) {
                transform: translateY(0);
                box-shadow: 0 2px 8px rgba(138,30,65,0.2);
            }
            .btn-primary:disabled {
                opacity: 0.5;
                cursor: not-allowed;
                transform: none;
                box-shadow: none;
            }

            /* Login form */
            .login-school-header {
                display: flex;
                align-items: center;
                gap: 12px;
                padding-bottom: 16px;
                margin-bottom: 20px;
                border-bottom: 1px solid var(--border-color);
            }
            .login-school-logo {
                width: 44px;
                height: 44px;
                border-radius: 8px;
                background: var(--main-color);
                display: flex;
                align-items: center;
                justify-content: center;
                flex-shrink: 0;
            }
            .login-school-logo span {
                color: #fff;
                font-size: 16px;
                font-weight: 700;
            }
            .login-school-info { flex: 1; }
            .login-school-name {
                font-size: 0.95rem;
                font-weight: 600;
                color: var(--text-dark);
            }
            .login-school-domain {
                font-size: 0.8rem;
                color: var(--text-muted);
            }
            .login-school-change {
                font-size: 0.8rem;
                color: var(--main-color);
                text-decoration: none;
                font-weight: 600;
                transition: color 0.2s;
                white-space: nowrap;
            }
            .login-school-change:hover { text-decoration: underline; }

            .form-group { margin-bottom: 16px; }

            .float-label {
                position: relative;
            }
            .float-label__input {
                width: 100%%;
                padding: 18px 14px 6px;
                border: 2px solid var(--border-color);
                border-radius: 8px;
                font-size: 1rem;
                font-family: var(--font);
                outline: none;
                transition: border-color 0.25s ease, box-shadow 0.25s ease;
                background: #fafafa;
            }
            .float-label__input::placeholder {
                color: transparent;
            }
            .float-label__input:focus {
                border-color: var(--main-color);
                background: #fff;
                box-shadow: 0 0 0 3px rgba(138,30,65,0.1);
            }
            .float-label label {
                position: absolute;
                left: 14px;
                top: 18px;
                color: var(--text-muted);
                font-size: 1rem;
                pointer-events: none;
                transition: all 0.2s ease;
                line-height: 1;
            }
            .float-label__input:focus + label,
            .float-label__input:not(:placeholder-shown) + label {
                top: 8px;
                font-size: 0.7rem;
                color: var(--main-color);
                font-weight: 600;
            }
            .float-label--password .float-label__input { padding-right: 48px; }

            .toggle-password {
                position: absolute;
                right: 10px;
                top: 16px;
                background: none;
                border: none;
                cursor: pointer;
                color: var(--text-muted);
                padding: 4px;
                display: flex;
                align-items: center;
                transition: color 0.2s;
            }
            .toggle-password:hover { color: var(--text-dark); }

            .error-message {
                background: #fff3f3;
                border: 1px solid #fca5a5;
                color: var(--error-color);
                padding: 12px 14px;
                border-radius: 8px;
                font-size: 0.88rem;
                margin-bottom: 16px;
                text-align: center;
            }

            .credentials-panel {
                margin-top: 20px;
                border-top: 1px solid var(--border-color);
                padding-top: 16px;
            }
            .credentials-panel details {
                border: 1px solid var(--border-color);
                border-radius: 8px;
                background: #fafafa;
                overflow: hidden;
            }
            .credentials-panel summary {
                padding: 10px 14px;
                cursor: pointer;
                font-size: 0.85rem;
                font-weight: 600;
                color: var(--text-muted);
                user-select: none;
                transition: background 0.15s;
            }
            .credentials-panel summary:hover { background: var(--border-color); }
            .credentials-table { padding: 0 14px 14px; }
            .credentials-table table {
                width: 100%%;
                border-collapse: collapse;
                font-size: 0.78rem;
            }
            .credentials-table th {
                text-align: left;
                padding: 6px 8px;
                color: var(--text-muted);
                font-weight: 600;
                border-bottom: 1px solid var(--border-color);
            }
            .credentials-table td {
                padding: 6px 8px;
                border-bottom: 1px solid var(--border-color);
            }
            .credentials-table code {
                background: var(--border-color);
                padding: 2px 6px;
                border-radius: 3px;
                font-size: 0.75rem;
            }
            .role-badge {
                padding: 2px 8px;
                border-radius: 3px;
                font-size: 0.72rem;
                font-weight: 600;
            }
            .role-leerling { background: #e3f2fd; color: #1565c0; }
            .role-leerkracht { background: #e8f5e9; color: #2e7d32; }
            .role-beheerder { background: #fff3e0; color: #e65100; }

            .login-footer {
                text-align: center;
                padding: 0.5rem 1rem 0;
                max-width: 680px;
            }
            .login-footer p {
                color: #666;
                font-size: 0.8rem;
                line-height: 1.5;
            }

            @media (max-width: 600px) {
                body { padding: 1rem; }
                .login-card { padding: 1.5rem 1.25rem; border-radius: 14px; }
                .login-title { font-size: 1.4rem; }
                .login-school-header { flex-wrap: wrap; }
                .login-school-change { position: static; margin-top: 4px; }
            }
            """;

    private String buildHtml(String eyebrow, String title, String body) {
        return """
                <!DOCTYPE html>
                <html lang="nl">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>%s - GoSmartLib</title>
                    <style>%s</style>
                </head>
                <body>
                    <div class="login-wrapper">
                        <div class="login-eyebrow">%s</div>
                        <div class="login-title">%s</div>
                        <div class="login-card">
                            <span class="dev-badge">DEV</span>
                            %s
                        </div>
                        <div class="login-footer">
                            <p>GoSmartLib &mdash; Ontwikkelsomgeving</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(title, CSS, eyebrow, title, body);
    }

    // ===== STEP 1: Platform Chooser (domain input with autocomplete) =====
    @GetMapping(value = {"/OAuth", "/OAuth/index/authorize", "/OAuth/index/authorize/"})
    public void showPlatformChooser(
            @RequestParam(required = false) String redirect_uri,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String response_type,
            @RequestParam(required = false) String client_id,
            @RequestParam(required = false) String scope,
            HttpServletResponse response
    ) throws IOException {
        var schools = registry.getAllSchools();
        String schoolsJson = schools.stream()
                .map(s -> "{\"name\":\"" + escapeJson(s.getSchoolName()) + "\",\"domain\":\"" + escapeJson(s.getDomainName()) + "\",\"code\":\"" + escapeJson(s.getSchoolCode()) + "\"}")
                .collect(Collectors.joining(",", "[", "]"));

        String body = """
                <div class="step-indicator">
                    <div class="step-dot active"></div>
                    <div class="step-line"></div>
                    <div class="step-dot"></div>
                </div>

                <form method="POST" action="/api/OAuth/index/authorize" id="platformForm">
                    <input type="hidden" name="redirect_uri" value="%s">
                    <input type="hidden" name="state" value="%s">
                    <input type="hidden" name="response_type" value="%s">
                    <input type="hidden" name="client_id" value="%s">
                    <input type="hidden" name="scope" value="%s">
                    <input type="hidden" name="school" id="selectedSchoolCode">

                    <div class="platform-input-wrap">
                        <input type="text" id="platformInput" class="platform-input"
                               placeholder="mijnschool.smartschool.be"
                               autocomplete="off" required>
                        <div class="platform-check" id="platformCheck">
                            <svg class="check-valid" width="20" height="20" viewBox="0 0 24 24" style="display:none"><path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
                            <svg class="check-invalid" width="20" height="20" viewBox="0 0 24 24" style="display:none"><path fill="currentColor" d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
                        </div>
                        <div class="autocomplete-dropdown" id="autocompleteDropdown"></div>
                    </div>
                    <div class="platform-hint" id="platformHint">Voer het Smartschool-adres van je school in</div>

                    <button type="submit" class="btn-primary" id="platformSubmit" disabled>Volgende</button>
                </form>

                <script>
                (function() {
                    var schools = %s;
                    var input = document.getElementById('platformInput');
                    var dropdown = document.getElementById('autocompleteDropdown');
                    var hint = document.getElementById('platformHint');
                    var check = document.getElementById('platformCheck');
                    var submit = document.getElementById('platformSubmit');
                    var schoolCodeInput = document.getElementById('selectedSchoolCode');
                    var highlightedIndex = -1;
                    var filteredSchools = [];

                    function escapeHtml(s) {
                        return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
                    }

                    function highlightMatch(text, query) {
                        if (!query) return escapeHtml(text);
                        var idx = text.toLowerCase().indexOf(query.toLowerCase());
                        if (idx === -1) return escapeHtml(text);
                        return escapeHtml(text.substring(0, idx)) + '<mark>' + escapeHtml(text.substring(idx, idx + query.length)) + '</mark>' + escapeHtml(text.substring(idx + query.length));
                    }

                    function renderDropdown(query) {
                        if (!query) { dropdown.classList.remove('open'); return; }
                        filteredSchools = schools.filter(function(s) {
                            return s.name.toLowerCase().indexOf(query.toLowerCase()) !== -1 ||
                                   s.domain.toLowerCase().indexOf(query.toLowerCase()) !== -1;
                        });
                        if (filteredSchools.length === 0) { dropdown.classList.remove('open'); return; }
                        dropdown.innerHTML = filteredSchools.map(function(s, i) {
                            return '<div class="autocomplete-item" data-index="' + i + '" data-code="' + s.code + '">' +
                                   '<span class="autocomplete-item__name">' + highlightMatch(s.name, query) + '</span>' +
                                   '<span class="autocomplete-item__domain">' + highlightMatch(s.domain, query) + '</span>' +
                                   '</div>';
                        }).join('');
                        dropdown.classList.add('open');
                        highlightedIndex = -1;
                        dropdown.querySelectorAll('.autocomplete-item').forEach(function(el) {
                            el.addEventListener('mousedown', function(e) {
                                e.preventDefault();
                                selectSchool(filteredSchools[parseInt(el.dataset.index)]);
                            });
                        });
                    }

                    function selectSchool(school) {
                        input.value = school.domain;
                        input.classList.add('valid');
                        input.classList.remove('invalid');
                        check.classList.add('visible', 'valid');
                        check.classList.remove('invalid');
                        check.querySelector('.check-valid').style.display = '';
                        check.querySelector('.check-invalid').style.display = 'none';
                        hint.textContent = school.name;
                        hint.className = 'platform-hint success';
                        submit.disabled = false;
                        schoolCodeInput.value = school.code;
                        dropdown.classList.remove('open');
                    }

                    function validateInput() {
                        var val = input.value.trim().toLowerCase();
                        var match = schools.find(function(s) { return s.domain.toLowerCase() === val; });
                        if (match) {
                            selectSchool(match);
                        } else if (val.length > 0) {
                            input.classList.remove('valid');
                            input.classList.add('invalid');
                            check.classList.add('visible', 'invalid');
                            check.classList.remove('valid');
                            check.querySelector('.check-valid').style.display = 'none';
                            check.querySelector('.check-invalid').style.display = '';
                            hint.textContent = 'School niet gevonden';
                            hint.className = 'platform-hint error';
                            submit.disabled = true;
                            schoolCodeInput.value = '';
                        } else {
                            input.classList.remove('valid', 'invalid');
                            check.classList.remove('visible', 'valid', 'invalid');
                            check.querySelector('.check-valid').style.display = 'none';
                            check.querySelector('.check-invalid').style.display = 'none';
                            hint.textContent = 'Voer het Smartschool-adres van je school in';
                            hint.className = 'platform-hint';
                            submit.disabled = true;
                            schoolCodeInput.value = '';
                        }
                    }

                    input.addEventListener('input', function() {
                        renderDropdown(input.value.trim());
                        if (!dropdown.classList.contains('open')) validateInput();
                    });
                    input.addEventListener('focus', function() {
                        if (input.value.trim()) renderDropdown(input.value.trim());
                    });
                    input.addEventListener('keydown', function(e) {
                        var items = dropdown.querySelectorAll('.autocomplete-item');
                        if (e.key === 'ArrowDown') {
                            e.preventDefault();
                            highlightedIndex = Math.min(highlightedIndex + 1, items.length - 1);
                            items.forEach(function(el, i) { el.classList.toggle('highlighted', i === highlightedIndex); });
                            if (highlightedIndex >= 0) items[highlightedIndex].scrollIntoView({block:'nearest'});
                        } else if (e.key === 'ArrowUp') {
                            e.preventDefault();
                            highlightedIndex = Math.max(highlightedIndex - 1, 0);
                            items.forEach(function(el, i) { el.classList.toggle('highlighted', i === highlightedIndex); });
                            if (highlightedIndex >= 0) items[highlightedIndex].scrollIntoView({block:'nearest'});
                        } else if (e.key === 'Enter') {
                            if (highlightedIndex >= 0 && filteredSchools[highlightedIndex]) {
                                e.preventDefault();
                                selectSchool(filteredSchools[highlightedIndex]);
                            }
                        } else if (e.key === 'Escape') {
                            dropdown.classList.remove('open');
                        }
                    });
                    document.addEventListener('click', function(e) {
                        if (!e.target.closest('.platform-input-wrap')) dropdown.classList.remove('open');
                    });
                    input.addEventListener('blur', function() {
                        setTimeout(function() { dropdown.classList.remove('open'); validateInput(); }, 150);
                    });
                })();
                </script>
                """.formatted(
                redirect_uri != null ? redirect_uri : "",
                state != null ? state : "",
                response_type != null ? response_type : "",
                client_id != null ? client_id : "",
                scope != null ? scope : "",
                schoolsJson
        );

        String html = buildHtml("Meld je aan in GoSmartLib", "Vul het schooladres van je school in", body);
        response.setContentType("text/html; charset=utf-8");
        response.getWriter().write(html);
    }

    @PostMapping(value = {"/OAuth", "/OAuth/index/authorize", "/OAuth/index/authorize/"})
    public void handlePlatformSelection(
            @RequestParam String school,
            @RequestParam(required = false) String redirect_uri,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String response_type,
            @RequestParam(required = false) String client_id,
            @RequestParam(required = false) String scope,
            HttpServletResponse response
    ) throws IOException {
        registry.setActiveSchool(school);

        String query = new StringBuilder()
                .append("redirect_uri=").append(URLEncoder.encode(redirect_uri != null ? redirect_uri : "", StandardCharsets.UTF_8))
                .append("&state=").append(URLEncoder.encode(state != null ? state : "", StandardCharsets.UTF_8))
                .append("&response_type=").append(URLEncoder.encode(response_type != null ? response_type : "", StandardCharsets.UTF_8))
                .append("&client_id=").append(URLEncoder.encode(client_id != null ? client_id : "", StandardCharsets.UTF_8))
                .append("&scope=").append(URLEncoder.encode(scope != null ? scope : "", StandardCharsets.UTF_8))
                .append("&school=").append(URLEncoder.encode(school, StandardCharsets.UTF_8))
                .toString();

        response.sendRedirect("/api/OAuth/index/login?" + query);
    }

    // ===== STEP 2: Login Page =====
    @GetMapping(value = {"/OAuth/index/login", "/OAuth/index/login/"})
    public void showLoginForm(
            @RequestParam(required = false) String redirect_uri,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String response_type,
            @RequestParam(required = false) String client_id,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String school,
            @RequestParam(required = false) String error,
            HttpServletResponse response
    ) throws IOException {
        MockSchoolData active = registry.getActiveSchool();
        if (active == null) {
            response.sendRedirect("/api/OAuth/index/authorize");
            return;
        }

        String errorHtml = error != null && !error.isEmpty()
                ? "<div class=\"error-message\">" + escapeHtml(error) + "</div>"
                : "";

        String credentialsHtml = active.getUsers().stream()
                .map(u -> """
                        <tr>
                            <td><code>%s</code></td>
                            <td><code>%s</code></td>
                            <td><span class="role-badge role-%s">%s</span></td>
                        </tr>
                        """.formatted(u.username, u.password, u.basisrol.toLowerCase(), u.basisrol))
                .collect(Collectors.joining());

        String initials = active.getSchoolName().replaceAll("\\s+", "").substring(0, Math.min(2, active.getSchoolName().replaceAll("\\s+", "").length()));

        String body = """
                <div class="step-indicator">
                    <div class="step-dot"></div>
                    <div class="step-line"></div>
                    <div class="step-dot active"></div>
                </div>

                %s

                <div class="login-school-header">
                    <div class="login-school-logo"><span>%s</span></div>
                    <div class="login-school-info">
                        <div class="login-school-name">%s</div>
                        <div class="login-school-domain">%s</div>
                    </div>
                    <a href="/api/OAuth/index/authorize?redirect_uri=%s&state=%s" class="login-school-change">Wijzig</a>
                </div>

                <form method="POST" action="/api/OAuth/index/login">
                    <input type="hidden" name="redirect_uri" value="%s">
                    <input type="hidden" name="state" value="%s">

                    <div class="form-group">
                        <div class="float-label">
                            <input type="text" id="username" name="username" required class="float-label__input" placeholder=" " autocomplete="username">
                            <label for="username">Gebruikersnaam</label>
                        </div>
                    </div>

                    <div class="form-group">
                        <div class="float-label float-label--password">
                            <input type="password" id="password" name="password" required class="float-label__input" placeholder=" " autocomplete="current-password">
                            <label for="password">Wachtwoord</label>
                            <button type="button" class="toggle-password" onclick="togglePwd()" title="Wachtwoord tonen">
                                <svg viewBox="0 0 24 24" width="20" height="20"><path fill="currentColor" d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/></svg>
                            </button>
                        </div>
                    </div>

                    <button type="submit" class="btn-primary">Aanmelden</button>
                </form>

                <div class="credentials-panel">
                    <details>
                        <summary>Geldige referentiegegevens</summary>
                        <div class="credentials-table">
                            <table>
                                <thead><tr><th>Gebruikersnaam</th><th>Wachtwoord</th><th>Rol</th></tr></thead>
                                <tbody>%s</tbody>
                            </table>
                        </div>
                    </details>
                </div>

                <script>
                function togglePwd() {
                    var p = document.getElementById('password');
                    p.type = p.type === 'password' ? 'text' : 'password';
                }
                </script>
                """.formatted(
                errorHtml,
                initials,
                active.getSchoolName(),
                active.getDomainName(),
                URLEncoder.encode(redirect_uri != null ? redirect_uri : "", StandardCharsets.UTF_8),
                URLEncoder.encode(state != null ? state : "", StandardCharsets.UTF_8),
                redirect_uri != null ? redirect_uri : "",
                state != null ? state : "",
                credentialsHtml
        );

        String html = buildHtml("Aanmelden", "Voer je gegevens in", body);
        response.setContentType("text/html; charset=utf-8");
        response.getWriter().write(html);
    }

    @PostMapping(value = {"/OAuth/index/login", "/OAuth/index/login/"})
    public void handleLogin(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String redirect_uri,
            @RequestParam(required = false) String state,
            HttpServletResponse response
    ) throws IOException {
        MockSchoolRegistry.AuthResult result = registry.authenticateUserAnySchool(username, password);
        if (result == null) {
            String encodedError = URLEncoder.encode("Ongeldige gebruikersnaam of wachtwoord", StandardCharsets.UTF_8);
            String location = "/api/OAuth/index/login?error=" + encodedError +
                    "&redirect_uri=" + URLEncoder.encode(redirect_uri != null ? redirect_uri : "", StandardCharsets.UTF_8) +
                    "&state=" + URLEncoder.encode(state != null ? state : "", StandardCharsets.UTF_8);
            response.sendRedirect(location);
            return;
        }

        MockSchoolData.MockUser user = result.user();
        MockSchoolData school = result.school();
        String code = registry.generateAuthCode(user, school);
        String location = (redirect_uri != null && !redirect_uri.isEmpty()
                ? redirect_uri
                : "http://localhost:8080/api/login/oauth2/code/smartschool")
                + "?code=" + code + (state != null ? "&state=" + state : "");

        response.sendRedirect(location);
        log.info("Mock OAuth login: user={} school={} -> code={}", username, school.getSchoolCode(), code);
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    @PostMapping(value = {"/OAuth/index/token", "/OAuth/token"})
    public ResponseEntity<Map<String, Object>> handleTokenExchange(
            @RequestParam String grant_type,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String refresh_token,
            @RequestParam(required = false) String client_id,
            @RequestParam(required = false) String client_secret,
            @RequestParam(required = false) String redirect_uri
    ) {
        String accessToken;
        String refreshToken;

        if ("refresh_token".equals(grant_type)) {
            String legacyId = jwtService.extractUsername(refresh_token);
            String schoolCode = (String) jwtService.extractClaim(refresh_token, "schoolCode");
            MockSchoolData school = registry.getSchoolByCode(schoolCode);
            MockSchoolData.MockUser user = school != null ? school.getUserByLegacyId(legacyId) : null;

            if (user == null || school == null) {
                return ResponseEntity.status(401).body(Map.of("error", "invalid_grant"));
            }

            TokenPair pair = generateTokenPair(user, school);
            accessToken = pair.accessToken();
            refreshToken = pair.refreshToken();
        } else {
            MockSchoolData school = registry.getSchoolByAuthCode(code);
            MockSchoolData.MockUser user = registry.resolveUserFromAuthCode(code);
            if (user == null || school == null) {
                return ResponseEntity.status(401).body(Map.of("error", "invalid_grant"));
            }

            TokenPair pair = generateTokenPair(user, school);
            accessToken = pair.accessToken();
            refreshToken = pair.refreshToken();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token", accessToken);
        body.put("token_type", "Bearer");
        body.put("expires_in", 3600);
        body.put("refresh_token", refreshToken);

        return ResponseEntity.ok(body);
    }

    @GetMapping(value = {"/Api/V1/fulluserinfo", "/Api/V1/fulluserinfo/"})
    public ResponseEntity<Map<String, Object>> fullUserInfo(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "access_token", required = false) String queryToken
    ) {
        String token = extractToken(authorization, queryToken);
        if (token == null) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token"));
        }

        String legacyId = jwtService.extractUsername(token);
        MockSchoolData.MockUser user = registry.getUserByLegacyId(legacyId);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token"));
        }

        MockSchoolData school = registry.getSchoolByLegacyId(legacyId);
        return ResponseEntity.ok(school.buildFullUserInfo(user));
    }

    @GetMapping(value = {"/Api/V1/userinfo", "/Api/V1/userinfo/"})
    public ResponseEntity<Map<String, Object>> userInfo(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "access_token", required = false) String queryToken
    ) {
        String token = extractToken(authorization, queryToken);
        if (token == null) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token"));
        }

        String legacyId = jwtService.extractUsername(token);
        MockSchoolData.MockUser user = registry.getUserByLegacyId(legacyId);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token"));
        }

        MockSchoolData school = registry.getSchoolByLegacyId(legacyId);
        return ResponseEntity.ok(school.buildUserInfo(user));
    }

    @GetMapping(value = {"/Api/V1/groupinfo", "/Api/V1/groupinfo/"})
    public ResponseEntity<Map<String, Object>> groupInfo(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "access_token", required = false) String queryToken
    ) {
        String token = extractToken(authorization, queryToken);
        if (token == null) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token"));
        }

        String legacyId = jwtService.extractUsername(token);
        MockSchoolData.MockUser user = registry.getUserByLegacyId(legacyId);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token"));
        }

        MockSchoolData school = registry.getSchoolByLegacyId(legacyId);
        return ResponseEntity.ok(school.buildGroupInfo(user));
    }

    @PostMapping(value = {"/Api/V1/sendmsg", "/Api/V1/sendmsg/"})
    public ResponseEntity<Map<String, Boolean>> sendMsg() {
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping(value = {"/Api/V1/sendnotif", "/Api/V1/sendnotif/"})
    public ResponseEntity<Map<String, Boolean>> sendNotif() {
        return ResponseEntity.ok(Map.of("success", true));
    }

    private TokenPair generateTokenPair(MockSchoolData.MockUser user, MockSchoolData school) {
        String role = switch (user.basisrol) {
            case "Leerling" -> "ROLE_STUDENT";
            case "Leerkracht" -> "ROLE_TEACHER";
            case "Beheerder" -> "ROLE_ADMIN";
            default -> "ROLE_STUDENT";
        };

        Map<String, Object> claims = Map.ofEntries(
                Map.entry(SmartschoolOAuth2UserService.CLAIM_CLASS_CODE, user.classCode),
                Map.entry(SmartschoolOAuth2UserService.CLAIM_YEAR_CODE, user.classCode.substring(0, 1)),
                Map.entry(SmartschoolOAuth2UserService.CLAIM_SCHOOL_CODE, school.getDomainName()),
                Map.entry("schoolId", school.getSchoolName()),
                Map.entry("campusId", ""),
                Map.entry("name", user.givenName),
                Map.entry("surname", user.familyName),
                Map.entry("email", user.email),
                Map.entry("platform", school.getPlatformUrl()),
                Map.entry("primaryGroupName", user.groupName),
                Map.entry("parentGroupNames", user.parentGroupNames)
        );

        String accessToken = jwtService.generateToken(user.legacyIdentifier, role, claims);
        Instant expiresAt = jwtService.extractExpirationInstant(accessToken);
        tokenStorageService.storeToken(user.legacyIdentifier, accessToken, role, expiresAt);

        String refreshToken = jwtService.generateToken(user.legacyIdentifier, role,
                Map.of("type", "refresh", "schoolCode", school.getSchoolCode()));

        return new TokenPair(accessToken, refreshToken);
    }

    private String extractToken(String authorization, String queryToken) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return queryToken;
    }

    private record TokenPair(String accessToken, String refreshToken) {}
}
