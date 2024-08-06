package uk.gov.companieshouse.acsp.manage.users.interceptor;

import static org.springframework.web.servlet.support.WebContentGenerator.METHOD_POST;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPost;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPost.UserRoleEnum;
import uk.gov.companieshouse.api.util.security.TokenPermissions;

@Component
public class AcspCreationPermissionInterceptor implements HandlerInterceptor {

    private final static String PATH_VARIABLE_MEMBERSHIP_ID = "acsp_number";

    @SuppressWarnings( "unchecked" )
    private String extractPathVariableFromRequest( final HttpServletRequest request, final String pathVariable ){
        final var pathVariables = ( Map<String, String> ) request.getAttribute( HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE );
        return pathVariables.get( pathVariable );
    }

    private <T> T extractBodyFromRequest( final HttpServletRequest request, Class<T> requestBodyClass ) throws IOException {
        final var requestBodyContent = request.getInputStream();
        if ( requestBodyContent.isFinished() ){
            return null;
        }
        final var objectMapper = new ObjectMapper();
        return objectMapper.readValue( requestBodyContent, requestBodyClass );
    }





    private void canCreateMembership( final TokenPermissions tokenPermissions, final String acspNumber, final UserRoleEnum userRole ){

    }




    @Override
    public boolean preHandle( final HttpServletRequest request, final HttpServletResponse response, final Object handler ) throws Exception {
        if ( request.getMethod().equalsIgnoreCase( METHOD_POST ) ) {
            final var tokenPermissions = (TokenPermissions) request.getAttribute( "token_permissions" );

            final var acspNumber = extractPathVariableFromRequest( request, PATH_VARIABLE_MEMBERSHIP_ID );

            final var requestBody = extractBodyFromRequest( request, RequestBodyPost.class );
            final var userId = requestBody.getUserId();
            final var userRole = requestBody.getUserRole();





//            canCreateMembership( tokenPermissions, acspNumber );


            final var x = 5;
        }

        return true;
    }







}
