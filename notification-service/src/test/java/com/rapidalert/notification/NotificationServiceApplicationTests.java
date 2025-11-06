package com.rapidalert.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that the full Spring context comes up — including the JPA layer
 * (Testcontainers-managed Postgres via the {@code jdbc:tc:...} URL in
 * {@code application-test.yml}) and the Kafka consumer beans (embedded broker
 * via {@link EmbeddedKafka}).
 */
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {
                "email-notification-test",
                "phone-notification-test",
                "telegram-notification-test",
                "push-notification-test",
                "recipient-list-splitter-test",
                "rapid-alert.triage-events-test"
        }
)
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
        // Context loads successfully
    }

}
