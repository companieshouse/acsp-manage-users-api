package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

@Tag("unit-test")
class ReactiveAcspWebClientConfigTest {

    @Test
    void webClientIsCreatedCorrectly(){
        Assertions.assertTrue( WebClient.class.isAssignableFrom( new ReactiveAcspWebClientConfig().acspWebClient().getClass() ) );
    }

}
