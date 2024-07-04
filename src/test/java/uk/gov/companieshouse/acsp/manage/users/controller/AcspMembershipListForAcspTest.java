package uk.gov.companieshouse.acsp.manage.users.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.service.AcspDataService;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;

@WebMvcTest(AcspMembershipListForAcsp.class)
@Tag("unit-test")
class AcspMembershipListForAcspTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StaticPropertyUtil staticPropertyUtil;

    @MockBean
    private UsersService usersService;

    @MockBean
    private AcspDataService acspDataService;

    @MockBean
    private AcspMembersService acspMembersService;

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    private void mockFetchAcspDataFor( String... acspNumbers ){
        Arrays.stream( acspNumbers ).forEach( acspNumber -> Mockito.doReturn( testDataManager.fetchAcspDataDaos( acspNumber ).getFirst() ).when( acspDataService ).fetchAcspData( acspNumber ) );
    }

    private ArgumentMatcher<AcspDataDao> acspDataDaoMatcher( String acspId, String acspName, String acspStatus ){
        return acsp -> {
            final var acspIdIsCorrect = acspId.equals( acsp.getId() );
            final var acspNameIsCorrect = acspName.equals( acsp.getAcspName() );
            final var acspStatusIsCorrect = acspStatus.equals( acsp.getAcspStatus() );
            return acspIdIsCorrect && acspNameIsCorrect && acspStatusIsCorrect;
        };
    }

    @Test
    void getMembersForAcspWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/acsp-members/acsps/COMA001" )
                        .header("Eric-identity", "COMU002")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getMembersForAcspWithMalformedAcspNumberReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/acsp-members/acsps/££££££" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU002")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getMembersForAcspWithMalformedPageIndexReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/acsp-members/acsps/COMA001?page_index=-1" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU002")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getMembersForAcspWithMalformedItemsPerPageReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/acsp-members/acsps/COMA001?items_per_page=-1" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU002")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getMembersForAcspWithMalformedUserEmailReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/acsp-members/acsps/COMA001?user_email=$$$$" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU002")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getMembersForAcspWithMalformedUserRoleReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/acsp-members/acsps/COMA001?role=$$$$" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU002")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getMembersForAcspWithNonExistentAcspNumberReturnsNotFound() throws Exception {
        Mockito.doThrow( new NotFoundRuntimeException( "acsp-manage-users-api", "Was not found" ) ).when( acspDataService ).fetchAcspData( "919191" );

        mockMvc.perform( get( "/acsp-members/acsps/919191" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU002")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isNotFound() );
    }

    @Test
    void getMembersForAcspWithNonExistentUserEmailReturnsNotFound() throws Exception {
        mockFetchAcspDataFor( "COMA001" );

        mockMvc.perform( get( "/acsp-members/acsps/COMA001?user_email=elon.musk@tesla.com" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU002")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isNotFound() );
    }

    @Test
    void getMembersForAcspWithEverythingSpecifiedFormsQueryCorrectly() throws Exception {
        mockFetchAcspDataFor( "COMA001" );

        final var searchResults = new UsersList();
        searchResults.add( testDataManager.fetchUserDtos( "COMU001" ).getFirst() );
        Mockito.doReturn( searchResults ).when( usersService ).searchUserDetails( List.of( "jimmy.carr@comedy.com" ) );

        mockMvc.perform( get( "/acsp-members/acsps/COMA001?include_removed=true&role=owner&page_index=4&items_per_page=2&user_email=jimmy.carr@comedy.com" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU002")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isOk() );

        Mockito.verify( acspMembersService ).fetchAcspMembers( argThat( acspDataDaoMatcher( "COMA001", "Comedy", "active" ) ), eq( true ), eq( "COMU001" ), eq( "owner" ), eq( 4 ), eq( 2 ) );
    }

    @Test
    void getMembersForAcspWithDefaultsFormsQueryCorrectly() throws Exception {
        mockFetchAcspDataFor( "COMA001" );

        mockMvc.perform( get( "/acsp-members/acsps/COMA001" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU002")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isOk() );

        Mockito.verify( acspMembersService ).fetchAcspMembers( argThat( acspDataDaoMatcher( "COMA001", "Comedy", "active" ) ), eq( false ), isNull(), isNull(), eq( 0 ), eq( 15 ) );
    }

}
