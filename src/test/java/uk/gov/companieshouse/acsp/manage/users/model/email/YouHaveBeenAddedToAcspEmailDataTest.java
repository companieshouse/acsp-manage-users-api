package uk.gov.companieshouse.acsp.manage.users.model.email;

import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class YouHaveBeenAddedToAcspEmailDataTest {

    @Test
    void canConstructEmailDataWithBuilderPatternApproach() {
        final var emailData = new YouHaveBeenAddedToAcspEmailData()
                .to( "buzz.lightyear@toystory.com" )
                .subject( "Space Ranger Promotion" )
                .addedBy( "Woody" )
                .acspName( "Netflix" );

        Assertions.assertEquals( "buzz.lightyear@toystory.com", emailData.getTo() );
        Assertions.assertEquals( "Space Ranger Promotion", emailData.getSubject() );
        Assertions.assertEquals( "Woody", emailData.getAddedBy() );
        Assertions.assertEquals( "Netflix", emailData.getAcspName() );
    }

    @Test
    void canConstructEmailDataWithTraditionalApproach() {
        final var emailData = new YouHaveBeenAddedToAcspEmailData();
        emailData.setTo( "buzz.lightyear@toystory.com" );
        emailData.setSubject( "Space Ranger Promotion" );
        emailData.setAddedBy( "Woody" );
        emailData.setAcspName( "Netflix" );

        Assertions.assertEquals( "buzz.lightyear@toystory.com", emailData.getTo() );
        Assertions.assertEquals( "Space Ranger Promotion", emailData.getSubject() );
        Assertions.assertEquals( "Woody", emailData.getAddedBy() );
        Assertions.assertEquals( "Netflix", emailData.getAcspName() );
    }

    @Test
    void canConstructEmailDataWithConstructor() {
        final var emailData = new YouHaveBeenAddedToAcspEmailData( "buzz.lightyear@toystory.com", "Woody", "Netflix" );
        Assertions.assertEquals( "buzz.lightyear@toystory.com", emailData.getTo() );
        Assertions.assertEquals( "You have been added as a member of a Companies House authorised agent", emailData.getSubject() );
        Assertions.assertEquals( "Woody", emailData.getAddedBy() );
        Assertions.assertEquals( "Netflix", emailData.getAcspName() );
    }

    @Test
    void subjectSetsDerivedSubject(){
        final var emailData = new YouHaveBeenAddedToAcspEmailData().subject();
        Assertions.assertEquals( "You have been added as a member of a Companies House authorised agent", emailData.getSubject() );
    }

    @Test
    void equalsReturnsTrueWhenEmailDataAreEquivalentOtherwiseFalse(){
        final Supplier<YouHaveBeenAddedToAcspEmailData> buzzEmailSupplier = () -> new YouHaveBeenAddedToAcspEmailData( "buzz.lightyear@toystory.com", "Woody", "Netflix" );
        final var potatoHeadEmail = new YouHaveBeenAddedToAcspEmailData( "potato.head@toystory.com", "Woody", "Netflix" );
        Assertions.assertEquals( buzzEmailSupplier.get(), buzzEmailSupplier.get() );
        Assertions.assertNotEquals( potatoHeadEmail, buzzEmailSupplier.get() );
    }

    @Test
    void toNotificationSentLoggingMessageComputesCorrectMessage(){
        final var message = new YouHaveBeenAddedToAcspEmailData( "buzz.lightyear@toystory.com", "Woody", "Netflix" ).toNotificationSentLoggingMessage();
        Assertions.assertEquals( "you_have_been_added_to_acsp notification sent. buzz.lightyear@toystory.com was added to Netflix by Woody.", message );
    }

}
