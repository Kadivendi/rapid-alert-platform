package com.rapidalert.template.repository;

import com.rapidalert.template.entity.RecipientId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipientIdRepository extends JpaRepository<RecipientId, Long> {

    Boolean existsByTemplateIdAndRecipientId(Long templateId, Long recipientId);
}
