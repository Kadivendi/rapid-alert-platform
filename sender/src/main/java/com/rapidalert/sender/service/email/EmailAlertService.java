package com.rapidalert.sender.service.email;

import com.rapidalert.sender.dto.response.TemplateHistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAlertService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@rapidalert.com}")
    private String fromAddress;

    public boolean sendEmail(String toAddress, TemplateHistoryResponse template) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toAddress);
            message.setSubject("Emergency Alert System Notification");
            message.setText(template.content());
            message.setSubject(template.title() != null ? template.title() : "Emergency Alert System Notification");
            mailSender.send(message);
            log.info("Sent Email successfully to {}", toAddress);
            return true;
        } catch (Exception e) {
            log.error("Failed to send Email to {}: {}", toAddress, e.getMessage());
            return false;
        }
    }
}
