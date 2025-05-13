package uk.gov.companieshouse.acsp.manage.users.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.companieshouse.acsp.manage.users.model.email.ConfirmYouAreAMember.ConfirmYouAreAStandardMemberEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.ConfirmYouAreAMember.ConfirmYouAreAnAdminMemberEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.ConfirmYouAreAMember.ConfirmYouAreAnOwnerMemberEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.YouHaveBeenInvitedToAcsp.YouHaveBeenInvitedToAcspEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.YourRoleAtAcspHasChanged.YourRoleAtAcspHasChangedToAdminEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.YourRoleAtAcspHasChanged.YourRoleAtAcspHasChangedToOwnerEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.YourRoleAtAcspHasChanged.YourRoleAtAcspHasChangedToStandardEmailData;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static uk.gov.companieshouse.acsp.manage.users.model.enums.MessageType.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class EmailServiceTest {

    @Mock
    private EmailProducer emailProducer;

    @InjectMocks
    private EmailService emailService;

    @Value( "${signin.url}" )
    private String signinUrl;

    @Test
    void sendConfirmYouAreAMemberEmailWithNullRecipientEmailOrAddedByOrAcspNameOrRoleThrowsIllegalArgumentException(){
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendConfirmYouAreAMemberEmail(  null, "demo@ch.gov.uk", "Witcher", UserRoleEnum.OWNER ).block() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendConfirmYouAreAMemberEmail(  "buzz.lightyear@toystory.com", null, "Witcher", UserRoleEnum.OWNER ).block() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendConfirmYouAreAMemberEmail(  "buzz.lightyear@toystory.com", "demo@ch.gov.uk", null, UserRoleEnum.OWNER ).block() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendConfirmYouAreAMemberEmail(  "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", null ).block() );
    }

    @Test
    void sendConfirmYouAreAMemberEmailWithUnexpectedIssueThrowsEmailSendingException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), eq( CONFIRM_YOU_ARE_AN_OWNER_MEMBER_MESSAGE_TYPE.getValue() ) );
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), eq( CONFIRM_YOU_ARE_AN_ADMIN_MEMBER_MESSAGE_TYPE.getValue() ) );
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), eq( CONFIRM_YOU_ARE_A_STANDARD_MEMBER_MESSAGE_TYPE.getValue() ) );

        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendConfirmYouAreAMemberEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.OWNER ).block() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendConfirmYouAreAMemberEmail(  "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.ADMIN ).block() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendConfirmYouAreAMemberEmail(  "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.STANDARD ).block() );
    }

    @Test
    void sendConfirmYouAreAMemberEmailWithRoleSetToOwnerThrowsMessageOnToKafkaQueue(){
        final var expectedEmailData = new ConfirmYouAreAnOwnerMemberEmailData( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", signinUrl );
        emailService.sendConfirmYouAreAMemberEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.OWNER ).block();
        Mockito.verify( emailProducer ).sendEmail( expectedEmailData, CONFIRM_YOU_ARE_AN_OWNER_MEMBER_MESSAGE_TYPE.getValue() );
    }

    @Test
    void sendConfirmYouAreAMemberEmailWithRoleSetToAdminThrowsMessageOnToKafkaQueue(){
        final var expectedEmailData = new ConfirmYouAreAnAdminMemberEmailData( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", signinUrl );
        emailService.sendConfirmYouAreAMemberEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.ADMIN ).block();
        Mockito.verify( emailProducer ).sendEmail( expectedEmailData, CONFIRM_YOU_ARE_AN_ADMIN_MEMBER_MESSAGE_TYPE.getValue() );
    }

    @Test
    void sendConfirmYouAreAMemberEmailWithRoleSetToStandardThrowsMessageOnToKafkaQueue(){
        final var expectedEmailData = new ConfirmYouAreAStandardMemberEmailData( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", signinUrl );
        emailService.sendConfirmYouAreAMemberEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.STANDARD ).block();
        Mockito.verify( emailProducer ).sendEmail( expectedEmailData, CONFIRM_YOU_ARE_A_STANDARD_MEMBER_MESSAGE_TYPE.getValue() );
    }

    @Test
    void sendYourRoleAtAcspHasChangedEmailWithNullRecipientEmailOrEditedByOrAcspNameOrNewRoleThrowsIllegalArgumentException(){
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail( null, "demo@ch.gov.uk", "Witcher", UserRoleEnum.OWNER ).block() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail( "buzz.lightyear@toystory.com", null, "Witcher", UserRoleEnum.OWNER ).block() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", null, UserRoleEnum.OWNER ).block() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", null ).block() );
    }

    @Test
    void sendYourRoleAtAcspHasChangedEmailWithUnexpectedIssueThrowsEmailSendingException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), eq( YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_OWNER_MESSAGE_TYPE.getValue() ) );
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), eq( YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_ADMIN_MESSAGE_TYPE.getValue() ) );
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), eq( YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_STANDARD_MESSAGE_TYPE.getValue() ) );

        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.OWNER ).block() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail(  "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.ADMIN ).block() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.STANDARD ).block() );
    }

    @Test
    void sendYourRoleAtAcspHasChangedEmailWithNewRoleSetToOwnerThrowsMessageOnToKafkaQueue(){
        final var expectedEmailData = new YourRoleAtAcspHasChangedToOwnerEmailData( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", signinUrl );
        emailService.sendYourRoleAtAcspHasChangedEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.OWNER ).block();
        Mockito.verify( emailProducer ).sendEmail( expectedEmailData, YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_OWNER_MESSAGE_TYPE.getValue() );
    }

    @Test
    void sendYourRoleAtAcspHasChangedEmailWithNewRoleSetToAdminThrowsMessageOnToKafkaQueue(){
        final var expectedEmailData = new YourRoleAtAcspHasChangedToAdminEmailData( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", signinUrl );
        emailService.sendYourRoleAtAcspHasChangedEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.ADMIN ).block();
        Mockito.verify( emailProducer ).sendEmail( expectedEmailData, YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_ADMIN_MESSAGE_TYPE.getValue() );
    }

    @Test
    void sendYourRoleAtAcspHasChangedEmailWithNewRoleSetToStandardThrowsMessageOnToKafkaQueue(){
        final var expectedEmailData = new YourRoleAtAcspHasChangedToStandardEmailData( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", signinUrl );
        emailService.sendYourRoleAtAcspHasChangedEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.STANDARD ).block();
        Mockito.verify( emailProducer ).sendEmail( expectedEmailData, YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_STANDARD_MESSAGE_TYPE.getValue() );
    }

    @Test
    void sendYouHaveBeenInvitedToAcspEmailWithNullInputsThrowsIllegalArgumentException(){
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendYouHaveBeenInvitedToAcspEmail( null, "demo@ch.gov.uk", "Witcher" ) );
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendYouHaveBeenInvitedToAcspEmail( "buzz.lightyear@toystory.com", null, "Witcher" ) );
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendYouHaveBeenInvitedToAcspEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", null ) );
    }

    @Test
    void sendYouHaveBeenInvitedToAcspEmailWithUnexpectedIssueThrowsEmailSendingException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), eq( YOU_HAVE_BEEN_INVITED_TO_ACSP.getValue() ) );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendYouHaveBeenInvitedToAcspEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher" ).block() );
    }

    @Test
    void sendYouHaveBeenInvitedToAcspEmailThrowsMessageOnToKafkaQueue(){
        final var expectedEmailData = new YouHaveBeenInvitedToAcspEmailData( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", signinUrl );
        emailService.sendYouHaveBeenInvitedToAcspEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher" ).block();
        Mockito.verify( emailProducer ).sendEmail( expectedEmailData, YOU_HAVE_BEEN_INVITED_TO_ACSP.getValue() );
    }

}
