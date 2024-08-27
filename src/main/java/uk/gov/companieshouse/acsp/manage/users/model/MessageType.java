package uk.gov.companieshouse.acsp.manage.users.model;

public enum MessageType {

    YOU_HAVE_BEEN_ADDED_TO_ACSP_MESSAGE_TYPE( "you_have_been_added_to_acsp" );

    private final String value;

    MessageType( final String messageType ) {
        this.value = messageType;
    }

    public String getValue() {
        return value;
    }

}
