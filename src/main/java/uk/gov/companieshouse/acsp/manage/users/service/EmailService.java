package uk.gov.companieshouse.acsp.manage.users.service;

import static uk.gov.companieshouse.acsp.manage.users.model.MessageType.YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_ADMIN_MESSAGE_TYPE;
import static uk.gov.companieshouse.acsp.manage.users.model.MessageType.YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_OWNER_MESSAGE_TYPE;
import static uk.gov.companieshouse.acsp.manage.users.model.MessageType.YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_STANDARD_MESSAGE_TYPE;
import static uk.gov.companieshouse.acsp.manage.users.model.MessageType.YOU_HAVE_BEEN_ADDED_TO_ACSP_MESSAGE_TYPE;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.acsp.manage.users.model.MessageType;
import uk.gov.companieshouse.acsp.manage.users.model.email.YouHaveBeenAddedToAcspEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.YourRoleAtAcspHasChangedToAdminEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.YourRoleAtAcspHasChangedToOwnerEmailData;
import uk.gov.companieshouse.acsp.manage.users.model.email.YourRoleAtAcspHasChangedToStandardEmailData;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class EmailService {

    private static final Logger LOG = LoggerFactory.getLogger( StaticPropertyUtil.APPLICATION_NAMESPACE );

    private final EmailProducer emailProducer;

    @Autowired
    public EmailService( final EmailProducer emailProducer ) {
        this.emailProducer = emailProducer;
    }

    @Async
    public void sendYouHaveBeenAddedToAcspEmail( final String xRequestId, final String recipientEmail, final String addedBy, final String acspName ){
        if ( Objects.isNull( recipientEmail ) || Objects.isNull( addedBy ) || Objects.isNull( acspName ) ){
            LOG.error( String.format( "Attempted to send %s email, with null recipientEmail, null addedBy, or null acspName.", YOU_HAVE_BEEN_ADDED_TO_ACSP_MESSAGE_TYPE.getValue() ) );
            throw new IllegalArgumentException( "recipientEmail, addedBy, and acspName must not be null." );
        }
        final var emailData = new YouHaveBeenAddedToAcspEmailData( recipientEmail, addedBy, acspName );
        emailProducer.sendEmail( emailData, YOU_HAVE_BEEN_ADDED_TO_ACSP_MESSAGE_TYPE.getValue() );
        LOG.infoContext( xRequestId, emailData.toNotificationSentLoggingMessage(), null );
    }

    @Async
    public void sendYourRoleAtAcspHasChangedEmail( final String xRequestId, final String recipientEmail, final String editedBy, final String acspName, final UserRoleEnum newRole ){
        if ( Objects.isNull( recipientEmail ) || Objects.isNull( editedBy ) || Objects.isNull( acspName ) || Objects.isNull( newRole ) ){
            LOG.error( "Attempted to send your-role-at-acsp-has-changed email, with null recipientEmail, null editedBy, null acspName, or null newRole." );
            throw new IllegalArgumentException( "recipientEmail, editedBy, acspName, and newRole must not be null." );
        }

        final MessageType messageType;
        final var emailData =
        switch ( newRole ) {
            case UserRoleEnum.OWNER -> {
                messageType = YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_OWNER_MESSAGE_TYPE;
                yield new YourRoleAtAcspHasChangedToOwnerEmailData( recipientEmail, editedBy, acspName );
            }
            case UserRoleEnum.ADMIN -> {
                messageType = YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_ADMIN_MESSAGE_TYPE;
                yield new YourRoleAtAcspHasChangedToAdminEmailData( recipientEmail, editedBy, acspName );
            }
            case UserRoleEnum.STANDARD -> {
                messageType = YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_STANDARD_MESSAGE_TYPE;
                yield new YourRoleAtAcspHasChangedToStandardEmailData( recipientEmail, editedBy, acspName );
            }
            default -> throw new IllegalArgumentException( "Role is invalid" );
        };

        emailProducer.sendEmail( emailData, messageType.getValue() );
        LOG.infoContext( xRequestId, emailData.toNotificationSentLoggingMessage(), null );
    }

}
