package com.Project;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;



@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Registers a new user asynchronously.
     * Validation of duplicate email/username + DB save happen in a
     * transactional unit; the confirmation email is fired off async
     * separately so it never blocks or fails the registration itself.
     */
    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<UserResponse> registerUser(RegisterRequest request) {
        logger.info("Processing registration for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setRole("USER");
        user.setIsActive(true);

        User saved = userRepository.save(user);

        // Fire-and-forget async email; does not block the response.
        emailService.sendRegistrationEmail(saved.getEmail(), saved.getUsername());

        return CompletableFuture.completedFuture(mapToResponse(saved));
    }

    /**
     * Authenticates a user asynchronously and issues a lightweight token.
     * Swap the token generation for real JWT signing in production.
     */
    @Async("taskExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<LoginResponse> login(LoginRequest request) {
        logger.info("Processing login for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new InvalidCredentialsException("Account is inactive. Please contact support.");
        }

        String token = generateToken(user);
        LoginResponse response = new LoginResponse(user.getId(), user.getUsername(), user.getEmail(),
                token, "Login successful");

        return CompletableFuture.completedFuture(response);
    }

    private String generateToken(User user) {
        // Lightweight placeholder token (base64 of userId:uuid).
        // Replace with a real JWT (e.g. jjwt) for production use.
        String raw = user.getId() + ":" + UUID.randomUUID();
        return Base64.getEncoder().encodeToString(raw.getBytes());
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getAddress(),
                user.getRole(),
                user.getIsActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
