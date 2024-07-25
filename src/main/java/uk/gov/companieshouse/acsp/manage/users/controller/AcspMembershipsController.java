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
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.PaginationValidatorUtil;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acsp_manage_users.api.AcspMembershipsInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyLookup;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPost;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

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
      final String requestId, final String acspNumber, final RequestBodyPost requestBody) {
    return null;
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
    logMap.put("membersCount", Optional.ofNullable(acspMembershipsList.getItems())
            .map(List::size)
            .orElse(0));
    LOG.info("Getting members for ACSP", successLogMap);

    return new ResponseEntity<>(acspMembershipsList, HttpStatus.OK);
  }
}
