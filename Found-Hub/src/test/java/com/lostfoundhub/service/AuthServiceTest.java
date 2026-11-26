package com.lostfoundhub.service;

import com.lostfoundhub.dto.RegisterRequest;
import com.lostfoundhub.dto.UserResponse;
import com.lostfoundhub.enums.UserRole;
import com.lostfoundhub.exception.DuplicateResourceException;
import com.lostfoundhub.model.User;
import com.lostfoundhub.repository.UserRepository;
import com.lostfoundhub.security.CustomUserDetails;
import com.lostfoundhub.security.JwtService;
import com.lostfoundhub.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET = "unit-test-secret-key-0123456789-abcdefghij";

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;

    private JwtService jwtService;
    private TokenBlacklistService blacklist;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 3_600_000);
        blacklist = new TokenBlacklistService();
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService, blacklist);
    }

    @Test
    void register_encodesPasswordLowercasesEmailAndCreatesNormalUser() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(5L);
            return u;
        });

        UserResponse response = authService.register(new RegisterRequest("John", " John@Example.com ", "Passw0rd!", null));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("HASHED", saved.getValue().getPassword());
        assertEquals("john@example.com", saved.getValue().getEmail());
        assertEquals(UserRole.USER, saved.getValue().getRole());
        assertEquals(5L, response.id());
    }

    @Test
    void register_duplicateEmail_throwsAndDoesNotSave() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> authService.register(new RegisterRequest("John", "john@example.com", "Passw0rd!", null)));

        verify(userRepository, never()).save(any());
    }

    @Test
    void logout_revokesTheToken() {
        User user = new User();
        user.setId(1L);
        user.setName("A");
        user.setEmail("a@example.com");
        user.setPassword("x");
        user.setRole(UserRole.USER);
        String token = jwtService.generateToken(new CustomUserDetails(user));
        Claims claims = jwtService.parseToken(token);
        assertFalse(blacklist.isRevoked(claims.getId()));

        authService.logout(token);

        assertTrue(blacklist.isRevoked(claims.getId()));
    }

    @Test
    void logout_withGarbageToken_doesNotThrow() {
        authService.logout("not-a-jwt");
    }
}
