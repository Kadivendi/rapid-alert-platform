package com.rapidalert.sender.listener;

import com.rapidalert.sender.client.NotificationClient;
import com.rapidalert.sender.dto.kafka.NotificationKafka;
import com.rapidalert.sender.dto.response.TemplateHistoryResponse;
import com.rapidalert.sender.ratelimit.TokenBucketRateLimiter;
import com.rapidalert.sender.service.email.EmailAlertService;
import com.rapidalert.sender.service.push.FcmAlertService;
import com.rapidalert.sender.service.sms.TwilioAlertService;
import com.rapidalert.sender.service.telegram.TelegramAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaListeners {

    private final TelegramAlertService telegramAlertService;
    private final TwilioAlertService twilioAlertService;
    private final EmailAlertService emailAlertService;
    private final FcmAlertService fcmAlertService;
    private final NotificationClient notificationClient;
    private final TokenBucketRateLimiter rateLimiter;

    @Value("${notification.maxRetryAttempts}")
    private int maxRetryAttempts;

    @KafkaListener(
            topics = "#{ '${spring.kafka.topics.notifications.telegram}' }",
            groupId = "rapidalert",
            containerFactory = "listenerContainerFactory"
    )
    private void telegramNotificationListener(NotificationKafka notification) {
        deliver("telegram", notification, (credential, template) ->
                telegramAlertService.sendMessage(credential, template));
    }

    @KafkaListener(
            topics = "#{ '${spring.kafka.topics.notifications.email}' }",
            groupId = "rapidalert",
            containerFactory = "listenerContainerFactory"
    )
    private void emailNotificationListener(NotificationKafka notification) {
        deliver("email", notification, (credential, template) ->
                emailAlertService.sendEmail(credential, template));
    }

    @KafkaListener(
            topics = "#{ '${spring.kafka.topics.notifications.phone}' }",
            groupId = "rapidalert",
            containerFactory = "listenerContainerFactory"
    )
    private void phoneNotificationListener(NotificationKafka notification) {
        deliver("sms", notification, (credential, template) ->
                twilioAlertService.sendSms(credential, template));
    }

    @KafkaListener(
            topics = "#{ '${spring.kafka.topics.notifications.push}' }",
            groupId = "rapidalert",
            containerFactory = "listenerContainerFactory"
    )
    private void pushNotificationListener(NotificationKafka notification) {
        deliver("push", notification, (credential, template) ->
                fcmAlertService.sendPush(credential, template));
    }

    private void deliver(String channel, NotificationKafka notification, AlertSender sender) {
        logNotification(notification);
        Long clientId = notification.clientId();
        Long notificationId = notification.id();

        if (notification.retryAttempts() >= maxRetryAttempts) {
            notificationClient.setNotificationAsError(clientId, notificationId);
            return;
        }

        try {
            rateLimiter.acquire(channel);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            notificationClient.setNotificationAsResending(clientId, notificationId);
            return;
        }

        try {
            boolean isSent = sender.send(notification.credential(), notification.template());
            if (isSent) {
                notificationClient.setNotificationAsSent(clientId, notificationId);
            } else {
                notificationClient.setNotificationAsResending(clientId, notificationId);
            }
        } catch (RuntimeException e) {
            log.warn("Send to {} failed for notification={}: {}", channel, notificationId, e.getMessage());
            notificationClient.setNotificationAsResending(clientId, notificationId);
        }
    }

    private void logNotification(NotificationKafka notificationKafka) {
        log.info(
                "Sending {} notification to `{}`, status={}, retryAttempts={}, tokens={}",
                notificationKafka.type(),
                notificationKafka.credential(),
                notificationKafka.status(),
                notificationKafka.retryAttempts(),
                rateLimiter.getAvailableTokens(String.valueOf(notificationKafka.type()).toLowerCase())
        );
    }

    @FunctionalInterface
    private interface AlertSender {
        boolean send(String credential, TemplateHistoryResponse template);
    }
}
