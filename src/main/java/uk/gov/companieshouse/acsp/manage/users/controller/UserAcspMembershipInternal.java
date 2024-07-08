package uk.gov.companieshouse.acsp.manage.users.controller;

import static uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.AcspStatusEnum.ACTIVE;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
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

@RestController
public class UserAcspMembershipInternal implements UserAcspMembershipInternalInterface {

  private static final Logger LOG =
      LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);
  private static final String USER_ID_KEY = "userId";
  private static final String ACSP_NUMBER_KEY = "acspNumber";

  private final AcspDataService acspDataService;
  private final UsersService usersService;
  private final AcspMembersService acspMembersService;

  @Autowired
  public UserAcspMembershipInternal(
      final AcspDataService acspDataService,
      final UsersService usersService,
      final AcspMembersService acspMembersService) {
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

    // we call this to throw an exception if the acsp data does not exist
    acspDataService.fetchAcspData(acspNumber);

    final String userEmail = internalRequestBodyPost.getOwnerEmail();
    final UsersList users =
        Optional.ofNullable(usersService.searchUserDetails(List.of(userEmail)))
            .filter((userList -> !userList.isEmpty()))
            .orElseThrow(
                () -> {
                  LOG.error(String.format("Failed to find user with userEmail %s", userEmail));
                  return new NotFoundRuntimeException(
                      StaticPropertyUtil.APPLICATION_NAMESPACE, "Failed to find user");
                });
    final String userId = users.getFirst().getUserId();
    final var acspMembers = acspMembersService.fetchAcspMemberships(users.getFirst(), false);
    if (!acspMembers.isEmpty()) {
      acspMembers.stream()
          .filter(item -> item.getAcspNumber().equals(acspNumber))
          .findFirst()
          .ifPresent(
              elem -> {
                final var errorMessage =
                    String.format(
                        "ACSP for acspNumber %s and userId %s already exists.", acspNumber, userId);
                LOG.error(errorMessage);
                throw new BadRequestRuntimeException(errorMessage);
              });
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
      final String xRequestId, final String acspNumber, final String userEmail) {
    LOG.infoContext(
        xRequestId,
        "Checking if user is an active ACSP member",
        new HashMap<>(Map.of(ACSP_NUMBER_KEY, acspNumber, "userEmail", userEmail)));

    var acspData = acspDataService.fetchAcspData(acspNumber);
    if (!ACTIVE.getValue().equals(acspData.getAcspStatus())) {
      LOG.infoContext(
          xRequestId,
          "ACSP is not active",
          new HashMap<>(
              Map.of(ACSP_NUMBER_KEY, acspNumber, "acspStatus", acspData.getAcspStatus())));
      return new ResponseEntity<>(false, HttpStatus.OK);
    }

    final UsersList users =
        Optional.ofNullable(usersService.searchUserDetails(List.of(userEmail)))
            .filter((userList -> !userList.isEmpty()))
            .orElseThrow(
                () -> {
                  LOG.errorContext(
                      xRequestId,
                      String.format("Failed to find user with userEmail %s", userEmail),
                      null,
                      new HashMap<>(Map.of("userEmail", userEmail)));
                  return new NotFoundRuntimeException(
                      StaticPropertyUtil.APPLICATION_NAMESPACE, "Failed to find user");
                });
    final String userId = users.getFirst().getUserId();

    LOG.debugContext(
        xRequestId,
        "Fetching active ACSP membership for user",
        new HashMap<>(
            Map.of(
                USER_ID_KEY, userId,
                ACSP_NUMBER_KEY, acspNumber)));

    final Optional<AcspMembersDao> activeMember =
        acspMembersService.fetchActiveAcspMemberByUserIdAndAcspNumber(userId, acspNumber);
    if (activeMember.isEmpty()) {
      LOG.infoContext(
          xRequestId,
          "User is not an active member of the ACSP",
          new HashMap<>(
              Map.of(
                  USER_ID_KEY, userId,
                  ACSP_NUMBER_KEY, acspNumber)));
      return new ResponseEntity<>(false, HttpStatus.OK);
    }

    LOG.infoContext(
        xRequestId,
        "User is an active member of the ACSP",
        new HashMap<>(
            Map.of(
                USER_ID_KEY, userId,
                ACSP_NUMBER_KEY, acspNumber)));
    return new ResponseEntity<>(true, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> performActionOnAcsp(
      @NotNull String xRequestId,
      @Pattern(regexp = "^[0-9A-Za-z-_]{0,32}$") String acspNumber,
      @Valid InternalRequestBodyPatch internalRequestBodyPatch) {
    return null;
  }
}
