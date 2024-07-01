package uk.gov.companieshouse.acsp.manage.users.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.acsp.manage.users.mapper.AcspMembershipListMapper;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.api.acsp_manage_users.api.UserAcspMembershipInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPost;
import uk.gov.companieshouse.api.acsp_manage_users.model.ResponseBodyPost;

import java.util.List;

@RestController
public class UserAcspMembership implements UserAcspMembershipInterface {

    private final AcspMembershipListMapper acspMembershipListMapper;
    private final AcspMembersService acspMembersService;

    public UserAcspMembership(
            final AcspMembershipListMapper acspMembershipListMapper,
            final AcspMembersService acspMembersService
    ) {
        this.acspMembershipListMapper = acspMembershipListMapper;
        this.acspMembersService = acspMembersService;
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
            @NotNull String xRequestId,
            @Pattern(regexp = "^[a-zA-Z0-9]*$") String id
    ) {
        return null; // TODO(https://companieshouse.atlassian.net/browse/IDVA6-1146)
    }

    @Override
    public ResponseEntity<List<AcspMembership>> getAcspMembershipForUserId(
            final String xRequestId,
            final String ericIdentity,
            final Boolean includeRemoved
    ) {
        final boolean excludeRemoved = includeRemoved == null || !includeRemoved;

        final List<AcspMembership> memberships = acspMembershipListMapper.daoToDto(
                acspMembersService.fetchAcspMembers(ericIdentity).filter(
                        acspMembersDao -> !excludeRemoved || !acspMembersDao.hasBeenRemoved()
                ).toList(),
                UserContext.getLoggedUser()
        );

        return new ResponseEntity<>(memberships, HttpStatus.OK);
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

