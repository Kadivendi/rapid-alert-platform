package com.rapidalert.sender.service.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.rapidalert.sender.dto.response.TemplateHistoryResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * Firebase Cloud Messaging push delivery.
 *
 * <p>Initialises a single {@link FirebaseApp} from the service-account JSON
 * pointed to by {@code notification.services.fcm.credentialsPath}. If the
 * credentials are not configured the service stays in a "dummy" mode — it
 * logs each dispatch and returns success without ever contacting Google,
 * which keeps local docker-compose runs green when no Firebase project is
 * wired up.
 *
 * <p>Token-revocation errors are propagated as failures rather than retried
 * forever; the existing rebalancer flow will move the recipient's
 * notification into the {@code RESENDING} state for the next sweep.
 */
@Slf4j
@Service
public class FcmAlertService {

    private static final String APP_NAME = "rapid-alert-fcm";

    @Value("${notification.services.fcm.credentialsPath:}")
    private String credentialsPath;

    @Value("${notification.services.fcm.projectId:}")
    private String projectId;

    private FirebaseMessaging messaging;
    private boolean dummyMode;

    @PostConstruct
    void init() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            this.dummyMode = true;
            log.warn(
                    "FIREBASE_CREDENTIALS_PATH not set — FCM running in dummy mode. "
                            + "Set the env var to enable live push delivery."
            );
            return;
        }
        try (FileInputStream stream = new FileInputStream(credentialsPath)) {
            FirebaseOptions.Builder optsBuilder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(stream));
            if (projectId != null && !projectId.isBlank()) {
                optsBuilder.setProjectId(projectId);
            }
            FirebaseOptions options = optsBuilder.build();

            FirebaseApp app = FirebaseApp.getApps().stream()
                    .filter(a -> APP_NAME.equals(a.getName()))
                    .findFirst()
                    .orElseGet(() -> FirebaseApp.initializeApp(options, APP_NAME));
            this.messaging = FirebaseMessaging.getInstance(app);
            log.info("FCM initialised (project={})", app.getOptions().getProjectId());
        } catch (IOException e) {
            this.dummyMode = true;
            log.error(
                    "Failed to load Firebase credentials from {} — falling back to dummy mode: {}",
                    credentialsPath, e.getMessage()
            );
        }
    }

    /**
     * Send a push notification.
     *
     * @param fcmToken the recipient's FCM registration token
     * @param template the rendered template (subject + body) to dispatch
     * @return {@code true} when the underlying provider accepted the message,
     *         {@code false} when delivery failed and the caller should mark
     *         the notification as {@code RESENDING}
     */
    public boolean sendPush(String fcmToken, TemplateHistoryResponse template) {
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("Skipping push: empty FCM token");
            return false;
        }

        String title = safeSubject(template);
        String body = safeBody(template);

        if (dummyMode || messaging == null) {
            log.info("FCM dummy mode: push to {} title='{}' body='{}'",
                    redact(fcmToken), title, body);
            return true;
        }

        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(
                        Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build()
                )
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setContentAvailable(true)
                                .setSound("default")
                                .build())
                        .build())
                .build();

        try {
            String responseId = messaging.send(message);
            log.info("Sent FCM push: id={} token={}", responseId, redact(fcmToken));
            return true;
        } catch (FirebaseMessagingException e) {
            log.error("FCM send failed for token {}: {} ({})",
                    redact(fcmToken), e.getMessage(),
                    e.getMessagingErrorCode());
            return false;
        }
    }

    private String safeSubject(TemplateHistoryResponse template) {
        if (template == null) return "Alert";
        String s = template.title();
        return (s == null || s.isBlank()) ? "Alert" : s;
    }

    private String safeBody(TemplateHistoryResponse template) {
        if (template == null) return "";
        String s = template.content();
        return s == null ? "" : s;
    }

    /** Redact all but the last four chars of an FCM token for safe logging. */
    private String redact(String token) {
        if (token == null || token.length() <= 4) return "***";
        return "***" + token.substring(token.length() - 4).toLowerCase(Locale.ROOT);
    }
}
