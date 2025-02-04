package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.acsp.manage.users.interceptor.*;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.api.interceptor.TokenPermissionsInterceptor;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    private static final String OAUTH_PROTECTED_ENDPOINTS = "/user/**";
    private static final String OAUTH_AND_KEY_PROTECTED_ENDPOINTS = "/acsps/**";
    private static final String HEALTH_CHECK_ENDPOINT = "/*/healthcheck";

    private final UsersService usersService;
    private final LoggingInterceptor loggingInterceptor;
    private final SessionValidityInterceptor sessionValidityInterceptor;

    public InterceptorConfig( final UsersService usersService, final LoggingInterceptor loggingInterceptor, final SessionValidityInterceptor sessionValidityInterceptor ) {
        this.usersService = usersService;
        this.loggingInterceptor = loggingInterceptor;
        this.sessionValidityInterceptor = sessionValidityInterceptor;
    }

    @Override
    public void addInterceptors( @NonNull final InterceptorRegistry registry ) {
        addLoggingInterceptor( registry );
        addAuthorizationInterceptors( registry );
        preprocessingInterceptors( registry );
        permissionValidityInterceptors( registry );
    }

    private void addLoggingInterceptor( final InterceptorRegistry registry ) {
        registry.addInterceptor( loggingInterceptor );
    }

    private void addAuthorizationInterceptors( final InterceptorRegistry registry ){
        registry.addInterceptor( new CompositeAuthorizationInterceptor( true, false, usersService ) )
                .addPathPatterns( OAUTH_PROTECTED_ENDPOINTS )
                .excludePathPatterns( HEALTH_CHECK_ENDPOINT, OAUTH_AND_KEY_PROTECTED_ENDPOINTS );

        registry.addInterceptor( new CompositeAuthorizationInterceptor( true, true, usersService ) )
                .addPathPatterns( OAUTH_AND_KEY_PROTECTED_ENDPOINTS )
                .excludePathPatterns( HEALTH_CHECK_ENDPOINT, OAUTH_PROTECTED_ENDPOINTS );
    }

    private void preprocessingInterceptors( final InterceptorRegistry registry ){
        registry.addInterceptor( new AdminPermissionsInterceptor() )
                .addPathPatterns( OAUTH_PROTECTED_ENDPOINTS, OAUTH_AND_KEY_PROTECTED_ENDPOINTS )
                .excludePathPatterns( HEALTH_CHECK_ENDPOINT );

        registry.addInterceptor( new TokenPermissionsInterceptor() )
                .addPathPatterns( OAUTH_AND_KEY_PROTECTED_ENDPOINTS )
                .excludePathPatterns( HEALTH_CHECK_ENDPOINT, OAUTH_PROTECTED_ENDPOINTS );
    }

    private void permissionValidityInterceptors( final InterceptorRegistry registry ) {
        registry.addInterceptor( sessionValidityInterceptor )
                .addPathPatterns( OAUTH_AND_KEY_PROTECTED_ENDPOINTS )
                .excludePathPatterns( HEALTH_CHECK_ENDPOINT, OAUTH_PROTECTED_ENDPOINTS );
    }

}
