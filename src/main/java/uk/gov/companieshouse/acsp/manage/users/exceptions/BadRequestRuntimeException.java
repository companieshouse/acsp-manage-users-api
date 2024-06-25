package uk.gov.companieshouse.acsp.manage.users.exceptions;

public class BadRequestRuntimeException extends RuntimeException {

    public BadRequestRuntimeException(String message) {
        super(message);
    }
}
