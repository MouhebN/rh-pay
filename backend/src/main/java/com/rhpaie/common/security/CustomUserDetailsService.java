package com.rhpaie.common.security;

import com.rhpaie.user.entity.UserAccount;
import com.rhpaie.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        UserAccount userAccount = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return User.builder()
                .username(userAccount.getUsername())
                .password(userAccount.getPassword())
                .disabled(!userAccount.isEnabled())
                .accountLocked(userAccount.isAccountLocked())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + userAccount.getRole().name())))
                .build();
    }
}
