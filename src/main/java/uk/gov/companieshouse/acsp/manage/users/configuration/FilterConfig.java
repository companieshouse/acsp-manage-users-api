package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.acsp.manage.users.filter.CachedRequestFilter;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<CachedRequestFilter> globalCacheRequestFilter() {
        final var filterRegistrationBean = new FilterRegistrationBean<CachedRequestFilter>();
        filterRegistrationBean.setFilter( new CachedRequestFilter() );
        filterRegistrationBean.addUrlPatterns( "/*" );
        return filterRegistrationBean;
    }

}