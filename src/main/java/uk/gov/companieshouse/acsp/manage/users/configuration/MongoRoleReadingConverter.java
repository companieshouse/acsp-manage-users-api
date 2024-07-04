package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

@ReadingConverter
public class MongoRoleReadingConverter implements Converter<String, UserRoleEnum> {

    @Override
    public UserRoleEnum convert( String role ) {
        return UserRoleEnum.fromValue( role );
    }

}
