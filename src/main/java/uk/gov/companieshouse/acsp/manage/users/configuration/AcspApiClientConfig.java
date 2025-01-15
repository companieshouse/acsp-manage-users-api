package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.handler.acspprofile.PrivateAcspProfileResourceHandler;

@Configuration
public class AcspApiClientConfig {


    @Value("${api.url}")
    private String apiUrl;


    @Bean
    public PrivateAcspProfileResourceHandler getAcspResourceHAndler() {
        final InternalApiClient internalApiClient = ApiClientConfig.getInternalApiClient();
        internalApiClient.setInternalBasePath(apiUrl);
        return internalApiClient.privateAcspProfileResourceHandler();
    }
}
