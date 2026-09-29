package com.rhpaie.auth.repository;

import com.rhpaie.auth.entity.LoginAudit;
import com.rhpaie.user.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {

    List<LoginAudit> findTop10ByUserAccountOrderByAttemptedAtDesc(UserAccount userAccount);

    List<LoginAudit> findTop10ByUsernameOrderByAttemptedAtDesc(String username);
}
