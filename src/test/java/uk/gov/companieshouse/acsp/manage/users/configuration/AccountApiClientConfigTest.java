package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.handler.accountsuser.PrivateAccountsUserResourceHandler;

@Tag("unit-test")
public class AccountApiClientConfigTest {

    @Test
    void getAccountUserResourceHandlerIsCorrectType(){
        Assertions.assertEquals( PrivateAccountsUserResourceHandler.class, new AccountApiClientConfig().getAccountUserResourceHandler().getClass() );
    }
    
}
