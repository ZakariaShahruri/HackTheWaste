package be.leuven.leuvengo.security;

import be.leuven.leuvengo.config.LeuvenGoProperties;
import be.leuven.leuvengo.domain.AppUser;
import be.leuven.leuvengo.domain.Role;
import be.leuven.leuvengo.repository.AppUserRepository;
import be.leuven.leuvengo.repository.FacultyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/** Registration, login and bearer-token sessions. */
@Service
public class AuthService {

    private static final String BEARER = "Bearer ";
    /** Runs BCrypt for unknown emails too, so response time doesn't reveal which emails exist. */
    private static final String DUMMY_HASH = new BCryptPasswordEncoder().encode("not-a-real-password");

    private final AppUserRepository users;
    private final FacultyRepository faculties;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final LeuvenGoProperties props;
    private final Clock clock;

    public AuthService(AppUserRepository users, FacultyRepository faculties,
                       LeuvenGoProperties props, Clock clock) {
        this.users = users;
        this.faculties = faculties;
        this.props = props;
        this.clock = clock;
    }

    /** A logged-in user plus the raw token to hand back to the client (once). */
    public record Session(AppUser user, String token) {}

    @Transactional
    public Session register(String email, String password, String facultyShortCode, boolean consentGiven) {
        if (!consentGiven) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You must accept the data consent to create an account");
        }
        String normalized = normalize(email);
        if (facultyShortCode != null && faculties.findByShortCode(facultyShortCode).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown clan " + facultyShortCode);
        }
        if (users.existsByEmail(normalized)) {
            throw emailTaken();
        }

        Instant now = clock.instant();
        AppUser user = new AppUser();
        user.setEmail(normalized);
        user.setPasswordHash(passwords.encode(password));
        user.setFacultyShortCode(facultyShortCode);
        user.setConsentGiven(true);
        user.setConsentGivenAt(now);
        user.setCreatedAt(now);
        try {
            return startSession(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException raceWithAnotherSignup) {
            throw emailTaken();
        }
    }

    @Transactional
    public Session login(String email, String password) {
        Optional<AppUser> user = users.findByEmail(normalize(email));
        String hash = user.map(AppUser::getPasswordHash).orElse(DUMMY_HASH);
        if (!passwords.matches(password, hash) || user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return startSession(user.get());
    }

    @Transactional
    public void logout(AppUser user) {
        user.setSessionTokenHash(null);
        user.setSessionExpiresAt(null);
        users.save(user);
    }

    /** Resolves an {@code Authorization: Bearer ...} header to a user with a live session. */
    @Transactional(readOnly = true)
    public Optional<AppUser> authenticate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER)) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring(BEARER.length()).trim();
        if (token.isEmpty()) return Optional.empty();
        return users.findBySessionTokenHash(SessionTokens.hash(token))
                .filter(u -> u.getSessionExpiresAt() != null && u.getSessionExpiresAt().isAfter(clock.instant()));
    }

    /** Creates or refreshes the demo crew account so the operations page can be tried out. */
    @Transactional
    public void ensureCrewAccount(String email, String password) {
        String normalized = normalize(email);
        AppUser crew = users.findByEmail(normalized).orElseGet(AppUser::new);
        crew.setEmail(normalized);
        crew.setPasswordHash(passwords.encode(password));
        crew.setRole(Role.CREW);
        crew.setConsentGiven(true);
        if (crew.getCreatedAt() == null) {
            crew.setCreatedAt(clock.instant());
            crew.setConsentGivenAt(clock.instant());
        }
        users.save(crew);
    }

    private Session startSession(AppUser user) {
        String token = SessionTokens.newToken();
        user.setSessionTokenHash(SessionTokens.hash(token));
        user.setSessionExpiresAt(clock.instant().plus(props.security().sessionTtl()));
        return new Session(users.save(user), token);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static ResponseStatusException emailTaken() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
    }
}
