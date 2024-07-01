package uk.gov.companieshouse.acsp.manage.users.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.api.acsp_manage_users.api.UserAcspMembershipInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPost;
import uk.gov.companieshouse.api.acsp_manage_users.model.ResponseBodyPost;

@RestController
public class UserAcspMembership implements UserAcspMembershipInterface {

  private final AcspMembersService acspMembersService;

  public UserAcspMembership(final AcspMembersService acspMembersService) {
    this.acspMembersService = acspMembersService;
  }

  @Override
  public ResponseEntity<ResponseBodyPost> addAcspMember(
      @NotNull String xRequestId,
      @NotNull String ericIdentity,
      @Valid RequestBodyPost requestBodyPost) {
    return null; // TODO(https://companieshouse.atlassian.net/browse/IDVA6-1149)
  }

  @Override
  public ResponseEntity<AcspMembership> getAcspMembershipForAcspId(
      @NotNull String xRequestId, @Pattern(regexp = "^[a-zA-Z0-9]*$") String id) {
    return null; // TODO(https://companieshouse.atlassian.net/browse/IDVA6-1146)
  }

  @Override
  public ResponseEntity<List<AcspMembership>> getAcspMembershipForUserId(
      final String xRequestId, final String ericIdentity, final Boolean includeRemoved) {
    final boolean excludeRemoved = Objects.isNull(includeRemoved) || !includeRemoved;
    return new ResponseEntity<>(
        acspMembersService.fetchAcspMemberships(ericIdentity, excludeRemoved), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> updateAcspMembershipForId(
      @NotNull String xRequestId,
      @Pattern(regexp = "^[a-zA-Z0-9]*$") String id,
      @Valid RequestBodyPatch requestBodyPatch) {
    return null; // TODO(https://companieshouse.atlassian.net/browse/IDVA6-1147)
  }
}
