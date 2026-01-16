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
import uk.gov.companieshouse.acsp.manage.users.client.EmailClient;
import uk.gov.companieshouse.acsp.manage.users.exceptions.EmailSendException;
import uk.gov.companieshouse.acsp.manage.users.factory.SendEmailFactory;
import uk.gov.companieshouse.acsp.manage.users.model.email.confirmyouareamember.ConfirmYouAreAStandardMemberEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.confirmyouareamember.ConfirmYouAreAnAdminMemberEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.confirmyouareamember.ConfirmYouAreAnOwnerMemberEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.data.BaseEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.yourroleatacsphaschanged.YourRoleAtAcspHasChangedToAdminEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.yourroleatacsphaschanged.YourRoleAtAcspHasChangedToOwnerEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.yourroleatacsphaschanged.YourRoleAtAcspHasChangedToStandardEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.enums.MessageType;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.chskafka.SendEmail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static uk.gov.companieshouse.acsp.manage.users.model.enums.MessageType.CONFIRM_YOU_ARE_AN_ADMIN_MEMBER_MESSAGE_TYPE;
import static uk.gov.companieshouse.acsp.manage.users.model.enums.MessageType.CONFIRM_YOU_ARE_AN_OWNER_MEMBER_MESSAGE_TYPE;
import static uk.gov.companieshouse.acsp.manage.users.model.enums.MessageType.CONFIRM_YOU_ARE_A_STANDARD_MEMBER_MESSAGE_TYPE;
import static uk.gov.companieshouse.acsp.manage.users.model.enums.MessageType.YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_ADMIN_MESSAGE_TYPE;
import static uk.gov.companieshouse.acsp.manage.users.model.enums.MessageType.YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_OWNER_MESSAGE_TYPE;
import static uk.gov.companieshouse.acsp.manage.users.model.enums.MessageType.YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_STANDARD_MESSAGE_TYPE;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class EmailServiceTest {
    @Mock
    private EmailClient emailClient;

    @InjectMocks
    private EmailService emailService;

    @Value( "${signin.url}" )
    private String signinUrl;

    @Mock
    private SendEmailFactory sendEmailFactory;

    @Test
    void sendConfirmYouAreAMemberEmailWithNullRecipientEmailOrAddedByOrAcspNameOrRoleThrowsIllegalArgumentException(){
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendConfirmYouAreAMemberEmail(  null, "demo@ch.gov.uk", "Witcher", UserRoleEnum.OWNER ).block() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendConfirmYouAreAMemberEmail(  "buzz.lightyear@toystory.com", null, "Witcher", UserRoleEnum.OWNER ).block() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendConfirmYouAreAMemberEmail(  "buzz.lightyear@toystory.com", "demo@ch.gov.uk", null, UserRoleEnum.OWNER ).block() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendConfirmYouAreAMemberEmail(  "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", null ).block() );
    }

    @Test
    void sendConfirmYouAreAMemberEmailWithUnexpectedIssueThrowsEmailSendException() {
        Mockito.doThrow(new EmailSendException("Failed to send email")).when(sendEmailFactory).createSendEmail(any(), eq(CONFIRM_YOU_ARE_AN_OWNER_MEMBER_MESSAGE_TYPE.getValue()));
        Mockito.doThrow(new EmailSendException("Failed to send email")).when(sendEmailFactory).createSendEmail(any(), eq(CONFIRM_YOU_ARE_AN_ADMIN_MEMBER_MESSAGE_TYPE.getValue()));
        Mockito.doThrow(new EmailSendException("Failed to send email")).when(sendEmailFactory).createSendEmail(any(), eq(CONFIRM_YOU_ARE_A_STANDARD_MEMBER_MESSAGE_TYPE.getValue()));

        Assertions.assertThrows(EmailSendException.class, () -> emailService.sendConfirmYouAreAMemberEmail("buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.OWNER).block());
        Assertions.assertThrows(EmailSendException.class, () -> emailService.sendConfirmYouAreAMemberEmail("buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.ADMIN).block());
        Assertions.assertThrows(EmailSendException.class, () -> emailService.sendConfirmYouAreAMemberEmail("buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.STANDARD).block());
    }

    @Test
    void sendConfirmYouAreAMemberEmailWithRoleSetToOwnerThrowsMessageOnToCHSKafkaAPI() {
        final var expectedEmailData = new ConfirmYouAreAnOwnerMemberEmailData("buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", signinUrl);
        SendEmail mockSendEmail = getMockSendEmail(CONFIRM_YOU_ARE_AN_OWNER_MEMBER_MESSAGE_TYPE);

        emailService.sendConfirmYouAreAMemberEmail("buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.OWNER).block();
        Mockito.verify(sendEmailFactory).createSendEmail(expectedEmailData, CONFIRM_YOU_ARE_AN_OWNER_MEMBER_MESSAGE_TYPE.getValue());
        Mockito.verify(emailClient).sendEmail(eq(mockSendEmail), anyString());

    }

    @Test
    void sendConfirmYouAreAMemberEmailWithRoleSetToAdminThrowsMessageOnToCHSKafkaAPI() {
        final var expectedEmailData = new ConfirmYouAreAnAdminMemberEmailData( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", signinUrl );
        SendEmail mockSendEmail = getMockSendEmail(CONFIRM_YOU_ARE_AN_ADMIN_MEMBER_MESSAGE_TYPE);
        emailService.sendConfirmYouAreAMemberEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.ADMIN ).block();
        Mockito.verify(sendEmailFactory).createSendEmail(expectedEmailData, CONFIRM_YOU_ARE_AN_ADMIN_MEMBER_MESSAGE_TYPE.getValue());
        Mockito.verify(emailClient).sendEmail(eq(mockSendEmail), anyString());

    }

    @Test
    void sendConfirmYouAreAMemberEmailWithRoleSetToStandardThrowsMessageOnToCHSKafkaAPI() {
        SendEmail mockSendEmail = getMockSendEmail(CONFIRM_YOU_ARE_A_STANDARD_MEMBER_MESSAGE_TYPE);
        final var expectedEmailData = new ConfirmYouAreAStandardMemberEmailData( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", signinUrl );
        emailService.sendConfirmYouAreAMemberEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.STANDARD ).block();
        Mockito.verify(sendEmailFactory).createSendEmail(expectedEmailData, CONFIRM_YOU_ARE_A_STANDARD_MEMBER_MESSAGE_TYPE.getValue());
        Mockito.verify(emailClient).sendEmail(eq(mockSendEmail), anyString());
    }

    private SendEmail getMockSendEmail(MessageType messageType) {
        SendEmail mockSendEmail = Mockito.mock(SendEmail.class);
        Mockito.when(sendEmailFactory.createSendEmail(Mockito.any(BaseEmailData.class), eq(messageType.getValue())))
                .thenReturn(mockSendEmail);
        return mockSendEmail;
    }

    @Test
    void sendYourRoleAtAcspHasChangedEmailWithNullRecipientEmailOrEditedByOrAcspNameOrNewRoleThrowsIllegalArgumentException(){
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail( null, "demo@ch.gov.uk", "Witcher", UserRoleEnum.OWNER ).block() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail( "buzz.lightyear@toystory.com", null, "Witcher", UserRoleEnum.OWNER ).block() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", null, UserRoleEnum.OWNER ).block() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", null ).block() );
    }

    @Test
    void sendYourRoleAtAcspHasChangedEmailWithUnexpectedIssueThrowsEmailSendException() {
        Mockito.doThrow(new EmailSendException("Failed to send email")).when(sendEmailFactory).createSendEmail(any(), eq(YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_OWNER_MESSAGE_TYPE.getValue()));
        Mockito.doThrow(new EmailSendException("Failed to send email")).when(sendEmailFactory).createSendEmail(any(), eq(YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_ADMIN_MESSAGE_TYPE.getValue()));
        Mockito.doThrow(new EmailSendException("Failed to send email")).when(sendEmailFactory).createSendEmail(any(), eq(YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_STANDARD_MESSAGE_TYPE.getValue()));

        Assertions.assertThrows(EmailSendException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail("buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.OWNER).block());
        Assertions.assertThrows(EmailSendException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail("buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.ADMIN).block());
        Assertions.assertThrows(EmailSendException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail("buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.STANDARD).block());
    }

    @Test
    void sendYourRoleAtAcspHasChangedEmailWithNewRoleSetToOwnerThrowsMessageOnToCHSKafkaAPI() {
        SendEmail mockSendEmail = getMockSendEmail(YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_OWNER_MESSAGE_TYPE);
        final var expectedEmailData = new YourRoleAtAcspHasChangedToOwnerEmailData( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", signinUrl );
        emailService.sendYourRoleAtAcspHasChangedEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.OWNER ).block();
        Mockito.verify(sendEmailFactory).createSendEmail(expectedEmailData, YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_OWNER_MESSAGE_TYPE.getValue());
        Mockito.verify(emailClient).sendEmail(eq(mockSendEmail), anyString());

    }

    @Test
    void sendYourRoleAtAcspHasChangedEmailWithNewRoleSetToAdminThrowsMessageOnToCHSKafkaAPI() {
        SendEmail mockSendEmail = getMockSendEmail(YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_ADMIN_MESSAGE_TYPE);
        final var expectedEmailData = new YourRoleAtAcspHasChangedToAdminEmailData( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", signinUrl );
        emailService.sendYourRoleAtAcspHasChangedEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.ADMIN ).block();
        Mockito.verify(sendEmailFactory).createSendEmail(expectedEmailData, YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_ADMIN_MESSAGE_TYPE.getValue());
        Mockito.verify(emailClient).sendEmail(eq(mockSendEmail), anyString());

    }

    @Test
    void sendYourRoleAtAcspHasChangedEmailWithNewRoleSetToStandardThrowsMessageOnToCHSKafkaAPI() {
        SendEmail mockSendEmail = getMockSendEmail(YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_STANDARD_MESSAGE_TYPE);
        final var expectedEmailData = new YourRoleAtAcspHasChangedToStandardEmailData( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", signinUrl );
        emailService.sendYourRoleAtAcspHasChangedEmail( "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.STANDARD ).block();
        Mockito.verify(sendEmailFactory).createSendEmail(expectedEmailData, YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_STANDARD_MESSAGE_TYPE.getValue());
        Mockito.verify(emailClient).sendEmail(eq(mockSendEmail), anyString());

    }

}
