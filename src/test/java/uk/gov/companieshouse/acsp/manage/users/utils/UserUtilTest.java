package uk.gov.companieshouse.acsp.manage.users.utils;

import static uk.gov.companieshouse.acsp.manage.users.utils.UserUtil.isRequestingUser;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.model.context.RequestContext;
import uk.gov.companieshouse.acsp.manage.users.model.context.RequestContextData.RequestContextDataBuilder;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class UserUtilTest {

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @Test
    void isRequestingUserWithNullInputsReturnsFalse(){
        Assertions.assertFalse( isRequestingUser( null ) );
        Assertions.assertFalse( isRequestingUser( new AcspMembersDao() ) );
    }

    private static Stream<Arguments> isRequestingUserCorrectlyClassifiesTargetAssociationScenarios(){
        return Stream.of(
                Arguments.of( new AcspMembersDao().userId( "WITU001" ), true ),
                Arguments.of( new AcspMembersDao().userId( "WITU002" ), false ),
                Arguments.of( new AcspMembersDao().userEmail( "geralt@witcher.com" ), true ),
                Arguments.of( new AcspMembersDao().userEmail( "yennefer@witcher.com" ), false )
        );
    }

    @ParameterizedTest
    @MethodSource( "isRequestingUserCorrectlyClassifiesTargetAssociationScenarios" )
    void isRequestingUserCorrectlyClassifiesTargetAssociation( final AcspMembersDao targetAssociation, final boolean expectedOutcome ){
        final var requestingUser = testDataManager.fetchUserDtos( "WITU001" ).getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader( ERIC_IDENTITY, requestingUser.getUserId() );
        RequestContext.setRequestContext( new RequestContextDataBuilder().setEricIdentity( request ).setUser( requestingUser ).build() );

        Assertions.assertEquals( expectedOutcome, isRequestingUser( targetAssociation ) );
    }

}
