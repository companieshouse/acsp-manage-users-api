package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.acsp.manage.users.interceptor.AuthorizationAndInternalUserInterceptors;
import uk.gov.companieshouse.acsp.manage.users.interceptor.AuthorizationInterceptor;
import uk.gov.companieshouse.acsp.manage.users.interceptor.LoggingInterceptor;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    private static final String OAUTH_PROTECTED_ENDPOINTS = "/user/**";
    private static final String OAUTH_AND_KEY_PROTECTED_ENDPOINTS = "/acsps/**";
    private static final String HEALTH_CHECK_ENDPOINT = "/*/healthcheck";

    private final LoggingInterceptor loggingInterceptor;
    private final AuthorizationInterceptor authorizationInterceptor;

    private final AuthorizationAndInternalUserInterceptors authorizationAndInternalUserInterceptors;

    public InterceptorConfig(final LoggingInterceptor loggingInterceptor, final AuthorizationInterceptor authorizationInterceptor, final AuthorizationAndInternalUserInterceptors authorizationAndInternalUserInterceptors) {
        this.loggingInterceptor = loggingInterceptor;
        this.authorizationInterceptor = authorizationInterceptor;
        this.authorizationAndInternalUserInterceptors = authorizationAndInternalUserInterceptors;
    }

    @Override
    public void addInterceptors(@NonNull final InterceptorRegistry registry) {
        addLoggingInterceptor(registry);
        addEricInterceptors(registry);
    }

    private void addLoggingInterceptor(final InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor);
    }

    private void addEricInterceptors(final InterceptorRegistry registry) {
    registry.addInterceptor( authorizationInterceptor )
            .addPathPatterns( OAUTH_PROTECTED_ENDPOINTS )
            .excludePathPatterns( HEALTH_CHECK_ENDPOINT, OAUTH_AND_KEY_PROTECTED_ENDPOINTS );

        registry.addInterceptor( authorizationAndInternalUserInterceptors )
                .addPathPatterns( OAUTH_AND_KEY_PROTECTED_ENDPOINTS )
                .excludePathPatterns( HEALTH_CHECK_ENDPOINT, OAUTH_PROTECTED_ENDPOINTS );
    }

}
