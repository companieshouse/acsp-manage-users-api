package uk.gov.companieshouse.acsp.manage.users.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.util.security.AuthorisationUtil;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.Objects;

@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

    private final UsersService usersService;

    public AuthorizationInterceptor(UsersService usersService) {
        this.usersService = usersService;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return isOauth2User(request, response) && isValidAuthorisedUser(request, response);
    }

    private boolean isOauth2User(HttpServletRequest request, HttpServletResponse response) {
        final var hasEricIdentity = Objects.nonNull( request.getHeader( "Eric-Identity" ) );
        final var hasEricIdentityType = Objects.nonNull( request.getHeader( "Eric-Identity-Type" ) );
        if ( hasEricIdentity && hasEricIdentityType && AuthorisationUtil.isOauth2User(request) ) {
            return true;
        }

        LOGGER.debugRequest(request, "invalid user", null);
        response.setStatus(401);
        return false;
    }

    private boolean isValidAuthorisedUser(HttpServletRequest request, HttpServletResponse response) {
        String identity = AuthorisationUtil.getAuthorisedIdentity(request);

        try {
            final var userDetails = usersService.fetchUserDetails( identity );
            UserContext.setLoggedUser( userDetails );
            return true;
        } catch ( NotFoundRuntimeException e ){
            LOGGER.debugRequest(request, "no user found with identity [" + identity + "]", null);
            response.setStatus(403);
            return false;
        }

    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        UserContext.clear();
    }
}
