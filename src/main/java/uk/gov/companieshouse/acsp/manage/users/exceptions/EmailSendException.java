package uk.gov.companieshouse.acsp.manage.users.exceptions;

public class EmailSendException extends RuntimeException {
    public EmailSendException(String message) {
        super(message);
    }
}

