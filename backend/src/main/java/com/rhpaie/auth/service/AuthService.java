package com.rhpaie.auth.service;

import com.rhpaie.auth.dto.CurrentUserResponse;
import com.rhpaie.auth.dto.LoginRequest;
import com.rhpaie.auth.dto.LoginResponse;
import com.rhpaie.auth.dto.RefreshTokenRequest;
import com.rhpaie.auth.dto.RegisterRequest;
import com.rhpaie.auth.entity.LoginAudit;
import com.rhpaie.auth.entity.LoginStatus;
import com.rhpaie.auth.entity.RefreshToken;
import com.rhpaie.auth.mapper.AuthMapper;
import com.rhpaie.auth.repository.LoginAuditRepository;
import com.rhpaie.auth.repository.RefreshTokenRepository;
import com.rhpaie.common.security.CustomUserDetailsService;
import com.rhpaie.common.security.JwtService;
import com.rhpaie.user.entity.UserAccount;
import com.rhpaie.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAuditRepository loginAuditRepository;
    private final AuthMapper authMapper;

    @Value("${application.security.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    public String health() {
        return "OK";
    }

    @Transactional
    public CurrentUserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        UserAccount userAccount = UserAccount.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .enabled(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build();

        return authMapper.toCurrentUserResponse(userRepository.save(userAccount));
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            UserAccount userAccount = findUserByUsernameOrEmail(request.username());
            userAccount.setFailedLoginAttempts(0);
            userAccount.setLastLoginAt(LocalDateTime.now());
            userRepository.save(userAccount);

            saveLoginAudit(userAccount, request.username(), LoginStatus.SUCCESS, null, httpRequest);

            UserDetails userDetails = userDetailsService.loadUserByUsername(userAccount.getUsername());
            String accessToken = jwtService.generateAccessToken(userDetails);
            String refreshToken = createRefreshToken(userAccount).getToken();

            return authMapper.toLoginResponse(
                    userAccount,
                    accessToken,
                    refreshToken,
                    jwtService.getAccessTokenExpirationMs()
            );
        } catch (AuthenticationException exception) {
            handleFailedLogin(request.username(), exception.getMessage(), httpRequest);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        RefreshToken existingToken = refreshTokenRepository.findByTokenAndRevokedFalse(request.refreshToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (existingToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            revokeRefreshToken(existingToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        UserAccount userAccount = existingToken.getUserAccount();
        if (!userAccount.isEnabled() || userAccount.isAccountLocked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User account is not active");
        }

        revokeRefreshToken(existingToken);

        UserDetails userDetails = userDetailsService.loadUserByUsername(userAccount.getUsername());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = createRefreshToken(userAccount).getToken();

        return authMapper.toLoginResponse(
                userAccount,
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpirationMs()
        );
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .ifPresent(this::revokeRefreshToken);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(String username) {
        UserAccount userAccount = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return authMapper.toCurrentUserResponse(userAccount);
    }

    private RefreshToken createRefreshToken(UserAccount userAccount) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userAccount(userAccount)
                .expiresAt(LocalDateTime.now().plusNanos(refreshTokenExpirationMs * 1_000_000))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    private void revokeRefreshToken(RefreshToken refreshToken) {
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
    }

    private UserAccount findUserByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
    }

    private void handleFailedLogin(String username, String failureReason, HttpServletRequest httpRequest) {
        userRepository.findByUsernameOrEmail(username, username)
                .ifPresent(userAccount -> {
                    int failedAttempts = userAccount.getFailedLoginAttempts() + 1;
                    userAccount.setFailedLoginAttempts(failedAttempts);
                    if (failedAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
                        userAccount.setAccountLocked(true);
                    }
                    userRepository.save(userAccount);
                    saveLoginAudit(userAccount, username, LoginStatus.FAILED, failureReason, httpRequest);
                });

        if (userRepository.findByUsernameOrEmail(username, username).isEmpty()) {
            saveLoginAudit(null, username, LoginStatus.FAILED, failureReason, httpRequest);
        }
    }

    private void saveLoginAudit(
            UserAccount userAccount,
            String username,
            LoginStatus status,
            String failureReason,
            HttpServletRequest httpRequest
    ) {
        LoginAudit loginAudit = LoginAudit.builder()
                .userAccount(userAccount)
                .username(username)
                .status(status)
                .ipAddress(httpRequest.getRemoteAddr())
                .userAgent(httpRequest.getHeader("User-Agent"))
                .failureReason(failureReason)
                .build();

        loginAuditRepository.save(loginAudit);
    }
}
