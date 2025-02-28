package uk.gov.companieshouse.acsp.manage.users.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.acsp.manage.users.model.email.BaseEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.ConfirmYouAreAMember.ConfirmYouAreAStandardMemberEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.ConfirmYouAreAMember.ConfirmYouAreAnAdminMemberEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.ConfirmYouAreAMember.ConfirmYouAreAnOwnerMemberEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.YourRoleAtAcspHasChanged.YourRoleAtAcspHasChangedToAdminEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.YourRoleAtAcspHasChanged.YourRoleAtAcspHasChangedToOwnerEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.YourRoleAtAcspHasChanged.YourRoleAtAcspHasChangedToStandardEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.enums.MessageType;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.email_producer.EmailProducer;
import java.util.Objects;

import static uk.gov.companieshouse.acsp.manage.users.model.enums.MessageType.*;
import static uk.gov.companieshouse.acsp.manage.users.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getXRequestId;

@Service
public class EmailService {

    @Value( "${signin.url}" )
    private String signinUrl;

    private final EmailProducer emailProducer;

    @Autowired
    public EmailService( final EmailProducer emailProducer ) {
        this.emailProducer = emailProducer;
    }

    private Mono<Void> sendEmail( final BaseEmailData<?> emailData, final MessageType messageType ){
        final var xRequestId = getXRequestId();
        return Mono.just( emailData )
                .doOnNext( email -> emailProducer.sendEmail( email, messageType.getValue() ) )
                .doOnNext( email -> LOGGER.infoContext( xRequestId, email.toNotificationSentLoggingMessage(), null ) )
                .onErrorMap( throwable -> {
                    LOGGER.errorContext( xRequestId, new Exception( emailData.toNotificationSendingFailureLoggingMessage() ), null );
                    return throwable;
                } )
                .then();
    }

    public Mono<Void> sendConfirmYouAreAMemberEmail( final String recipientEmail, final String addedBy, final String acspName, final UserRoleEnum role ){
        if ( Objects.isNull( recipientEmail ) || Objects.isNull( addedBy ) || Objects.isNull( acspName ) || Objects.isNull( role ) ){
            LOGGER.errorContext( getXRequestId(), new Exception( "Attempted to send confirm-you-are-a-member email, with null recipientEmail, null addedBy, null acspName, or null role." ), null );
            throw new IllegalArgumentException( "recipientEmail, addedBy, acspName, and role must not be null." );
        }

        final MessageType messageType;
        final var emailData =
        switch ( role ) {
            case UserRoleEnum.OWNER -> {
                messageType = CONFIRM_YOU_ARE_AN_OWNER_MEMBER_MESSAGE_TYPE;
                yield new ConfirmYouAreAnOwnerMemberEmailData( recipientEmail, addedBy, acspName, signinUrl );
            }
            case UserRoleEnum.ADMIN -> {
                messageType = CONFIRM_YOU_ARE_AN_ADMIN_MEMBER_MESSAGE_TYPE;
                yield new ConfirmYouAreAnAdminMemberEmailData( recipientEmail, addedBy, acspName, signinUrl );
            }
            case UserRoleEnum.STANDARD -> {
                messageType = CONFIRM_YOU_ARE_A_STANDARD_MEMBER_MESSAGE_TYPE;
                yield new ConfirmYouAreAStandardMemberEmailData( recipientEmail, addedBy, acspName, signinUrl );
            }
            default -> {
                LOGGER.errorContext( getXRequestId(), new Exception( String.format( "Role is invalid: %s", role.getValue() ) ), null );
                throw new IllegalArgumentException( "Role is invalid" );
            }
        };

        return sendEmail( emailData, messageType );
    }

    public Mono<Void> sendYourRoleAtAcspHasChangedEmail( final String recipientEmail, final String editedBy, final String acspName, final UserRoleEnum newRole ){
        final var xRequestId = getXRequestId();
        if ( Objects.isNull( recipientEmail ) || Objects.isNull( editedBy ) || Objects.isNull( acspName ) || Objects.isNull( newRole ) ){
            LOGGER.errorContext( xRequestId, new Exception( "Attempted to send your-role-at-acsp-has-changed email, with null recipientEmail, null editedBy, null acspName, or null newRole." ), null );
            throw new IllegalArgumentException( "recipientEmail, editedBy, acspName, and newRole must not be null." );
        }

        final MessageType messageType;
        final var emailData =
        switch ( newRole ) {
            case UserRoleEnum.OWNER -> {
                messageType = YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_OWNER_MESSAGE_TYPE;
                yield new YourRoleAtAcspHasChangedToOwnerEmailData( recipientEmail, editedBy, acspName, signinUrl );
            }
            case UserRoleEnum.ADMIN -> {
                messageType = YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_ADMIN_MESSAGE_TYPE;
                yield new YourRoleAtAcspHasChangedToAdminEmailData( recipientEmail, editedBy, acspName, signinUrl );
            }
            case UserRoleEnum.STANDARD -> {
                messageType = YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_STANDARD_MESSAGE_TYPE;
                yield new YourRoleAtAcspHasChangedToStandardEmailData( recipientEmail, editedBy, acspName, signinUrl );
            }
            default -> {
                LOGGER.errorContext( xRequestId, new Exception( String.format( "Role is invalid: %s", newRole.getValue() ) ), null );
                throw new IllegalArgumentException( "Role is invalid" );
            }
        };

        return sendEmail( emailData, messageType );
    }

}
