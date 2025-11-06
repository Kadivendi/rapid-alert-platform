package com.rapidalert.notification.listener;

import com.rapidalert.notification.client.RecipientClient;
import com.rapidalert.notification.client.ShortenerClient;
import com.rapidalert.notification.dto.kafka.NotificationKafka;
import com.rapidalert.notification.dto.kafka.RecipientListKafka;
import com.rapidalert.notification.dto.request.NotificationRequest;
import com.rapidalert.notification.dto.response.NotificationResponse;
import com.rapidalert.notification.dto.response.RecipientResponse;
import com.rapidalert.notification.dto.response.TemplateHistoryResponse;
import com.rapidalert.notification.dto.response.UrlsResponse;
import com.rapidalert.notification.mapper.NotificationMapper;
import com.rapidalert.notification.model.NotificationType;
import com.rapidalert.notification.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static com.rapidalert.notification.model.NotificationType.*;

@Component
@RequiredArgsConstructor
public class KafkaListeners {

    private final KafkaTemplate<String, NotificationKafka> kafkaTemplate;
    private final NotificationService notificationService;
    private final RecipientClient recipientClient;
    private final ShortenerClient shortenerClient;
    private final NotificationMapper mapper;

    @Value("${spring.kafka.topics.notifications.email}")
    private String emailTopic;

    @Value("${spring.kafka.topics.notifications.phone}")
    private String phoneTopic;

    @Value("${spring.kafka.topics.notifications.telegram}")
    private String telegramTopic;

    @Value("${spring.kafka.topics.notifications.push}")
    private String pushTopic;

    @KafkaListener(
            topics = "#{ '${spring.kafka.topics.splitter}' }",
            groupId = "rapidalert",
            containerFactory = "listenerContainerFactory"
    )
    private void listener(RecipientListKafka recipientListKafka) { // TODO: @Async
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Runnable runnable = () -> {
            Long clientId = recipientListKafka.clientId();
            TemplateHistoryResponse template = recipientListKafka.templateHistoryResponse();

            for (Long recipientId : recipientListKafka.recipientIds()) {
                RecipientResponse response;
                try {
                    response = recipientClient.receiveByClientIdAndRecipientId(clientId, recipientId)
                            .getBody();
                } catch (RuntimeException e) {
                    // TODO
                    continue;
                }

                if (response == null) {
                    continue;
                }

                UrlsResponse urlsResponse = shortenerClient.generate(template.responseId())
                        .getBody();

                sendNotificationByCredential(response::email, EMAIL, response, clientId, template, emailTopic, urlsResponse);
                sendNotificationByCredential(response::phoneNumber, PHONE, response, clientId, template, phoneTopic, urlsResponse);
                sendNotificationByCredential(response::telegramId, TELEGRAM, response, clientId, template, telegramTopic, urlsResponse);
                sendNotificationByCredential(response::fcmToken, PUSH, response, clientId, template, pushTopic, urlsResponse);
            }
        };

        executorService.execute(runnable);
        executorService.shutdown();
    }

    private void sendNotificationByCredential( // TODO: too many params
                                               Supplier<String> supplier,
                                               NotificationType type,
                                               RecipientResponse recipientResponse,
                                               Long clientId,
                                               TemplateHistoryResponse template,
                                               String topic,
                                               UrlsResponse urlsResponse
    ) {
        String credential = supplier.get();
        if (credential != null) {
            Long notificationId;
            try {
                notificationId = notificationService.createNotification( // TODO: mapper
                        NotificationRequest.builder()
                                .type(type)
                                .credential(credential)
                                .template(template)
                                .recipientId(recipientResponse.id())
                                .clientId(clientId)
                                .urlId(urlsResponse.urlId())
                                .build()
                ).id();
            } catch (EntityNotFoundException e) {
                // TODO
                return;
            }
            NotificationResponse response = notificationService.setNotificationAsPending(clientId, notificationId);
            NotificationKafka notificationKafka = mapper.mapToKafka(response, urlsResponse.urlOptionMap());
            kafkaTemplate.send(topic, notificationKafka);
        }
    }
}
