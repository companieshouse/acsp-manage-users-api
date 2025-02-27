package uk.gov.companieshouse.acsp.manage.users.common.model.exceptions;

import static uk.gov.companieshouse.acsp.manage.users.common.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.acsp.manage.users.common.utils.StaticPropertyUtil.APPLICATION_NAMESPACE;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

public class NotFoundRuntimeException extends RuntimeException {

    private static final Logger LOG = LoggerFactory.getLogger( APPLICATION_NAMESPACE );

    public NotFoundRuntimeException( final String exceptionMessage, final Exception loggingMessage ) {
        super( exceptionMessage );
        LOG.errorContext( getXRequestId(), loggingMessage, null );
    }

}


