package uk.gov.companieshouse.acsp.manage.users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;

@SpringBootApplication
public class AcspManageUsersServiceApplication {

    @Value("${spring.application.name}")
    public static String applicationNameSpace;

    StaticPropertyUtil staticPropertyUtil;
    @Autowired
    public AcspManageUsersServiceApplication(StaticPropertyUtil staticPropertyUtil) {
        this.staticPropertyUtil = staticPropertyUtil;
    }

    public static void main(String[] args) {
        SpringApplication.run(AcspManageUsersServiceApplication.class, args);
    }

}