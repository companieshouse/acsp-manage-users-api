package uk.gov.companieshouse.acsp.manage.users.ascpprofile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@org.springframework.context.annotation.Configuration
public class AcspWebClientConfig {

    @Value( "${api.url}" )
    private String apiUrl;

    @Value( "${chs.internal.api.key}" )
    private String chsInternalApiKey;

    @Bean
    public WebClient acspWebClient(){
        return WebClient.builder()
                .baseUrl( apiUrl )
                .defaultHeader( "Authorization", chsInternalApiKey )
                .build();
    }

}
