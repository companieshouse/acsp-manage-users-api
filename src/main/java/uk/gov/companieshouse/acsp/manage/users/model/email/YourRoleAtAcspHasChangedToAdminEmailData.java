package uk.gov.companieshouse.acsp.manage.users.model.email;

import static uk.gov.companieshouse.acsp.manage.users.model.MessageType.YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_ADMIN_MESSAGE_TYPE;

public class YourRoleAtAcspHasChangedToAdminEmailData extends YourRoleAtAcspHasChangedEmailData {

    public YourRoleAtAcspHasChangedToAdminEmailData(){}

    public YourRoleAtAcspHasChangedToAdminEmailData( final String to, final String editedBy, final String acspName ){
        super( to, editedBy, acspName );
    }

    @Override
    public String toNotificationSentLoggingMessage(){
        return String.format( "%s notification sent. %s role at %s was changed by %s.", YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_ADMIN_MESSAGE_TYPE.getValue(), getTo(), getAcspName(), getEditedBy() );
    }

}
