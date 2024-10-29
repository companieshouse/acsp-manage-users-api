package uk.gov.companieshouse.acsp.manage.users.model.email;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class ConfirmYouAreAnOwnerMemberEmailDataTest {

    @Test
    void toNotificationSentLoggingMessageComputesCorrectMessage(){
        final var message = new ConfirmYouAreAnOwnerMemberEmailData( "buzz.lightyear@toystory.com", "Woody", "Netflix" ).toNotificationSentLoggingMessage();
        Assertions.assertEquals( "confirm_you_are_an_owner_member notification sent. buzz.lightyear@toystory.com was added to Netflix by Woody.", message );
    }

}
