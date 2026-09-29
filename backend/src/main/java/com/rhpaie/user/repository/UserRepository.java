package com.rhpaie.user.repository;

import com.rhpaie.user.entity.UserAccount;
import com.rhpaie.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByUsernameOrEmail(String username, String email);

    List<UserAccount> findByRole(Role role);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
