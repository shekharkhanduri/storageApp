package com.storeit.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        // 256-bit secret (>= 32 bytes) required for HMAC-SHA256
        String secret = "test-secret-key-that-is-at-least-32-bytes-long!!";
        jwtUtil = new JwtUtil(secret, 900, 604800);
    }

    @Test
    @DisplayName("Access token: generate → validate → extract claims")
    void accessToken_roundTrip() {
        String token = jwtUtil.generateAccessToken("user-123", "test@example.com");

        assertTrue(jwtUtil.validateToken(token));
        assertEquals("user-123", jwtUtil.extractUserId(token));
        assertEquals("test@example.com", jwtUtil.extractEmail(token));
        assertEquals("access", jwtUtil.extractTokenType(token));
    }

    @Test
    @DisplayName("Refresh token: generate → validate → type is 'refresh'")
    void refreshToken_roundTrip() {
        String token = jwtUtil.generateRefreshToken("user-456", "refresh@example.com");

        assertTrue(jwtUtil.validateToken(token));
        assertEquals("user-456", jwtUtil.extractUserId(token));
        assertEquals("refresh", jwtUtil.extractTokenType(token));
    }

    @Test
    @DisplayName("Garbage string should not validate")
    void invalidToken_returnsFalse() {
        assertFalse(jwtUtil.validateToken("not.a.jwt"));
    }

    @Test
    @DisplayName("Tampered token should not validate")
    void tamperedToken_returnsFalse() {
        String token = jwtUtil.generateAccessToken("user-123", "test@example.com");
        char lastChar = token.charAt(token.length() - 1);
        char replaced = lastChar == 'a' ? 'b' : 'a';
        String tampered = token.substring(0, token.length() - 1) + replaced;
        assertFalse(jwtUtil.validateToken(tampered));
    }

    @Test
    @DisplayName("Null token should not validate")
    void nullToken_returnsFalse() {
        assertFalse(jwtUtil.validateToken(null));
    }

    @Test
    @DisplayName("Empty string should not validate")
    void emptyToken_returnsFalse() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    @DisplayName("Token signed with different secret should not validate")
    void differentSecret_returnsFalse() {
        JwtUtil other = new JwtUtil(
                "another-secret-key-that-is-at-least-32-bytes-long!", 900, 604800);
        String token = other.generateAccessToken("user-123", "test@example.com");

        assertFalse(jwtUtil.validateToken(token));
    }
}
