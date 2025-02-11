package uk.gov.companieshouse.acsp.manage.users.exceptions;

public class ForbiddenRuntimeException extends RuntimeException {

    public ForbiddenRuntimeException( final String message ) {
        super( message );
    }
}
