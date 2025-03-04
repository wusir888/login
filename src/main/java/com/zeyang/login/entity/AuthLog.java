package com.zeyang.login.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * zeyang
 */


@Data
@Entity
@Table(name = "auth_logs",
        indexes = {
                @Index(name = "idx_created_at", columnList = "created_at"),
                @Index(name = "idx_user_id", columnList = "user_id")
        })
public class AuthLog {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "auth_logs_ibfk_1"))
    @OnDelete(action = OnDeleteAction.CASCADE)  // 对应 ON DELETE CASCADE
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, columnDefinition = "ENUM('LOGIN_SUCCESS','LOGIN_FAILURE','LOGOUT')")
    private AuthAction action;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "location", length = 100)
    private String location;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}