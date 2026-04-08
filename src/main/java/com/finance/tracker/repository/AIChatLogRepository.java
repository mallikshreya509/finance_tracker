package com.finance.tracker.repository;

import com.finance.tracker.model.AIChatLog;
import com.finance.tracker.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AIChatLogRepository extends JpaRepository<AIChatLog, Long> {
    Page<AIChatLog> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
