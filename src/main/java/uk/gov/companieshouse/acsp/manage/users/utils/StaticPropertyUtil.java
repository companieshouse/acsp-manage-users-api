package uk.gov.companieshouse.acsp.manage.users.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StaticPropertyUtil {

    public static String APPLICATION_NAMESPACE;

    private StaticPropertyUtil( @Value( "${spring.application.name}" ) final String applicationNameSpace ) {
        StaticPropertyUtil.APPLICATION_NAMESPACE = applicationNameSpace;
    }

}
