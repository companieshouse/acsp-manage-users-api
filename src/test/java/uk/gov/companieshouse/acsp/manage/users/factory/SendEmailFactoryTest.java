package uk.gov.companieshouse.acsp.manage.users.factory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.acsp.manage.users.model.email.data.BaseEmailData;
import uk.gov.companieshouse.api.chskafka.SendEmail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SendEmailFactoryTest {

    private static final String TEST_APP_ID = "test-app-id";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_MESSAGE_TYPE = "test-message-type";

    private SendEmailFactory sendEmailFactory;

    @BeforeEach
    void setUp() {
        sendEmailFactory = new SendEmailFactory(TEST_APP_ID);
    }

    @Test
    void testCreateSendEmail() {
        // Arrange
        BaseEmailData<?> emailData = mock(BaseEmailData.class);
        when(emailData.getTo()).thenReturn(TEST_EMAIL);

        // Act
        SendEmail sendEmail = sendEmailFactory.createSendEmail(emailData, TEST_MESSAGE_TYPE);

        // Assert
        assertEquals(TEST_APP_ID, sendEmail.getAppId());
        assertEquals(TEST_EMAIL, sendEmail.getEmailAddress());
        assertEquals(TEST_MESSAGE_TYPE, sendEmail.getMessageType());
        verify(emailData, times(2)).getTo();
    }
}