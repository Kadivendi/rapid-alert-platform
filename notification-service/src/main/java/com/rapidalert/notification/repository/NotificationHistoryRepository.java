package com.rapidalert.notification.repository;

import com.rapidalert.notification.entity.NotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {
}
