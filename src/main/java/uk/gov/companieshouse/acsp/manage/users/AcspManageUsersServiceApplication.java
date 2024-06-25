package uk.gov.companieshouse.acsp.manage.users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;

@SpringBootApplication
public class AcspManageUsersServiceApplication {

    StaticPropertyUtil staticPropertyUtil;
    @Autowired
    public AcspManageUsersServiceApplication(StaticPropertyUtil staticPropertyUtil) {
        this.staticPropertyUtil = staticPropertyUtil;
    }

    public static void main(String[] args) {
        SpringApplication.run(AcspManageUsersServiceApplication.class, args);
    }

}