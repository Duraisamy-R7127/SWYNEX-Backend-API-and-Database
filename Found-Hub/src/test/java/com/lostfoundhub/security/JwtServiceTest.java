package com.lostfoundhub.security;

import com.lostfoundhub.enums.UserRole;
import com.lostfoundhub.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-0123456789-abcdefghij";

    private CustomUserDetails admin() {
        User user = new User();
        user.setId(7L);
        user.setName("Admin");
        user.setEmail("admin@example.com");
        user.setPassword("hash");
        user.setRole(UserRole.ADMIN);
        return new CustomUserDetails(user);
    }

    @Test
    void generatedTokenContainsSubjectAndRole() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        Claims claims = jwtService.parseToken(jwtService.generateToken(admin()));

        assertEquals("admin@example.com", claims.getSubject());
        assertEquals("ADMIN", claims.get("role", String.class));
    }

    @Test
    void everyTokenHasItsOwnId() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        String first = jwtService.parseToken(jwtService.generateToken(admin())).getId();
        String second = jwtService.parseToken(jwtService.generateToken(admin())).getId();
        assertNotEquals(first, second);
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        String token = jwtService.generateToken(admin());
        String[] parts = token.split("\\.");
        char first = parts[2].charAt(0);
        String tampered = parts[0] + "." + parts[1] + "." + (first == 'A' ? 'B' : 'A') + parts[2].substring(1);

        assertThrows(JwtException.class, () -> jwtService.parseToken(tampered));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = new JwtService("another-secret-key-0123456789-abcdefghijk", 60_000).generateToken(admin());
        JwtService jwtService = new JwtService(SECRET, 60_000);

        assertThrows(JwtException.class, () -> jwtService.parseToken(token));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET, -1_000);
        String token = jwtService.generateToken(admin());

        assertThrows(JwtException.class, () -> jwtService.parseToken(token));
    }

    @Test
    void shortSecretFailsFast() {
        assertThrows(IllegalStateException.class, () -> new JwtService("short", 60_000));
    }
}
