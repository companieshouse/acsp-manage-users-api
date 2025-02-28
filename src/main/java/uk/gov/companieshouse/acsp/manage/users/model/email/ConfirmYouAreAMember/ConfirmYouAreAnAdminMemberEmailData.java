package uk.gov.companieshouse.acsp.manage.users.model.email.ConfirmYouAreAMember;

import static uk.gov.companieshouse.acsp.manage.users.model.enums.MessageType.CONFIRM_YOU_ARE_AN_ADMIN_MEMBER_MESSAGE_TYPE;

import uk.gov.companieshouse.acsp.manage.users.model.email.ConfirmYouAreAMember.ConfirmYouAreAMemberEmailData;

public class ConfirmYouAreAnAdminMemberEmailData extends ConfirmYouAreAMemberEmailData {

    public ConfirmYouAreAnAdminMemberEmailData(){}

    public ConfirmYouAreAnAdminMemberEmailData( final String to, final String addedBy, final String acspName, final String signinUrl ){
        super( to, addedBy, acspName, signinUrl );
    }

    @Override
    public String toNotificationSentLoggingMessage(){
        return String.format( "%s notification sent. %s was added to %s by %s.", CONFIRM_YOU_ARE_AN_ADMIN_MEMBER_MESSAGE_TYPE.getValue(), getTo(), getAcspName(), getAddedBy() );
    }

    @Override
    public String toNotificationSendingFailureLoggingMessage(){
        return String.format( "Failed to send %s notification. Details: to=%s, acspName=%s, addedBy=%s.", CONFIRM_YOU_ARE_AN_ADMIN_MEMBER_MESSAGE_TYPE.getValue(), getTo(), getAcspName(), getAddedBy() );
    }

}
