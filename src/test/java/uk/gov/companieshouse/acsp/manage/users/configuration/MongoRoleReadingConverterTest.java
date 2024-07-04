package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

@Tag("unit-test")
class MongoRoleReadingConverterTest {

    @Test
    void convertTransformsStringToUserRoleEnum(){
        final var mongoRoleReadingConverter = new MongoRoleReadingConverter();
        Assertions.assertEquals( UserRoleEnum.OWNER, mongoRoleReadingConverter.convert( "owner" ) );
        Assertions.assertEquals( UserRoleEnum.ADMIN, mongoRoleReadingConverter.convert( "admin" ) );
        Assertions.assertEquals( UserRoleEnum.STANDARD, mongoRoleReadingConverter.convert( "standard" ) );
    }

}
