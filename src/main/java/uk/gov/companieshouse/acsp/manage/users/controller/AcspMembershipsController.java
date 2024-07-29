package uk.gov.companieshouse.acsp.manage.users.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.UserContext;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.PaginationValidatorUtil;
import uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.api.AcspMembershipsInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyLookup;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPost;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import static uk.gov.companieshouse.acsp.manage.users.model.ErrorCode.*;

@Controller
public class AcspMembershipsController implements AcspMembershipsInterface {

  private static final Logger LOG =
      LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

  private static final String PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN =
      "Please check the request and try again";
  private static final String ACSP_NUMBER_KEY = "acspNumber";
  private static final String REQUEST_ID_KEY = "requestId";

  private final UsersService usersService;
  private final AcspDataService acspDataService;
  private final AcspMembersService acspMembersService;

  public AcspMembershipsController(
      final UsersService usersService,
      final AcspDataService acspDataService,
      final AcspMembersService acspMembersService) {
    this.usersService = usersService;
    this.acspDataService = acspDataService;
    this.acspMembersService = acspMembersService;
  }

  @Override
  public ResponseEntity<AcspMembership> addMemberForAcsp(
      final String xRequestId, final String acspNumber, final RequestBodyPost requestBodyPost) {
    LOG.info(
        String.format(
            "Received request for POST `/%s/memberships` with X-Request-Id: %s, user email: %s, and user role: %s",
            acspNumber, xRequestId, requestBodyPost.getUserId(), requestBodyPost.getUserRole()));

    if (Objects.isNull(requestBodyPost.getUserId())) {
      LOG.infoContext(xRequestId, "User ID for the target user not provided", null);
      throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
    }

    AcspDataDao acspDataDao;
    try {
      acspDataDao = acspDataService.fetchAcspData(acspNumber);
    } catch (NotFoundRuntimeException exception) {
      LOG.infoContext(xRequestId, exception.getLocalizedMessage(), null);
      throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
    }

    this.validateLoggedInUserAuthorisationToAddNewMemberForAcsp(
        xRequestId, acspNumber, requestBodyPost);

    User user;
    try {
      user = usersService.fetchUserDetails(requestBodyPost.getUserId());
    } catch (NotFoundRuntimeException exception) {
      LOG.infoContext(xRequestId, exception.getLocalizedMessage(), null);
      throw new BadRequestRuntimeException(ERROR_CODE_1001.getCode());
    }

    final var memberships = acspMembersService.fetchAcspMemberships(user, false);
    if (!memberships.getItems().isEmpty()) {
      LOG.infoContext(
          xRequestId,
          String.format("User with ID %s already has an active ACSP membership", user.getUserId()),
          null);
      throw new BadRequestRuntimeException(ERROR_CODE_1002.getCode());
    }

    final var loggedUser = UserContext.getLoggedUser();
    final var userRole =
        AcspMembership.UserRoleEnum.fromValue(requestBodyPost.getUserRole().getValue());
    final var addedAcspMembership =
        acspMembersService.addAcspMembership(
            user,
            acspDataDao,
            acspNumber,
            userRole,
            Objects.requireNonNull(loggedUser).getUserId());

    return new ResponseEntity<>(addedAcspMembership, HttpStatus.CREATED);
  }

  private void validateLoggedInUserAuthorisationToAddNewMemberForAcsp(
      final String xRequestId, final String acspNumber, final RequestBodyPost requestBodyPost) {
    final var isOauth2 = RequestContextUtil.isOAuth2Request();
    final var loggedUser = Objects.requireNonNull(UserContext.getLoggedUser());
    if (isOauth2) {
      final var loggedUserMemberships = acspMembersService.fetchAcspMemberships(loggedUser, false);
      final var acspMembership =
          loggedUserMemberships.getItems().stream()
              .filter(membership -> membership.getAcspNumber().equals(acspNumber))
              .findFirst();
      if (acspMembership.isPresent()) {
        final var loggedInUserRole = acspMembership.get().getUserRole().getValue();
        final var newUserRole = requestBodyPost.getUserRole().getValue();
        final var isStandardUser =
            AcspMembership.UserRoleEnum.STANDARD.getValue().equals(loggedInUserRole);
        final var isAdminAddingOwner =
            AcspMembership.UserRoleEnum.ADMIN.getValue().equals(loggedInUserRole)
                && AcspMembership.UserRoleEnum.OWNER.getValue().equals(newUserRole);
        if (isStandardUser || isAdminAddingOwner) {
          final var message = getErrorMessage(isStandardUser, loggedUser);
          LOG.infoContext(xRequestId, message, null);
          throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
        }
      }
    }
  }

  private String getErrorMessage(final boolean isStandardUser, final User loggedUser) {
    return String.format(
        "User with ID %s and %s role is not authorised to add %s for ACSP",
        loggedUser.getUserId(),
        isStandardUser ? "standard" : "admin",
        isStandardUser ? "a member" : "an owner member");
  }

  @Override
  public ResponseEntity<AcspMembershipsList> findMembershipsForUserAndAcsp(
      final String requestId,
      final String acspNumber,
      final Boolean includeRemoved,
      final RequestBodyLookup requestBody) {
    Map<String, Object> logMap = new HashMap<>();
    logMap.put(REQUEST_ID_KEY, requestId);
    logMap.put(ACSP_NUMBER_KEY, acspNumber);
    logMap.put("includeRemoved", includeRemoved);
    logMap.put("userEmail", requestBody.getUserEmail());
    LOG.info("Getting members for ACSP & User email", logMap);

    if (Objects.isNull(requestBody.getUserEmail())) {
      LOG.error(String.format("%s: A user email was not provided.", requestId));
      throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
    }

    final var userEmail = requestBody.getUserEmail();
    final var usersList =
        Optional.ofNullable(usersService.searchUserDetails(List.of(userEmail)))
            .filter(users -> !users.isEmpty())
            .orElseThrow(
                () -> {
                  LOG.error(String.format("User %s was not found", userEmail));
                  return new NotFoundRuntimeException(
                      StaticPropertyUtil.APPLICATION_NAMESPACE,
                      PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
                });
    final var user = usersList.getFirst();

    // This will probably be replaced by the ACSP Data Sync API once available.
    acspDataService.fetchAcspData(acspNumber); // can throw 404.

    final var acspMembershipsList =
        acspMembersService.fetchAcspMemberships(user, includeRemoved, acspNumber);

    Map<String, Object> successLogMap = new HashMap<>();
    logMap.put(REQUEST_ID_KEY, requestId);
    logMap.put("userEmail", userEmail);
    logMap.put(ACSP_NUMBER_KEY, acspNumber);
    LOG.info("Getting members for ACSP & User email", successLogMap);

    return new ResponseEntity<>(acspMembershipsList, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<AcspMembershipsList> getMembersForAcsp(
      final String acspNumber,
      final String requestId,
      final Boolean includeRemoved,
      final Integer pageIndex,
      final Integer itemsPerPage,
      final String role) {
    Map<String, Object> logMap = new HashMap<>();
    logMap.put(REQUEST_ID_KEY, requestId);
    logMap.put(ACSP_NUMBER_KEY, acspNumber);
    logMap.put("includeRemoved", includeRemoved);
    logMap.put("pageIndex", pageIndex);
    logMap.put("itemsPerPage", itemsPerPage);
    logMap.put("role", role);
    LOG.info("Getting members for ACSP", logMap);

    final boolean roleIsValid =
        Optional.ofNullable(role)
            .map(
                theRole ->
                    Arrays.stream(AcspMembership.UserRoleEnum.values())
                        .map(AcspMembership.UserRoleEnum::getValue)
                        .anyMatch(validRole -> validRole.equals(theRole)))
            .orElse(true);

    if (!roleIsValid) {
      LOG.error(String.format("Role was invalid: %s", role));
      throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
    }

    final var paginationParams =
        PaginationValidatorUtil.validateAndGetParams(pageIndex, itemsPerPage);

    // This will probably be replaced by the ACSP Data Sync API once available.
    final var acspDataDao = acspDataService.fetchAcspData(acspNumber);

    final var acspMembershipsList =
        acspMembersService.findAllByAcspNumberAndRole(
            acspNumber,
            acspDataDao,
            role,
            includeRemoved,
            paginationParams.pageIndex,
            paginationParams.itemsPerPage);

    Map<String, Object> successLogMap = new HashMap<>();
    logMap.put(REQUEST_ID_KEY, requestId);
    logMap.put(ACSP_NUMBER_KEY, acspNumber);
    logMap.put(
        "membersCount",
        Optional.ofNullable(acspMembershipsList.getItems()).map(List::size).orElse(0));
    LOG.info("Getting members for ACSP", successLogMap);

    return new ResponseEntity<>(acspMembershipsList, HttpStatus.OK);
  }
}
