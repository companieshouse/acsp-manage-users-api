package uk.gov.companieshouse.acsp.manage.users.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.acsp_manage_users.api.AcspMembershipListForAcspInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembers;

@RestController
public class AcspMembershipListForAcsp implements AcspMembershipListForAcspInterface {

    @Override
    public ResponseEntity<AcspMembers> getMembersForAcsp(
            @Pattern(regexp = "^[0-9A-Za-z-_]{0,32}$") String acspNumber,
            @NotNull String xRequestId,
            @Valid Boolean includeRemoved,
            @Valid Integer pageIndex,
            @Valid Integer itemsPerPage,
            @Pattern(regexp = "^[a-zA-Z0-9-_]*$") @Valid String userId,
            @Valid String role
    ) {
        return null; // TODO(https://companieshouse.atlassian.net/browse/IDVA6-1142)
    }

}
