package uk.gov.companieshouse.acsp.manage.users.model;

public enum MessageType {

    YOU_HAVE_BEEN_ADDED_TO_ACSP_MESSAGE_TYPE( "you_have_been_added_to_acsp" ),

    YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_STANDARD_MESSAGE_TYPE( "your_role_at_acsp_has_changed_to_standard" ),

    YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_ADMIN_MESSAGE_TYPE( "your_role_at_acsp_has_changed_to_admin" ),

    YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_OWNER_MESSAGE_TYPE( "your_role_at_acsp_has_changed_to_owner" );

    private final String value;

    MessageType( final String messageType ) {
        this.value = messageType;
    }

    public String getValue() {
        return value;
    }

}
