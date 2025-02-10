package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.acsp.manage.users.interceptor.*;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;

    public InterceptorConfig( final LoggingInterceptor loggingInterceptor ) {
        this.loggingInterceptor = loggingInterceptor;
    }

    @Override
    public void addInterceptors( @NonNull final InterceptorRegistry registry ) {
        registry.addInterceptor( loggingInterceptor );
    }

}
