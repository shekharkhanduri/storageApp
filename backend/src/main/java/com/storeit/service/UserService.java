package com.storeit.service;

import com.storeit.dto.AuthResponse;
import com.storeit.dto.LoginRequest;
import com.storeit.dto.RegisterRequest;
import com.storeit.dto.UserDto;
import com.storeit.entity.User;
import com.storeit.exception.DuplicateResourceException;
import com.storeit.exception.ResourceNotFoundException;
import com.storeit.mapper.UserMapper;
import com.storeit.repository.UserRepository;
import com.storeit.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "User with email " + request.getEmail() + " already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResourceNotFoundException("Invalid email or password");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setRefreshTokenHash(null);
        userRepository.save(user);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String tokenType = jwtUtil.extractTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new IllegalArgumentException("Token is not a refresh token");
        }

        String userId = jwtUtil.extractUserId(refreshToken);
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify the stored refresh token hash matches
        if (user.getRefreshTokenHash() == null ||
                !hashToken(refreshToken).equals(user.getRefreshTokenHash())) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }

        // Issue new access token only (refresh token stays the same)
        String accessToken = jwtUtil.generateAccessToken(
                user.getId().toString(), user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .user(userMapper.toDto(user))
                .build();
    }

    public UserDto getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMapper.toDto(user);
    }

    // ---- private helpers ----

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(
                user.getId().toString(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(
                user.getId().toString(), user.getEmail());

        // Persist SHA-256 hash of refresh token (not the raw JWT)
        user.setRefreshTokenHash(hashToken(refreshToken));
        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userMapper.toDto(user))
                .build();
    }

    /**
     * SHA-256 hash of a token string, returned as lowercase hex.
     * Used to store refresh tokens securely without keeping the raw JWT.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
