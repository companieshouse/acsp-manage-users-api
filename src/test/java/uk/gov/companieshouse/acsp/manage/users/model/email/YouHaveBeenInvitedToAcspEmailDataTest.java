package uk.gov.companieshouse.acsp.manage.users.model.email;

import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.companieshouse.acsp.manage.users.model.email.YouHaveBeenInvitedToAcsp.YouHaveBeenInvitedToAcspEmailData;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class YouHaveBeenInvitedToAcspEmailDataTest {

    @Value( "${signin.url}" )
    private String signinUrl;

    @Test
    void canConstructEmailDataWithBuilderPatternApproach() {
        final var emailData = new YouHaveBeenInvitedToAcspEmailData()
                .to( "buzz.lightyear@toystory.com" )
                .subject( "Space Ranger Promotion" )
                .invitedBy( "Woody" )
                .acspName( "Netflix" )
                .signinUrl( signinUrl );

        Assertions.assertEquals( "buzz.lightyear@toystory.com", emailData.getTo() );
        Assertions.assertEquals( "Space Ranger Promotion", emailData.getSubject() );
        Assertions.assertEquals( "Woody", emailData.getInvitedBy() );
        Assertions.assertEquals( "Netflix", emailData.getAcspName() );
        Assertions.assertEquals( signinUrl, emailData.getSigninUrl() );
    }

    @Test
    void canConstructEmailDataWithTraditionalApproach() {
        final var emailData = new YouHaveBeenInvitedToAcspEmailData();
        emailData.setTo( "buzz.lightyear@toystory.com" );
        emailData.setSubject( "Space Ranger Promotion" );
        emailData.setInvitedBy( "Woody" );
        emailData.setAcspName( "Netflix" );
        emailData.setSigninUrl( signinUrl );

        Assertions.assertEquals( "buzz.lightyear@toystory.com", emailData.getTo() );
        Assertions.assertEquals( "Space Ranger Promotion", emailData.getSubject() );
        Assertions.assertEquals( "Woody", emailData.getInvitedBy() );
        Assertions.assertEquals( "Netflix", emailData.getAcspName() );
        Assertions.assertEquals( signinUrl, emailData.getSigninUrl() );
    }

    @Test
    void canConstructEmailDataWithConstructor() {
        final var emailData = new YouHaveBeenInvitedToAcspEmailData( "buzz.lightyear@toystory.com", "Woody", "Netflix", signinUrl );
        Assertions.assertEquals( "buzz.lightyear@toystory.com", emailData.getTo() );
        Assertions.assertEquals( "You have been invited to a Companies House authorised agent", emailData.getSubject() );
        Assertions.assertEquals( "Woody", emailData.getInvitedBy() );
        Assertions.assertEquals( "Netflix", emailData.getAcspName() );
        Assertions.assertEquals( signinUrl, emailData.getSigninUrl() );
    }

    @Test
    void subjectSetsDerivedSubject(){
        final var emailData = new YouHaveBeenInvitedToAcspEmailData()
                .to( "buzz.lightyear@toystory.com" )
                .subject( "Space Ranger Promotion" )
                .invitedBy( "Woody" )
                .acspName( "Netflix" )
                .signinUrl( signinUrl )
                .subject();

        Assertions.assertEquals( "You have been invited to a Companies House authorised agent", emailData.getSubject() );
    }

    @Test
    void equalsReturnsTrueWhenEmailDataAreEquivalentOtherwiseFalse(){
        final Supplier<YouHaveBeenInvitedToAcspEmailData> buzzEmailSupplier = () -> new YouHaveBeenInvitedToAcspEmailData( "buzz.lightyear@toystory.com", "Woody", "Netflix", signinUrl );
        final var potatoHeadEmail = new YouHaveBeenInvitedToAcspEmailData( "potato.head@toystory.com", "Woody", "Netflix", signinUrl );
        Assertions.assertEquals( buzzEmailSupplier.get(), buzzEmailSupplier.get() );
        Assertions.assertNotEquals( potatoHeadEmail, buzzEmailSupplier.get() );
    }

    @Test
    void toNotificationSentLoggingMessageComputesCorrectMessage(){
        final var message = new YouHaveBeenInvitedToAcspEmailData( "buzz.lightyear@toystory.com", "Woody", "Netflix", signinUrl ).toNotificationSentLoggingMessage();
        Assertions.assertEquals( "you_have_been_invited_to_acsp notification sent. buzz.lightyear@toystory.com was invited to Netflix by Woody.", message );
    }

}
