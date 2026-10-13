package com.storeit.controller;

import com.storeit.dto.AuthResponse;
import com.storeit.dto.LoginRequest;
import com.storeit.dto.RegisterRequest;
import com.storeit.dto.UserDto;
import com.storeit.entity.User;
import com.storeit.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, logout, token refresh, profile")
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {

        AuthResponse auth = userService.register(request);
        setRefreshTokenCookie(response, auth.getRefreshToken());
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResponse auth = userService.login(request);
        setRefreshTokenCookie(response, auth.getRefreshToken());
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout — invalidates refresh token")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal User user,
            HttpServletResponse response) {

        userService.logout(user.getId());
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using httpOnly cookie")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "storeit-refresh", required = false) String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        AuthResponse auth = userService.refreshToken(refreshToken);
        return ResponseEntity.ok(auth);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal User user) {
        UserDto dto = userService.getCurrentUser(user.getId());
        return ResponseEntity.ok(dto);
    }

    // ---- cookie helpers ----

    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("storeit-refresh", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);   // set true behind HTTPS in production
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("storeit-refresh", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
