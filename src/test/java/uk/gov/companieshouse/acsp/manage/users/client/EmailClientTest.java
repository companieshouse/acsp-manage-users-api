package uk.gov.companieshouse.acsp.manage.users.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.acsp.manage.users.exceptions.EmailSendException;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.chskafka.SendEmail;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.chskafka.PrivateSendEmailHandler;
import uk.gov.companieshouse.api.handler.chskafka.request.PrivateSendEmailPost;
import uk.gov.companieshouse.api.http.HttpClient;
import uk.gov.companieshouse.api.model.ApiResponse;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailClientTest {

    @Mock
    private Supplier<InternalApiClient> internalApiClientSupplier;

    @Mock
    private InternalApiClient internalApiClient;

    @Mock
    private SendEmail sendEmail;

    @Mock
    private ApiResponse<Void> apiResponse;

    @Mock
    private HttpClient httpClient;

    @Mock
    private PrivateSendEmailHandler privateSendEmailHandler;

    @Mock
    private PrivateSendEmailPost privateSendEmailPost;

    @InjectMocks
    private EmailClient emailClient;

    @BeforeEach
    void setUp() {
        emailClient = new EmailClient(internalApiClientSupplier);
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.sendEmailHandler()).thenReturn(privateSendEmailHandler);
        when(privateSendEmailHandler.postSendEmail(anyString(), eq(sendEmail))).thenReturn(privateSendEmailPost);
    }

    @Test
    void testSendEmail_Success() throws Exception {
        // Arrange
        when(privateSendEmailPost.execute()).thenReturn(apiResponse);
        when(apiResponse.getStatusCode()).thenReturn(200);

        // Act
        emailClient.sendEmail(sendEmail, "test-request-id");

        // Assert
        verify(privateSendEmailPost).execute();
    }

    @Test
    void testSendEmail_ApiErrorResponseException() throws Exception {
        // Arrange
        when(privateSendEmailPost.execute()).thenThrow(mock(ApiErrorResponseException.class));

        // Act & Assert
        assertThrows(EmailSendException.class, () ->
                emailClient.sendEmail(sendEmail, "test-request-id")
        );
    }
}