package com.rhpaie.user.service;

import com.rhpaie.user.dto.UserRequest;
import com.rhpaie.user.dto.UserResponse;
import com.rhpaie.user.dto.UserStatusRequest;
import com.rhpaie.user.dto.UserUpdateRequest;
import com.rhpaie.user.entity.UserAccount;
import com.rhpaie.user.mapper.UserMapper;
import com.rhpaie.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public String health() {
        return "OK";
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userMapper.toResponse(findUserAccountById(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        validateUsernameAvailable(request.username(), null);
        validateEmailAvailable(request.email(), null);

        UserAccount userAccount = UserAccount.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .enabled(request.enabled() == null || request.enabled())
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build();

        return userMapper.toResponse(userRepository.save(userAccount));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        UserAccount userAccount = findUserAccountById(id);
        validateUsernameAvailable(request.username(), id);
        validateEmailAvailable(request.email(), id);

        userAccount.setUsername(request.username());
        userAccount.setEmail(request.email());
        userAccount.setRole(request.role());

        return userMapper.toResponse(userRepository.save(userAccount));
    }

    @Transactional
    public UserResponse updateStatus(Long id, UserStatusRequest request) {
        UserAccount userAccount = findUserAccountById(id);
        userAccount.setEnabled(request.enabled());
        return userMapper.toResponse(userRepository.save(userAccount));
    }

    @Transactional
    public UserResponse unlock(Long id) {
        UserAccount userAccount = findUserAccountById(id);
        userAccount.setAccountLocked(false);
        userAccount.setFailedLoginAttempts(0);
        return userMapper.toResponse(userRepository.save(userAccount));
    }

    @Transactional
    public void delete(Long id) {
        UserAccount userAccount = findUserAccountById(id);
        userRepository.delete(userAccount);
    }

    @Transactional
    public UserResponse changePassword(Long id, String newPassword) {
        UserAccount userAccount = findUserAccountById(id);
        userAccount.setPassword(passwordEncoder.encode(newPassword));
        userAccount.setPasswordChangedAt(LocalDateTime.now());
        return userMapper.toResponse(userRepository.save(userAccount));
    }

    private UserAccount findUserAccountById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void validateUsernameAvailable(String username, Long currentUserId) {
        userRepository.findByUsername(username)
                .filter(userAccount -> !userAccount.getId().equals(currentUserId))
                .ifPresent(userAccount -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
                });
    }

    private void validateEmailAvailable(String email, Long currentUserId) {
        userRepository.findByEmail(email)
                .filter(userAccount -> !userAccount.getId().equals(currentUserId))
                .ifPresent(userAccount -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
                });
    }
}
