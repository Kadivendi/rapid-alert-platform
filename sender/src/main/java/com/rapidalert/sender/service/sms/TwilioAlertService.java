package com.rapidalert.sender.service.sms;

import com.rapidalert.sender.dto.response.TemplateHistoryResponse;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Slf4j
@Service
public class TwilioAlertService {

    @Value("${twilio.account.sid:dummy_sid}")
    private String accountSid;

    @Value("${twilio.auth.token:dummy_token}")
    private String authToken;

    @Value("${twilio.phone.number:+1234567890}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        if (!"dummy_sid".equals(accountSid)) {
            Twilio.init(accountSid, authToken);
        }
    }

    public boolean sendSms(String toPhone, TemplateHistoryResponse template) {
        try {
            if ("dummy_sid".equals(accountSid)) {
                log.info("Twilio dummy mode: Sending SMS to {} with text {}", toPhone, template.content());
                return true;
            }
            Message message = Message.creator(
                    new PhoneNumber(toPhone),
                    new PhoneNumber(fromNumber),
                    template.content()
            ).create();
            log.info("Sent SMS successfully, SID: {}", message.getSid());
            return true;
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", toPhone, e.getMessage());
            return false;
        }
    }
}
