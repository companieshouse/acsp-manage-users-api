package uk.gov.companieshouse.acsp.manage.users.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.acsp.manage.users.AcspManageUsersServiceApplication;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembershipService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acsp_manage_users.api.UserAcspMembershipInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPost;
import uk.gov.companieshouse.api.acsp_manage_users.model.ResponseBodyPost;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.List;
import java.util.Objects;

@RestController
public class UserAcspMembership implements UserAcspMembershipInterface {

    private final AcspMembershipService acspMembershipService;

    public static final String PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN = "Please check the request and try again";

    private static final Logger LOG = LoggerFactory.getLogger(AcspManageUsersServiceApplication.applicationNameSpace);
    public UserAcspMembership(AcspMembershipService acspMembersService) {
        this.acspMembershipService = acspMembersService;
    }

    @Override
    public ResponseEntity<ResponseBodyPost> addAcspMember(
            @NotNull String xRequestId,
            @NotNull String ericIdentity,
            @Valid RequestBodyPost requestBodyPost
    ) {
        return null; // TODO(https://companieshouse.atlassian.net/browse/IDVA6-1149)
    }

    @Override
    public ResponseEntity<AcspMembership> getAcspMembershipForAcspId(
           final String xRequestId,
           final String id
    ) {
        LOG.debug(String.format("%s: Retrieving id for member id (%s) ...", xRequestId, id));
        if (Objects.isNull(id)){
            LOG.error(String.format("%s: No member id was provided.", xRequestId));
            throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
        }

        if ( !acspMembershipService.memberIdExists( id )){
            LOG.debug( String.format( "%s: Unable to find member id: %s", xRequestId, id ) );
            throw new NotFoundRuntimeException( "acsp-members", PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
        }

        final var association = acspMembershipService.fetchAcspMembership(id);
        if (association.isEmpty()) {
            var errorMessage = String.format("Cannot find Association for the Id: %s", id);
            LOG.error(errorMessage);
            throw new NotFoundRuntimeException(StaticPropertyUtil.APPLICATION_NAMESPACE, errorMessage);
        }
        return new ResponseEntity<>(association.get(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<AcspMembership>> getAcspMembershipForUserId(
            @NotNull String xRequestId,
            @NotNull String ericIdentity,
            @Valid Boolean includeRemoved
    ) {
        return null; // TODO(https://companieshouse.atlassian.net/browse/IDVA6-1145)
    }

    @Override
    public ResponseEntity<Void> updateAcspMembershipForId(
            @NotNull String xRequestId,
            @Pattern(regexp = "^[a-zA-Z0-9]*$") String id,
            @Valid RequestBodyPatch requestBodyPatch
    ) {
        return null; // TODO(https://companieshouse.atlassian.net/browse/IDVA6-1147)
    }

}

