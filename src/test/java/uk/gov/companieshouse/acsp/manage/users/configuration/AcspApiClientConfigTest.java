package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.junit.jupiter.api.Assertions;
import uk.gov.companieshouse.api.handler.acspprofile.PrivateAcspProfileResourceHandler;

public class AcspApiClientConfigTest {

    void getAcspResourceHAndlerIsCorrectType(){
        Assertions.assertEquals( PrivateAcspProfileResourceHandler.class, new AcspApiClientConfig().getAcspResourceHAndler() );

    }

}
