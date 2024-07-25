package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.InternalApiClient;

@Tag("unit-test")
class ApiClientConfigTest {

    @Test
    void internalApiClientIsCorrectType(){
        Assertions.assertEquals( InternalApiClient.class, new ApiClientConfig().getInternalApiClient().getClass() );
    }

}
