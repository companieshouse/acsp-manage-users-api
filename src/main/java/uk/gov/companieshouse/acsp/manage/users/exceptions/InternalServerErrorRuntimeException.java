package uk.gov.companieshouse.acsp.manage.users.exceptions;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil.APPLICATION_NAMESPACE;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

public class InternalServerErrorRuntimeException extends RuntimeException {

    private static final Logger LOG = LoggerFactory.getLogger( APPLICATION_NAMESPACE );

    public InternalServerErrorRuntimeException( final String exceptionMessage, final Exception loggingMessage ) {
        super( exceptionMessage );
        LOG.errorContext( getXRequestId(), loggingMessage, null );}

}


