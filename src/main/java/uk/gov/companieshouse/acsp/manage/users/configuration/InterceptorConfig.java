package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.acsp.manage.users.interceptor.AuthorizationInterceptor;
import uk.gov.companieshouse.acsp.manage.users.interceptor.LoggingInterceptor;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;
    private final AuthorizationInterceptor authorizationInterceptor;

    private static final String OAUTH_PROTECTED_ENDPOINTS = "/acsp-members/**";
    private static final String OAUTH_PROTECTED_ENDPOINTS_BASE = "/acsp-members";
    private static final String KEY_PROTECTED_ENDPOINTS = "/internal/acsp-members/**";
    private static final String HEALTH_CHECK_ENDPOINT = "/*/healthcheck";


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
        registry
                .addInterceptor(authorizationInterceptor)
                .addPathPatterns(OAUTH_PROTECTED_ENDPOINTS, OAUTH_PROTECTED_ENDPOINTS_BASE)
                .excludePathPatterns(HEALTH_CHECK_ENDPOINT, KEY_PROTECTED_ENDPOINTS);

        registry.addInterceptor( new InternalUserInterceptor( StaticPropertyUtil.APPLICATION_NAMESPACE ) )
                .addPathPatterns( KEY_PROTECTED_ENDPOINTS )
                .excludePathPatterns( HEALTH_CHECK_ENDPOINT, OAUTH_PROTECTED_ENDPOINTS );
    }

}
