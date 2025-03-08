package com.zeyang.login.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zeyang.login.entity.AuthLog;
import com.zeyang.login.entity.User;

@Repository
public interface AuthLogRepository extends JpaRepository<AuthLog, Long> {
    List<AuthLog> findByUserOrderByCreatedAtDesc(User user);
    List<AuthLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
} 