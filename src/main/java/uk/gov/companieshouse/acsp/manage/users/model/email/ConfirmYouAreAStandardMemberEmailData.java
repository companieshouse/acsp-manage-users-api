package uk.gov.companieshouse.acsp.manage.users.model.email;

import static uk.gov.companieshouse.acsp.manage.users.model.MessageType.CONFIRM_YOU_ARE_A_STANDARD_MEMBER_MESSAGE_TYPE;

public class ConfirmYouAreAStandardMemberEmailData extends ConfirmYouAreAMemberEmailData {

    public ConfirmYouAreAStandardMemberEmailData(){}

    public ConfirmYouAreAStandardMemberEmailData( final String to, final String addedBy, final String acspName ){
        super( to, addedBy, acspName );
    }

    @Override
    public String toNotificationSentLoggingMessage(){
        return String.format( "%s notification sent. %s was added to %s by %s.", CONFIRM_YOU_ARE_A_STANDARD_MEMBER_MESSAGE_TYPE.getValue(), getTo(), getAcspName(), getAddedBy() );
    }

}
