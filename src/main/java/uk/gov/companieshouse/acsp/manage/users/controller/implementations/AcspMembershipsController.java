package uk.gov.companieshouse.acsp.manage.users.controller.implementations;

import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.utils.PaginationValidatorUtil;
import uk.gov.companieshouse.acsp.manage.users.utils.PaginationValidatorUtil.PaginationParams;
import uk.gov.companieshouse.api.acsp_manage_users.api.AcspMembershipsInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyLookup;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPost;

@Controller
public class AcspMembershipsController implements AcspMembershipsInterface {

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
    final PaginationParams paginationParams =
        PaginationValidatorUtil.validateAndGetParams(pageIndex, itemsPerPage);

    if (!Set.of("admin", "owner", "standard").contains(role.toLowerCase())) {
      throw new BadRequestRuntimeException("Please check the request and try again");
    }

    final AcspDataDao acspDataDao = acspDataService.fetchAcspData(acspNumber);
    final var acspMembersDaoPage =
        acspMembersService.findAllByAcspNumber(
            acspNumber, includeRemoved, paginationParams.pageIndex, paginationParams.itemsPerPage);

    final var acspMembershipsList =
        acspMembersService.mapToAcspMembershipsList(acspMembersDaoPage, acspDataDao);

    return new ResponseEntity<>(acspMembershipsList, HttpStatus.OK);
  }
}
