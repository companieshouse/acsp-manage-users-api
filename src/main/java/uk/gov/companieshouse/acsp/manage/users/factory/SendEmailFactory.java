package uk.gov.companieshouse.acsp.manage.users.factory;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.acsp.manage.users.model.email.data.BaseEmailData;
import uk.gov.companieshouse.api.chskafka.SendEmail;

import java.util.UUID;

import static uk.gov.companieshouse.acsp.manage.users.utils.ParsingUtil.parseJsonFrom;

@Component
public class SendEmailFactory {
    private static final String FALLBACK = "";
    private final String appId;

    public SendEmailFactory(@Value("${email.appId}") String appId) {
        this.appId = appId;
    }

    public SendEmail createSendEmail(BaseEmailData<?> emailData, String messageType) {
        var sendEmail = new SendEmail();
        sendEmail.jsonData(parseJsonFrom(emailData, FALLBACK));
        sendEmail.setEmailAddress(emailData.getTo());
        sendEmail.setAppId(appId);
        sendEmail.setMessageId(UUID.randomUUID().toString());
        sendEmail.setMessageType(messageType);
        return sendEmail;
    }

}