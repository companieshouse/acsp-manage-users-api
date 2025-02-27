package uk.gov.companieshouse.acsp.manage.users.ascpprofile;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

@Tag("unit-test")
class AcspWebClientConfigTest {

    @Test
    void webClientIsCreatedCorrectly(){
        Assertions.assertTrue( WebClient.class.isAssignableFrom( new AcspWebClientConfig().acspWebClient().getClass() ) );
    }

}
