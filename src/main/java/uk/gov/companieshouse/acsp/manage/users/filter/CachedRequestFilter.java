package uk.gov.companieshouse.acsp.manage.users.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class CachedRequestFilter implements Filter {

    @Override
    public void init( final FilterConfig filterConfig ) throws ServletException {
        Filter.super.init( filterConfig );
    }

    @Override
    public void doFilter( final ServletRequest request, final ServletResponse response, final FilterChain filterChain ) throws IOException, ServletException {
        final var cachedRequest = new CachedHttpServletRequest( (HttpServletRequest) request );
        filterChain.doFilter( cachedRequest, response );
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }

}

