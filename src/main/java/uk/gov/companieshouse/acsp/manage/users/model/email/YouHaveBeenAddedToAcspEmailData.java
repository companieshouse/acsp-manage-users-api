package uk.gov.companieshouse.acsp.manage.users.model.email;

import static uk.gov.companieshouse.acsp.manage.users.model.MessageType.YOU_HAVE_BEEN_ADDED_TO_ACSP_MESSAGE_TYPE;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class YouHaveBeenAddedToAcspEmailData extends BaseEmailData<YouHaveBeenAddedToAcspEmailData> {

    private String addedBy;

    private String acspName;

    public YouHaveBeenAddedToAcspEmailData(){}

    public YouHaveBeenAddedToAcspEmailData( final String to, final String addedBy, final String acspName ) {
        setTo( to );
        this.addedBy = addedBy;
        this.acspName = acspName;
        setSubject();
    }

    public void setAddedBy( final String addedBy ) {
        this.addedBy = addedBy;
    }

    public YouHaveBeenAddedToAcspEmailData addedBy( final String addedBy ){
        setAddedBy( addedBy );
        return this;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAcspName( final String acspName ) {
        this.acspName = acspName;
    }

    public YouHaveBeenAddedToAcspEmailData acspName( final String acspName ){
        setAcspName( acspName );
        return this;
    }

    public String getAcspName() {
        return acspName;
    }

    @Override
    protected YouHaveBeenAddedToAcspEmailData self(){
        return this;
    }

    @Override
    public void setSubject(){
        setSubject( "You have been added as a member of a Companies House authorised agent" );
    }

    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }

        if ( !( o instanceof YouHaveBeenAddedToAcspEmailData that ) ) {
            return false;
        }

        return new EqualsBuilder()
                .append( getAddedBy(), that.getAddedBy() )
                .append( getAcspName(), that.getAcspName() )
                .append( getTo(), that.getTo() )
                .append( getSubject(), that.getSubject() )
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append( getAddedBy() )
                .append( getAcspName() )
                .append( getTo() )
                .append( getSubject() )
                .toHashCode();
    }

    @Override
    public String toString() {
        return "YouHaveBeenAddedToAcspEmailData{" +
                "addedBy='" + addedBy + '\'' +
                ", acspName='" + acspName + '\'' +
                ", to='" + getTo() + '\'' +
                ", subject='" + getSubject() + '\'' +
                '}';
    }

    @Override
    public String toNotificationSentLoggingMessage(){
        return String.format( "%s notification sent. %s was added to %s by %s.", YOU_HAVE_BEEN_ADDED_TO_ACSP_MESSAGE_TYPE.getValue(), getTo(), getAcspName(), getAddedBy() );
    }

}
