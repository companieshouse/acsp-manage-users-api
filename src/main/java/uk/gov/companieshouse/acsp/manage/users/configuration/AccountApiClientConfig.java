package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.handler.accountsuser.PrivateAccountsUserResourceHandler;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;

@Configuration
public class AccountApiClientConfig {

    @Value("${account.api.url}")
    private String accountApiUrl;


    @Value("${chs.internal.api.key}")
    private String chsInternalApiKey;

    @Bean
    public PrivateAccountsUserResourceHandler getAccountUserResourceHandler(){
        final InternalApiClient internalApiClient = new InternalApiClient(new ApiKeyHttpClient(chsInternalApiKey));
        internalApiClient.setInternalBasePath(accountApiUrl);
        return internalApiClient.privateAccountsUserResourceHandler();
    }
}
