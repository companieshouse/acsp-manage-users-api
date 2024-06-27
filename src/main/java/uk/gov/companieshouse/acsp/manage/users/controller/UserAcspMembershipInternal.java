package uk.gov.companieshouse.acsp.manage.users.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.acsp_manage_users.api.UserAcspMembershipInternalInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.InternalRequestBodyPatch;
import uk.gov.companieshouse.api.acsp_manage_users.model.InternalRequestBodyPost;
import uk.gov.companieshouse.api.acsp_manage_users.model.ResponseBodyPost;

@RestController
public class UserAcspMembershipInternal implements UserAcspMembershipInternalInterface {

    public UserAcspMembershipInternal() {}

    @Override
    public ResponseEntity<ResponseBodyPost> addAcspOwner(
            @NotNull String xRequestId,
            @Pattern(regexp = "^[0-9A-Za-z-_]{0,32}$") String acspNumber,
            @Valid InternalRequestBodyPost internalRequestBodyPost) {
        return null; // TODO(https://companieshouse.atlassian.net/browse/IDVA6-1238)
    }

    @Override
    public ResponseEntity<Boolean> isActiveMember(
            @NotNull String xRequestId,
            @Pattern(regexp = "^[0-9A-Za-z-_]{0,32}$") String acspNumber,
            @NotNull @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$") String userEmail
    ) {
        return null; // TODO(https://companieshouse.atlassian.net/browse/IDVA6-1212)
    }

    @Override
    public ResponseEntity<Void> performActionOnAcsp(
            @NotNull String xRequestId,
            @Pattern(regexp = "^[0-9A-Za-z-_]{0,32}$") String acspNumber,
            @Valid InternalRequestBodyPatch internalRequestBodyPatch
    ) {
        return null;
    }

}

