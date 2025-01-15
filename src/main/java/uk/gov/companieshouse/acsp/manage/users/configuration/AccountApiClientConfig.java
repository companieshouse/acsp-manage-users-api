package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.handler.accountsuser.PrivateAccountsUserResourceHandler;

@Configuration
public class AccountApiClientConfig {

    @Value("${account.api.url}")
    private String accountApiUrl;

    @Bean
    public PrivateAccountsUserResourceHandler getAccountUserResourceHandler(){
        final InternalApiClient internalApiClient = ApiClientConfig.getInternalApiClient();
        internalApiClient.setInternalBasePath(accountApiUrl);
        return internalApiClient.privateAccountsUserResourceHandler();
    }
}
