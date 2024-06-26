package uk.gov.companieshouse.acsp.manage.users.exceptions;

public class NotFoundRuntimeException extends RuntimeException {

    private final String fieldLocation;

    public NotFoundRuntimeException(String fieldLocation, String message) {
        super(message);
        this.fieldLocation = fieldLocation;
    }

    public String getFieldLocation() {
        return fieldLocation;
    }
}


