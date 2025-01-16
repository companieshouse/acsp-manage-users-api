package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.handler.acspprofile.PrivateAcspProfileResourceHandler;

@Tag("unit-test")
class AcspApiClientConfigTest {

    @Test
    void getAcspResourceHAndlerIsCorrectType(){
        Assertions.assertEquals( PrivateAcspProfileResourceHandler.class, new AcspApiClientConfig().getAcspResourceHAndler().getClass() );

    }

}
