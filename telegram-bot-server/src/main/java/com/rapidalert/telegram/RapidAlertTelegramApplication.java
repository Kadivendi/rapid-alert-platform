package com.rapidalert.telegram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Telegram bot side-car.
 *
 * The bot connects to the Telegram Bot API via long polling and surfaces a small
 * /info command so recipients can self-register their chat id with the
 * recipient-service.
 */
@SpringBootApplication
public class RapidAlertTelegramApplication {

    public static void main(String[] args) {
        SpringApplication.run(RapidAlertTelegramApplication.class, args);
    }
}
