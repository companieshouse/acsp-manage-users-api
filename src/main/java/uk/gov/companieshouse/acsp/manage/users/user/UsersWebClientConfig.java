package uk.gov.companieshouse.acsp.manage.users.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@org.springframework.context.annotation.Configuration
public class UsersWebClientConfig {

    @Value( "${account.api.url}" )
    private String accountApiUrl;

    @Value( "${chs.internal.api.key}" )
    private String chsInternalApiKey;

    @Bean
    public WebClient usersWebClient(){
        return WebClient.builder()
                .baseUrl( accountApiUrl )
                .defaultHeader( "Authorization", chsInternalApiKey )
                .build();
    }

}
