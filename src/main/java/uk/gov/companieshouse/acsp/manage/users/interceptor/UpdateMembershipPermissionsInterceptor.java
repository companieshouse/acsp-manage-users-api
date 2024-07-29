package uk.gov.companieshouse.acsp.manage.users.interceptor;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.isOAuth2Request;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ContentCachingRequestWrapper;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch.UserRoleEnum;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class UpdateMembershipPermissionsInterceptor implements HandlerInterceptor {

    private final AcspMembersService acspMembersService;
    private static final Logger LOG = LoggerFactory.getLogger( StaticPropertyUtil.APPLICATION_NAMESPACE );
    private final static String METHOD_PATCH = "patch";
    private final static String PATH_VARIABLE_MEMBERSHIP_ID = "membership_id";
    private static final String PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN = "Please check the request and try again";

    public UpdateMembershipPermissionsInterceptor( final AcspMembersService acspMembersService ) {
        this.acspMembersService = acspMembersService;
    }

    @SuppressWarnings( "unchecked" )
    private String extractMembershipIdFromPath( final HttpServletRequest request ){
        final var pathVariables = (Map<String, String>) request.getAttribute( HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE );
        return pathVariables.get( PATH_VARIABLE_MEMBERSHIP_ID );
    }

    private UserRoleEnum extractUserRoleFromRequestBody( final HttpServletRequest request ) throws IOException {
        final var requestBodyContent = request.getInputStream();
        if ( requestBodyContent.available() == 0 ){
            return null;
        }
        final var objectMapper = new ObjectMapper();
        final var requestBody = objectMapper.readValue( requestBodyContent, RequestBodyPatch.class );

        // TODO: problem... if you called request.getInputStream, you will consume the body and it won't make it to controller

        return requestBody.getUserRole();
    }

    private void throwBadRequestWhenActionIsNotPermittedByOAuth2User( final User requestingUser, final AcspMembersDao membershipIdAssociation, final UserRoleEnum userRole ){
        if ( UserRoleEnum.OWNER.equals( userRole ) ){
            LOG.error( String.format( "User is not permitted to change Acsp Membership %s's role to owner", membershipIdAssociation.getId() ) );
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }

        final var requestUserAssociation =
        acspMembersService.fetchActiveAcspMembership( requestingUser.getUserId(), membershipIdAssociation.getAcspNumber() )
                .orElseThrow( () -> {
                    LOG.error( String.format( "Could not find %s's Acsp Membership at Acsp %s", requestingUser.getUserId(), membershipIdAssociation.getAcspNumber() ) );
                    return new NotFoundRuntimeException( StaticPropertyUtil.APPLICATION_NAMESPACE, PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
                } );

        if ( AcspMembership.UserRoleEnum.STANDARD.getValue().equals( requestUserAssociation.getUserRole() ) ){
            LOG.error( "User is not permitted to perform this action because their role is 'standard'" );
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }

        if ( AcspMembership.UserRoleEnum.ADMIN.getValue().equals( requestUserAssociation.getUserRole() ) && AcspMembership.UserRoleEnum.OWNER.getValue().equals( membershipIdAssociation.getUserRole() ) ){
            LOG.error( "User is not permitted to perform this action because their role is 'admin' and the target user's role is 'owner'" );
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }
    }

    public boolean preHandle( final HttpServletRequest request, final HttpServletResponse response, final Object handler ) throws Exception {
        if ( request.getMethod().equalsIgnoreCase( METHOD_PATCH ) ){
            final var membershipId = extractMembershipIdFromPath( request );
            final var userRole = extractUserRoleFromRequestBody( request );

            final var membershipIdAssociation =
            acspMembersService.fetchMembershipDao( membershipId )
                    .orElseThrow( () -> {
                        LOG.error( String.format( "Could not find Acsp Membership %s", membershipId ) );
                        return new NotFoundRuntimeException( StaticPropertyUtil.APPLICATION_NAMESPACE, PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
                    } );

            if ( UserRoleEnum.OWNER.getValue().equals( membershipIdAssociation.getUserRole() ) && acspMembersService.fetchNumberOfActiveOwners( membershipIdAssociation.getAcspNumber() ) <= 1 ){
                LOG.error( String.format( "Acsp Membership %s is the last owner", membershipId ) );
                throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
            }

            if ( isOAuth2Request() ){
                final var requestingUser = UserContext.getLoggedUser();
                throwBadRequestWhenActionIsNotPermittedByOAuth2User( requestingUser, membershipIdAssociation, userRole );
            }
        }

        return true;
    }


}
