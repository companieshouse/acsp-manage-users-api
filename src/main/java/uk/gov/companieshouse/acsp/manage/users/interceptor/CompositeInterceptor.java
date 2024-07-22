package uk.gov.companieshouse.acsp.manage.users.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.springframework.web.servlet.HandlerInterceptor;

public class CompositeInterceptor implements HandlerInterceptor {

    private List<HandlerInterceptor> interceptors = new LinkedList<>();

    public CompositeInterceptor( HandlerInterceptor... interceptors ){
        this.interceptors.addAll( Arrays.asList( interceptors ) );
    }

    @Override
    public boolean preHandle( HttpServletRequest request, HttpServletResponse response, Object handler ) throws Exception {
        var errorResponseCode = 401;
        for ( HandlerInterceptor interceptor: interceptors ){
            if( interceptor.preHandle( request, response, handler ) ){
                return true;
            }
            if ( response.getStatus() == 403 ){
                errorResponseCode = 403;
            }
        }
        response.setStatus( errorResponseCode );
        return false;
    }

}
