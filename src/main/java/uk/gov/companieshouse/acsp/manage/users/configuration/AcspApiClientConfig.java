package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.handler.acspprofile.PrivateAcspProfileResourceHandler;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;

@Configuration
public class AcspApiClientConfig {


    @Value("${api.url}")
    private String apiUrl;

    @Value("${chs.internal.api.key}")
    private String chsInternalApiKey;

    @Bean
    public PrivateAcspProfileResourceHandler getAcspResourceHAndler() {
        final InternalApiClient internalApiClient = new InternalApiClient(new ApiKeyHttpClient(chsInternalApiKey));
        internalApiClient.setInternalBasePath(apiUrl);
        return internalApiClient.privateAcspProfileResourceHandler();
    }
}
