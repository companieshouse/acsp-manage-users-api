package uk.gov.companieshouse.acsp.manage.users.service;

import static uk.gov.companieshouse.acsp.manage.users.model.MessageType.YOU_HAVE_BEEN_ADDED_TO_ACSP_MESSAGE_TYPE;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.acsp.manage.users.model.email.YouHaveBeenAddedToAcspEmailData;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
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

}
