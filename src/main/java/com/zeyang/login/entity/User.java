package com.zeyang.login.entity;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * zeyang
 */
@Data
@Entity
@Table(name = "users",
        indexes = {
                @Index(name = "idx_email", columnList = "email"),
                @Index(name = "idx_username", columnList = "username")
        })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 匹配 bigint 类型

    @Column(name = "username",
            nullable = false,
            unique = true,
            length = 50)
    private String username;

    @Column(name = "password_hash",
            nullable = false,
            length = 255)
    private String passwordHash;

    @Column(name = "salt",
            nullable = false,
            length = 64)
    private String salt;

    @Column(name = "email",
            nullable = false,
            unique = true,
            length = 100)
    private String email;

    @Column(name = "phone",
            length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status",
            nullable = false,
            columnDefinition = "ENUM('ACTIVE','LOCKED','DISABLED') DEFAULT 'ACTIVE'")
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "failed_attempts",
            columnDefinition = "INT DEFAULT 0")
    private Integer failedAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @CreationTimestamp
    @Column(name = "created_at",
            nullable = false,
            updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at",
            nullable = false)
    private LocalDateTime updatedAt;
}
