package uk.gov.companieshouse.acsp.manage.users;

import org.mockito.Mockito;
import uk.gov.companieshouse.acsp.manage.users.factory.SendEmailFactory;
import uk.gov.companieshouse.api.chskafka.SendEmail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

public class TestUtils {
    public static SendEmail getMockSendEmail(SendEmailFactory sendEmailFactory) {
        SendEmail mockSendEmail = Mockito.mock(SendEmail.class);
        Mockito.when(sendEmailFactory.createSendEmail(any(), anyString())).thenReturn(mockSendEmail);
        return mockSendEmail;
    }
}
