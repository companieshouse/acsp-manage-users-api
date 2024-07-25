package uk.gov.companieshouse.acsp.manage.users.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.companieshouse.acsp.manage.users.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyLookup;

@ExtendWith(MockitoExtension.class)
class AcspMembershipsControllerTest {

    @Mock
    private UsersService usersService;

    @Mock
    private AcspDataService acspDataService;

    @Mock
    private AcspMembersService acspMembersService;

    @InjectMocks
    private AcspMembershipsController acspMembershipsController;

    private static final String REQUEST_ID = "test-request-id";
    private static final String ACSP_NUMBER = "COMA001";

    @Test
    void getMembersForAcsp_ValidRequest_ReturnsOk() {
        final boolean includeRemoved = false;
        final int pageIndex = 0;
        final int itemsPerPage = 15;
        final String role = "owner";

        final AcspDataDao acspDataDao = new AcspDataDao();
        when(acspDataService.fetchAcspData(ACSP_NUMBER)).thenReturn(acspDataDao);

        AcspMembershipsList expectedList = new AcspMembershipsList();
        when(acspMembersService.findAllByAcspNumberAndRole(ACSP_NUMBER, acspDataDao, role,
                includeRemoved, pageIndex, itemsPerPage))
                .thenReturn(expectedList);

        ResponseEntity<AcspMembershipsList> response = acspMembershipsController.getMembersForAcsp(
                ACSP_NUMBER, REQUEST_ID, includeRemoved, pageIndex, itemsPerPage, role);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedList, response.getBody());
    }

    @Test
    void getMembersForAcsp_InvalidRole_ThrowsBadRequestException() {
        final String invalidRole = "invalid_role";

        assertThrows(BadRequestRuntimeException.class, () ->
                acspMembershipsController.getMembersForAcsp(ACSP_NUMBER, REQUEST_ID, false, 0, 15,
                        invalidRole)
        );
    }

    @Test
    void getMembersForAcsp_NullRole_ReturnsOk() {
        final Boolean includeRemoved = true;
        final Integer pageIndex = 0;
        final Integer itemsPerPage = 20;
        final String role = null;

        final AcspDataDao acspDataDao = new AcspDataDao();
        when(acspDataService.fetchAcspData(ACSP_NUMBER)).thenReturn(acspDataDao);

        final AcspMembershipsList expectedList = new AcspMembershipsList();
        when(acspMembersService.findAllByAcspNumberAndRole(ACSP_NUMBER, acspDataDao, role,
                includeRemoved, pageIndex, itemsPerPage))
                .thenReturn(expectedList);

        ResponseEntity<AcspMembershipsList> response = acspMembershipsController.getMembersForAcsp(
                ACSP_NUMBER, REQUEST_ID, includeRemoved, pageIndex, itemsPerPage, role);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedList, response.getBody());
    }

    @Test
    void findMembershipsForUserAndAcsp_ValidRequest_ReturnsOk() {
        final String userEmail = "test@example.com";
        final Boolean includeRemoved = false;
        final RequestBodyLookup requestBody = new RequestBodyLookup();
        requestBody.setUserEmail(userEmail);

        final User user = new User();
        user.setEmail(userEmail);
        final UsersList usersList = new UsersList();
        usersList.add(user);
        when(usersService.searchUserDetails(List.of(userEmail))).thenReturn(usersList);

        AcspDataDao acspDataDao = new AcspDataDao();
        when(acspDataService.fetchAcspData(ACSP_NUMBER)).thenReturn(acspDataDao);

        AcspMembershipsList expectedList = new AcspMembershipsList();
        when(acspMembersService.fetchAcspMemberships(user, includeRemoved, ACSP_NUMBER)).thenReturn(
                expectedList);

        ResponseEntity<AcspMembershipsList> response = acspMembershipsController.findMembershipsForUserAndAcsp(
                REQUEST_ID, ACSP_NUMBER, includeRemoved, requestBody);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedList, response.getBody());
    }

    @Test
    void findMembershipsForUserAndAcsp_NullUserEmail_ThrowsBadRequestException() {
        RequestBodyLookup requestBody = new RequestBodyLookup();
        requestBody.setUserEmail(null);

        assertThrows(BadRequestRuntimeException.class, () ->
                acspMembershipsController.findMembershipsForUserAndAcsp(REQUEST_ID, ACSP_NUMBER,
                        false, requestBody)
        );
    }

    @Test
    void findMembershipsForUserAndAcsp_UserNotFound_ThrowsNotFoundException() {
        final String userEmail = "nonexistent@example.com";
        final RequestBodyLookup requestBody = new RequestBodyLookup();
        requestBody.setUserEmail(userEmail);

        when(usersService.searchUserDetails(List.of(userEmail))).thenReturn(new UsersList());

        assertThrows(NotFoundRuntimeException.class, () ->
                acspMembershipsController.findMembershipsForUserAndAcsp(REQUEST_ID, ACSP_NUMBER,
                        false, requestBody)
        );
    }
}