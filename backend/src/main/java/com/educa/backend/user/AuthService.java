package com.educa.backend.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.common.error.ApiException;
import com.educa.backend.common.error.ConflictException;
import com.educa.backend.config.EducaProperties;
import com.educa.backend.security.JwtService;
import com.educa.backend.user.dto.LoginRequest;
import com.educa.backend.user.dto.RegisterRequest;
import com.educa.backend.user.dto.TokenResponse;
import com.educa.backend.user.dto.UserDto;

import org.springframework.http.HttpStatus;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final long refreshTtlSeconds;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthenticationManager authenticationManager,
                       UserMapper userMapper, EducaProperties properties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userMapper = userMapper;
        this.refreshTtlSeconds = properties.jwt().refreshTtlSeconds();
    }

    @Transactional
    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Un compte existe déjà pour cet email");
        }
        Role learner = roleRepository.findByName(RoleName.LEARNER)
                .orElseThrow(() -> new IllegalStateException("Rôle LEARNER absent — vérifier les migrations"));

        User user = new User();
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setPreferredLanguage(request.preferredLanguage() != null ? request.preferredLanguage() : "fr");
        user.addRole(learner);

        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Identifiants invalides"));

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Jeton de rafraîchissement invalide"));

        if (!stored.isActive()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Jeton de rafraîchissement expiré ou révoqué");
        }
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken)).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private TokenResponse issueTokens(User user) {
        List<String> roles = user.getRoles().stream().map(r -> r.getName().name()).toList();
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), roles);

        String rawRefresh = generateRawToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(sha256(rawRefresh));
        refreshToken.setExpiresAt(Instant.now().plusSeconds(refreshTtlSeconds));
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(accessToken, rawRefresh, jwtService.getAccessTtlSeconds(), userMapper.toDto(user));
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
