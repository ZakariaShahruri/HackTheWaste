package be.leuven.leuvengo.web;

import be.leuven.leuvengo.domain.AppUser;
import be.leuven.leuvengo.domain.Role;
import be.leuven.leuvengo.security.AuthService;
import be.leuven.leuvengo.security.CurrentUser;
import be.leuven.leuvengo.security.RateLimited;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    /** BCrypt only looks at the first 72 bytes, so longer passwords are rejected outright. */
    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Size(max = 10) String facultyShortCode,
            Boolean consentGiven) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(max = 72) String password) {}

    /** {@code token} is only present right after register/login. */
    public record AuthResponse(Long id, String email, String facultyShortCode, Role role, int points,
                               String token) {

        static AuthResponse of(AppUser u, String token) {
            return new AuthResponse(u.getId(), u.getEmail(), u.getFacultyShortCode(), u.getRole(),
                    u.getPoints(), token);
        }
    }

    @PostMapping("/register")
    @RateLimited(value = "register", requests = 5, perSeconds = 3600)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        AuthService.Session s = auth.register(req.email(), req.password(),
                blankToNull(req.facultyShortCode()), Boolean.TRUE.equals(req.consentGiven()));
        return AuthResponse.of(s.user(), s.token());
    }

    @PostMapping("/login")
    @RateLimited(value = "login", requests = 10, perSeconds = 300)
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        AuthService.Session s = auth.login(req.email(), req.password());
        return AuthResponse.of(s.user(), s.token());
    }

    @GetMapping("/me")
    public AuthResponse me(@CurrentUser AppUser user) {
        return AuthResponse.of(user, null);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CurrentUser AppUser user) {
        auth.logout(user);
        return ResponseEntity.noContent().build();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
