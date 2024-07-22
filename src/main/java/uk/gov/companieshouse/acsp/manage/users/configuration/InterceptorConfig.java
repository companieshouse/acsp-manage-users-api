package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.acsp.manage.users.interceptor.AuthorizationInterceptor;
import uk.gov.companieshouse.acsp.manage.users.interceptor.CompositeInterceptor;
import uk.gov.companieshouse.acsp.manage.users.interceptor.LoggingInterceptor;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    private static final String OAUTH_PROTECTED_ENDPOINTS = "/acsp-members/user/**";
    private static final String OAUTH_AND_KEY_PROTECTED_ENDPOINTS = "/acsp-members/acsps/**";
    private static final String HEALTH_CHECK_ENDPOINT = "/*/healthcheck";

    private final LoggingInterceptor loggingInterceptor;
    private final AuthorizationInterceptor authorizationInterceptor;

    public InterceptorConfig(final LoggingInterceptor loggingInterceptor, AuthorizationInterceptor authorizationInterceptor) {
        this.loggingInterceptor = loggingInterceptor;
        this.authorizationInterceptor = authorizationInterceptor;
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

        registry.addInterceptor( new CompositeInterceptor( authorizationInterceptor, new InternalUserInterceptor( StaticPropertyUtil.APPLICATION_NAMESPACE ) ) )
                .addPathPatterns( OAUTH_AND_KEY_PROTECTED_ENDPOINTS )
                .excludePathPatterns( HEALTH_CHECK_ENDPOINT, OAUTH_PROTECTED_ENDPOINTS );
    }

}
