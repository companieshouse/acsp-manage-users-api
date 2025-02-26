package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.acsp.manage.users.interceptor.*;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    private final LoggingAndCleanupInterceptor loggingInterceptor;

    public InterceptorConfig( final LoggingAndCleanupInterceptor loggingInterceptor ) {
        this.loggingInterceptor = loggingInterceptor;
    }

    @Override
    public void addInterceptors( final InterceptorRegistry registry ) {
        registry.addInterceptor( loggingInterceptor );
    }

}
