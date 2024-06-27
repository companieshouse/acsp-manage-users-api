package uk.gov.companieshouse.acsp.manage.users.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.acsp_manage_users.api.UserAcspMembershipInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPost;
import uk.gov.companieshouse.api.acsp_manage_users.model.ResponseBodyPost;

import java.util.List;

@RestController
public class UserAcspMembership implements UserAcspMembershipInterface {

    public UserAcspMembership() {}

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

