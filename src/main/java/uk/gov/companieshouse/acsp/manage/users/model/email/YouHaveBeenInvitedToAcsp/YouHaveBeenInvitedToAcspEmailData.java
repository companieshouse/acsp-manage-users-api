package uk.gov.companieshouse.acsp.manage.users.model.email.YouHaveBeenInvitedToAcsp;

import static uk.gov.companieshouse.acsp.manage.users.model.enums.MessageType.YOU_HAVE_BEEN_INVITED_TO_ACSP;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import uk.gov.companieshouse.acsp.manage.users.model.email.BaseEmailData;

public class YouHaveBeenInvitedToAcspEmailData extends BaseEmailData<YouHaveBeenInvitedToAcspEmailData> {

    private String invitedBy;

    private String acspName;

    private String signinUrl;

    public YouHaveBeenInvitedToAcspEmailData(){}

    public YouHaveBeenInvitedToAcspEmailData( final String to, final String invitedBy, final String acspName, final String signinUrl ){
        setTo( to );
        this.invitedBy = invitedBy;
        this.acspName = acspName;
        this.signinUrl = signinUrl;
        setSubject();
    }

    public void setInvitedBy( final String invitedBy ){
        this.invitedBy = invitedBy;
    }

    public YouHaveBeenInvitedToAcspEmailData invitedBy( final String invitedBy ){
        setInvitedBy( invitedBy );
        return this;
    }

    public String getInvitedBy(){
        return invitedBy;
    }

    public void setAcspName( final String acspName ) {
        this.acspName = acspName;
    }

    public YouHaveBeenInvitedToAcspEmailData acspName( final String acspName ){
        setAcspName( acspName );
        return this;
    }

    public String getAcspName() {
        return acspName;
    }

    public void setSigninUrl( final String signinUrl ){
        this.signinUrl = signinUrl;
    }

    public YouHaveBeenInvitedToAcspEmailData signinUrl( final String signinUrl ){
        setSigninUrl( signinUrl );
        return this;
    }

    public String getSigninUrl(){
        return signinUrl;
    }

    @Override
    protected YouHaveBeenInvitedToAcspEmailData self(){
        return this;
    }

    @Override
    public void setSubject(){
        setSubject( "You have been invited to a Companies House authorised agent" );
    }

    @Override
    public boolean equals( final Object o ){
        if ( this == o ) {
            return true;
        }

        if ( !( o instanceof YouHaveBeenInvitedToAcspEmailData that ) ) {
            return false;
        }

        return new EqualsBuilder()
                .append( getTo(), that.getTo() )
                .append( getSubject(), that.getSubject() )
                .append( getInvitedBy(), that.getInvitedBy() )
                .append( getAcspName(), that.getAcspName() )
                .append( getSigninUrl(), that.getSigninUrl() )
                .isEquals();
    }

    @Override
    public int hashCode(){
        return new HashCodeBuilder( 17, 37 )
                .append( getTo() )
                .append( getSubject() )
                .append( getInvitedBy() )
                .append( getAcspName() )
                .append( getSigninUrl() )
                .toHashCode();
    }

    @Override
    public String toString() {
        return "YouHaveBeenInvitedToAcspEmailData{" +
                "invitedBy='" + invitedBy + '\'' +
                ", acspName='" + acspName + '\'' +
                ", signinUrl='" + signinUrl + '\'' +
                ", to='" + getTo() + '\'' +
                ", subject='" + getSubject() + '\'' +
                '}';
    }

    @Override
    public String toNotificationSentLoggingMessage(){
        return String.format( "%s notification sent. %s was invited to %s by %s.", YOU_HAVE_BEEN_INVITED_TO_ACSP.getValue(), getTo(), getAcspName(), getInvitedBy() );
    }

    @Override
    public String toNotificationSendingFailureLoggingMessage(){
        return String.format( "Failed to send %s notification. Details: to=%s, acspName=%s, invitedBy=%s.", YOU_HAVE_BEEN_INVITED_TO_ACSP.getValue(), getTo(), getAcspName(), getInvitedBy() );
    }

}
