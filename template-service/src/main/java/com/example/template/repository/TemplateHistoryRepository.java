package com.rapidalert.template.repository;

import com.rapidalert.template.entity.TemplateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TemplateHistoryRepository extends JpaRepository<TemplateHistory, Long> {

    Optional<TemplateHistory> findByIdAndClientId(Long templateHistoryId, Long clientId);

    Optional<TemplateHistory> findByClientIdAndResponseIdAndTitleAndContent(
            Long clientId,
            Long responseId,
            String title,
            String content
    );
}
