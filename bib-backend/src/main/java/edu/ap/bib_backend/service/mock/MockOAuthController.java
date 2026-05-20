package edu.ap.bib_backend.service.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MockOAuthController(MockSchoolRegistry registry) {
        this.registry = registry;
    }

    @GetMapping(value = {"/OAuth", "/OAuth/index/authorize", "/OAuth/index/authorize/"})
    public void showLoginForm(
            @RequestParam(required = false) String redirect_uri,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String response_type,
            @RequestParam(required = false) String client_id,
            @RequestParam(required = false) String scope,
            HttpServletResponse response
    ) throws IOException {
        MockSchoolData school = registry.getActiveSchool();

        String credentialsRows = school.getUsers().stream()
                .map(u -> "<tr><td><code>%s</code></td><td><code>%s</code></td><td>%s</td></tr>"
                        .formatted(u.username, u.password, u.basisrol))
                .collect(Collectors.joining());

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Mock Smartschool Login</title>
                    <style>
                        body { font-family: Arial, sans-serif; max-width: 600px; margin: 50px auto; padding: 20px; background: #f5f5f5; }
                        .container { background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        h1 { color: #1a73e8; margin-bottom: 10px; }
                        .badge { background: #ff9800; color: white; padding: 4px 8px; border-radius: 4px; font-size: 12px; }
                        .school-info { background: #e8f5e9; padding: 10px; border-radius: 4px; margin: 15px 0; }
                        form { margin-top: 20px; }
                        label { display: block; margin-top: 10px; font-weight: bold; }
                        input[type="text"], input[type="password"] { width: 100%%; padding: 8px; margin-top: 4px; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box; }
                        button { margin-top: 20px; padding: 10px 20px; background: #1a73e8; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 16px; }
                        button:hover { background: #1557b0; }
                        .credentials { background: #fff3e0; padding: 15px; border-radius: 4px; margin-top: 20px; }
                        .credentials h3 { margin-top: 0; }
                        table { width: 100%%; border-collapse: collapse; }
                        td, th { padding: 6px 10px; text-align: left; border-bottom: 1px solid #ddd; }
                        code { background: #f5f5f5; padding: 2px 6px; border-radius: 3px; font-size: 13px; }
                        input[type="hidden"] { display: none; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>Mock Smartschool Login <span class="badge">DEV MODE</span></h1>
                        <div class="school-info">
                            <strong>School:</strong> %s<br>
                            <strong>Domain:</strong> %s
                        </div>
                        <form method="POST" action="/api/OAuth/index/authorize">
                            <input type="hidden" name="redirect_uri" value="%s">
                            <input type="hidden" name="state" value="%s">
                            <label for="username">Username:</label>
                            <input type="text" id="username" name="username" required placeholder="e.g. ts1.student1">
                            <label for="password">Password:</label>
                            <input type="password" id="password" name="password" required placeholder="e.g. student1">
                            <button type="submit">Login</button>
                        </form>
                        <div class="credentials">
                            <h3>Valid Credentials</h3>
                            <table>
                                <tr><th>Username</th><th>Password</th><th>Role</th></tr>
                                %s
                            </table>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                school.getSchoolName(),
                school.getDomainName(),
                redirect_uri != null ? redirect_uri : "",
                state != null ? state : "",
                credentialsRows
        );

        response.setContentType("text/html; charset=utf-8");
        response.getWriter().write(html);
    }

    @PostMapping(value = {"/OAuth", "/OAuth/index/authorize", "/OAuth/index/authorize/"})
    public void handleLogin(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String redirect_uri,
            @RequestParam(required = false) String state,
            HttpServletResponse response
    ) throws IOException {
        MockSchoolData.MockUser user = registry.authenticateUser(username, password);
        if (user == null) {
            response.setContentType("text/html; charset=utf-8");
            response.getWriter().write("""
                    <!DOCTYPE html>
                    <html><head><title>Login Failed</title>
                    <style>body{font-family:Arial;margin:50px;text-align:center;} .error{color:red;}</style></head>
                    <body><h1 class="error">Invalid credentials</h1>
                    <p>Username or password is incorrect.</p>
                    <a href="/api/OAuth/index/authorize">Try again</a></body></html>
                    """);
            return;
        }

        String code = registry.generateAuthCode(user);
        String location = (redirect_uri != null && !redirect_uri.isEmpty()
                ? redirect_uri
                : "http://localhost:8080/api/login/oauth2/code/smartschool")
                + "?code=" + code + (state != null ? "&state=" + state : "");

        response.sendRedirect(location);
        log.info("Mock OAuth login: user={} -> code={}", username, code);
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
        String token;
        String refreshToken;

        if ("refresh_token".equals(grant_type)) {
            MockSchoolData.MockUser user = registry.getUserByToken(refresh_token);
            MockSchoolData school = registry.getSchoolByToken(refresh_token);
            if (user != null && school != null) {
                token = school.generateMockAccessToken(user.username);
                refreshToken = token.replace("mock-oauth-token", "mock-refresh-token");
                registry.registerToken(token, user, school);
            } else {
                return ResponseEntity.status(401).body(Map.of("error", "invalid_grant"));
            }
        } else {
            MockSchoolData.MockUser user = registry.resolveUserFromAuthCode(code);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "invalid_grant"));
            }

            MockSchoolData school = null;
            for (MockSchoolData s : registry.getAllSchools()) {
                if (s.getUserByUsername(user.username) != null) {
                    school = s;
                    break;
                }
            }
            if (school == null) {
                school = registry.getActiveSchool();
            }

            token = school.generateMockAccessToken(user.username);
            refreshToken = token.replace("mock-oauth-token", "mock-refresh-token");
            registry.registerToken(token, user, school);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token", token);
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

        MockSchoolData.MockUser user = registry.getUserByToken(token);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token"));
        }

        MockSchoolData school = registry.getSchoolByToken(token);
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

        MockSchoolData.MockUser user = registry.getUserByToken(token);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token"));
        }

        MockSchoolData school = registry.getSchoolByToken(token);
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

        MockSchoolData.MockUser user = registry.getUserByToken(token);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token"));
        }

        MockSchoolData school = registry.getSchoolByToken(token);
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

    private String extractToken(String authorization, String queryToken) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return queryToken;
    }
}
