package uk.gov.companieshouse.acsp.manage.users.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.acsp_manage_users.api.UserAcspMembershipInternalInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.InternalRequestBodyPatch;
import uk.gov.companieshouse.api.acsp_manage_users.model.InternalRequestBodyPost;
import uk.gov.companieshouse.api.acsp_manage_users.model.ResponseBodyPost;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.List;
import java.util.Optional;

@RestController
public class UserAcspMembershipInternal implements UserAcspMembershipInternalInterface {

  private static final Logger LOG =
      LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);
  private final AcspDataService acspDataService;
  private final UsersService usersService;
  private final AcspMembersService acspMembersService;

  @Autowired
  public UserAcspMembershipInternal(
      AcspDataService acspDataService,
      UsersService usersService,
      AcspMembersService acspMembersService) {
    this.acspDataService = acspDataService;
    this.usersService = usersService;
    this.acspMembersService = acspMembersService;
  }

  @Override
  public ResponseEntity<ResponseBodyPost> addAcspOwner(
      final String xRequestId,
      final String acspNumber,
      final InternalRequestBodyPost internalRequestBodyPost) {
    LOG.debugContext(
        xRequestId,
        String.format("Attempting to add ACSP owner for acsp_number %s", acspNumber),
        null);
    acspDataService.fetchAcspData(acspNumber);
    final String userEmail = internalRequestBodyPost.getOwnerEmail();
    final UsersList users =
        Optional.ofNullable(usersService.searchUserDetails(List.of(userEmail)))
            .filter((userList -> !userList.isEmpty()))
            .orElseThrow(
                () -> {
                  LOG.error(String.format("Failed to find user with userEmail %s", userEmail));
                  return new NotFoundRuntimeException(
                      "acsp-manage-users-api", "Failed to find user");
                });
    final String userId = users.getFirst().getUserId();
    final var acspMembers =
        acspMembersService.fetchAcspMembersForAcspNumberAndUserId(acspNumber, userId);
    if (acspMembers.isPresent() && StringUtils.isBlank(acspMembers.get().getRemovedBy())) {
      final var errorMessage =
          String.format("ACSP for acspNumber %s and userId %s already exists.", acspNumber, userId);
      LOG.error(errorMessage);
      throw new BadRequestRuntimeException(errorMessage);
    }
    final var newAcspMembers =
        acspMembersService.createAcspMembersWithOwnerRole(acspNumber, userId);
    LOG.debugContext(
        xRequestId,
        String.format("Successfully added ACSP owner for acsp_number %s", acspNumber),
        null);
    return new ResponseEntity<>(
        new ResponseBodyPost().acspMembershipId(newAcspMembers.getId()), HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Boolean> isActiveMember(
      @NotNull String xRequestId,
      @Pattern(regexp = "^[0-9A-Za-z-_]{0,32}$") String acspNumber,
      @NotNull @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
          String userEmail) {
    return null; // TODO(https://companieshouse.atlassian.net/browse/IDVA6-1212)
  }

  @Override
  public ResponseEntity<Void> performActionOnAcsp(
      @NotNull String xRequestId,
      @Pattern(regexp = "^[0-9A-Za-z-_]{0,32}$") String acspNumber,
      @Valid InternalRequestBodyPatch internalRequestBodyPatch) {
    return null;
  }
}
