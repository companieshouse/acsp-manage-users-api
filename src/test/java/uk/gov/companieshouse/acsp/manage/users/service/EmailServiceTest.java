package uk.gov.companieshouse.acsp.manage.users.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static uk.gov.companieshouse.acsp.manage.users.model.MessageType.YOU_HAVE_BEEN_ADDED_TO_ACSP_MESSAGE_TYPE;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.acsp.manage.users.model.email.YouHaveBeenAddedToAcspEmailData;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class EmailServiceTest {

    @Mock
    private EmailProducer emailProducer;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendYouHaveBeenAddedToAcspEmailWithNullRecipientEmailOrAddedByOrAcspNameThrowsIllegalArgumentException(){
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendYouHaveBeenAddedToAcspEmail( "theId123", null, "demo@ch.gov.uk", "Witcher" ) );
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendYouHaveBeenAddedToAcspEmail( "theId123", "buzz.lightyear@toystory.com", null, "Witcher" ) );
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendYouHaveBeenAddedToAcspEmail( "theId123", "buzz.lightyear@toystory.com", "demo@ch.gov.uk", null ) );
    }

    @Test
    void sendYouHaveBeenAddedToAcspEmailWithUnexpectedIssueThrowsEmailSendingException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), eq( YOU_HAVE_BEEN_ADDED_TO_ACSP_MESSAGE_TYPE.getValue() ) );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendYouHaveBeenAddedToAcspEmail( "theId123", "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher" ) );
    }

    @Test
    void sendYouHaveBeenAddedToAcspEmailThrowsMessageOnToKafkaQueue(){
        final var expectedEmailData = new YouHaveBeenAddedToAcspEmailData( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher" );
        emailService.sendYouHaveBeenAddedToAcspEmail( "theId123", "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher" );
        Mockito.verify( emailProducer ).sendEmail( expectedEmailData, YOU_HAVE_BEEN_ADDED_TO_ACSP_MESSAGE_TYPE.getValue() );
    }

}
