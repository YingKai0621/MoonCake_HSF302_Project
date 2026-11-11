package com.se201411.project.mooncake.security.repository;

import com.se201411.project.mooncake.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @Query("""
        SELECT token
        FROM PasswordResetToken token
        JOIN FETCH token.account
        WHERE token.tokenHash = :tokenHash
        """)
    Optional<PasswordResetToken> findWithAccountByTokenHash(@Param("tokenHash") String tokenHash);

    boolean existsByAccount_IdAndCreatedAtAfter(Long accountId, LocalDateTime createdAfter);

    /**
     * Closes every still-open token of an account so only the newest link works.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE PasswordResetToken token
        SET token.usedAt = :now
        WHERE token.account.id = :accountId
          AND token.usedAt IS NULL
        """)
    int invalidateOpenTokens(@Param("accountId") Long accountId, @Param("now") LocalDateTime now);

    /**
     * Conditional single-use claim: returns 1 only for the request that wins the token.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE PasswordResetToken token
        SET token.usedAt = :now
        WHERE token.id = :tokenId
          AND token.usedAt IS NULL
          AND token.expiresAt > :now
        """)
    int consume(@Param("tokenId") Long tokenId, @Param("now") LocalDateTime now);
}
