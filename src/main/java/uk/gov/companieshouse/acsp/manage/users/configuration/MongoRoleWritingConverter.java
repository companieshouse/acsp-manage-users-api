package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

@WritingConverter
public class MongoRoleWritingConverter implements Converter<UserRoleEnum, String> {

    @Override
    public String convert( UserRoleEnum role ) {
        return role.getValue();
    }

}
