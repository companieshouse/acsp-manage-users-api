package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

@Tag("unit-test")
class MongoRoleWritingConverterTest {

    @Test
    void convertTransformsStringToUserRoleEnum(){
        final var mongoRoleWritingConverter = new MongoRoleWritingConverter();
        Assertions.assertEquals( "owner", mongoRoleWritingConverter.convert( UserRoleEnum.OWNER ) );
        Assertions.assertEquals( "admin", mongoRoleWritingConverter.convert( UserRoleEnum.ADMIN ) );
        Assertions.assertEquals( "standard", mongoRoleWritingConverter.convert( UserRoleEnum.STANDARD ) );
    }

}
