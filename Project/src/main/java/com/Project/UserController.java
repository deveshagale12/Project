package com.Project;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Register a new user. Runs asynchronously; the HTTP request thread
     * is released back to the pool while registration + email run on the
     * async executor, improving throughput under load.
     */
    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<ApiResponse<UserResponse>>> register(
            @Valid @RequestBody RegisterRequest request) {

        return userService.registerUser(request)
                .thenApply(userResponse -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(ApiResponse.success("User registered successfully", userResponse)));
    }

    /**
     * Login with email + password, returns a token on success.
     */
    @PostMapping("/login")
    public CompletableFuture<ResponseEntity<ApiResponse<LoginResponse>>> login(
            @Valid @RequestBody LoginRequest request) {

        return userService.login(request)
                .thenApply(loginResponse -> ResponseEntity
                        .ok(ApiResponse.success("Login successful", loginResponse)));
    }
}
