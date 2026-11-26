package com.lostfoundhub.service;

import com.lostfoundhub.dto.LoginRequest;
import com.lostfoundhub.dto.LoginResponse;
import com.lostfoundhub.dto.RegisterRequest;
import com.lostfoundhub.dto.UserResponse;
import com.lostfoundhub.enums.UserRole;
import com.lostfoundhub.exception.DuplicateResourceException;
import com.lostfoundhub.exception.ResourceNotFoundException;
import com.lostfoundhub.model.User;
import com.lostfoundhub.repository.UserRepository;
import com.lostfoundhub.security.CustomUserDetails;
import com.lostfoundhub.security.JwtService;
import com.lostfoundhub.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /** Public registration always creates a normal USER (admins cannot self-register). */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered: " + email);
        }
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(UserRole.USER);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        // Throws BadCredentialsException (-> 401) when email or password is wrong
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String token = jwtService.generateToken(principal);
        return new LoginResponse(token, "Bearer", jwtService.getExpirationMs(), UserResponse.from(user));
    }

    /** Revokes the token so it cannot be used again. */
    public void logout(String token) {
        try {
            Claims claims = jwtService.parseToken(token);
            tokenBlacklistService.revoke(claims.getId(), claims.getExpiration());
        } catch (JwtException | IllegalArgumentException ex) {
            // Already invalid/expired - nothing to revoke
        }
    }
}
