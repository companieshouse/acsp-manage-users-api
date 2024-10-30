package uk.gov.companieshouse.acsp.manage.users.model.email;

import static uk.gov.companieshouse.acsp.manage.users.model.MessageType.CONFIRM_YOU_ARE_AN_ADMIN_MEMBER_MESSAGE_TYPE;

public class ConfirmYouAreAnAdminMemberEmailData extends ConfirmYouAreAMemberEmailData {

    public ConfirmYouAreAnAdminMemberEmailData(){}

    public ConfirmYouAreAnAdminMemberEmailData( final String to, final String addedBy, final String acspName, final String signinUrl ){
        super( to, addedBy, acspName, signinUrl );
    }

    @Override
    public String toNotificationSentLoggingMessage(){
        return String.format( "%s notification sent. %s was added to %s by %s.", CONFIRM_YOU_ARE_AN_ADMIN_MEMBER_MESSAGE_TYPE.getValue(), getTo(), getAcspName(), getAddedBy() );
    }

}
