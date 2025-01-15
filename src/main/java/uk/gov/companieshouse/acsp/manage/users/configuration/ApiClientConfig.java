package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;

@Configuration
public class ApiClientConfig {

    @Value("${chs.internal.api.key}")
    private static String internalApiKey;

    public static InternalApiClient getInternalApiClient(){
        return new InternalApiClient(new ApiKeyHttpClient(internalApiKey));
    }

}
