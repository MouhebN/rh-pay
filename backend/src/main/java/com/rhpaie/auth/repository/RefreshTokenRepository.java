package com.rhpaie.auth.repository;

import com.rhpaie.auth.entity.RefreshToken;
import com.rhpaie.user.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    List<RefreshToken> findByUserAccountAndRevokedFalse(UserAccount userAccount);
}
