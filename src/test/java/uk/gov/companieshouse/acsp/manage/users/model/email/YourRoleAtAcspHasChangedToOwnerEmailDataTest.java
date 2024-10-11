package uk.gov.companieshouse.acsp.manage.users.model.email;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class YourRoleAtAcspHasChangedToOwnerEmailDataTest {

    @Test
    void toNotificationSentLoggingMessageComputesCorrectMessage(){
        final var message = new YourRoleAtAcspHasChangedToOwnerEmailData( "buzz.lightyear@toystory.com", "Woody", "Netflix" ).toNotificationSentLoggingMessage();
        Assertions.assertEquals( "your_role_at_acsp_has_changed_to_owner notification sent. buzz.lightyear@toystory.com role at Netflix was changed by Woody.", message );
    }

}
