package com.rapidalert.telegram.config;

import com.rapidalert.telegram.bot.RapidAlertTelegramBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class RapidAlertTelegramConfig {

    /**
     * Register the bot with Telegram only in non-test profiles. The
     * registration call goes out to api.telegram.org to validate the token,
     * which makes the contextLoads() test flaky / fail in offline CI.
     */
    @Bean
    @Profile("!test")
    public TelegramBotsApi telegramBotsApi(RapidAlertTelegramBot bot) throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(bot);
        return api;
    }
}
