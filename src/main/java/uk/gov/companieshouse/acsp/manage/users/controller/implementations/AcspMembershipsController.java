package uk.gov.companieshouse.acsp.manage.users.controller.implementations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.utils.PaginationValidatorUtil;
import uk.gov.companieshouse.acsp.manage.users.utils.PaginationValidatorUtil.PaginationParams;
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

  private final AcspDataService acspDataService;
  private final AcspMembersService acspMembersService;

  public AcspMembershipsController(
      final AcspDataService acspDataService, final AcspMembersService acspMembersService) {
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
    return null;
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
    logMap.put("requestId", requestId);
    logMap.put("acspNumber", acspNumber);
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

    final PaginationParams paginationParams =
        PaginationValidatorUtil.validateAndGetParams(pageIndex, itemsPerPage);

    // This will probably be replaced by the ACSP Data Sync API once available.
    final AcspDataDao acspDataDao = acspDataService.fetchAcspData(acspNumber);

    final var acspMembershipsList =
        acspMembersService.findAllByAcspNumberAndRole(
            acspNumber,
            acspDataDao,
            role,
            includeRemoved,
            paginationParams.pageIndex,
            paginationParams.itemsPerPage);

    LOG.info(
        "Successfully retrieved members for ACSP",
        new HashMap<>(
            Map.of(
                "requestId",
                requestId,
                "acspNumber",
                acspNumber,
                "membersCount",
                acspMembershipsList.getTotalResults())));

    return new ResponseEntity<>(acspMembershipsList, HttpStatus.OK);
  }
}
