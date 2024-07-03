package uk.gov.companieshouse.acsp.manage.users.mapper;

import static uk.gov.companieshouse.acsp.manage.users.common.DateUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.acsp.manage.users.common.DateUtils.reduceTimestampResolution;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipLinks;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class BaseMapperTest {

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    private BaseMapper baseMapper;

    private static final String DEFAULT_KIND = "acsp-membership";

    private static final String DEFAULT_DISPLAY_NAME = "Not Provided";

    @BeforeEach
    void setup(){
        baseMapper = new BaseMapperImpl();
    }

    @Test
    void localDateTimeToOffsetDateTimeWithNullReturnsNull(){
        Assertions.assertNull( baseMapper.localDateTimeToOffsetDateTime( null ) );
    }

    @Test
    void localDateTimeToOffsetDateTimeReturnsCorrectTimestamp(){
        final var inputDate = LocalDateTime.now();
        final var outputDate = baseMapper.localDateTimeToOffsetDateTime( inputDate );
        Assertions.assertEquals( localDateTimeToNormalisedString( inputDate ), reduceTimestampResolution( outputDate.toString() ) );
    }

    @Test
    void enrichAcspMembershipWithKindWithNullThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> baseMapper.enrichAcspMembershipWithKind( null ) );
    }

    @Test
    void enrichAcspMembershipWithKindEnrichesMembership(){
        final var acspMembership = new AcspMembership();
        baseMapper.enrichAcspMembershipWithKind( acspMembership );
        Assertions.assertEquals( DEFAULT_KIND, acspMembership.getKind() );
    }

    @Test
    void enrichAcspMembershipWithLinksWithNullThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> baseMapper.enrichAcspMembershipWithLinks( null ) );
    }

    @Test
    void enrichAcspMembershipWithLinksEnrichesMembership(){
        final var acspMembership = new AcspMembership().id("1");
        baseMapper.enrichAcspMembershipWithLinks( acspMembership );
        Assertions.assertEquals( new AcspMembershipLinks().self("/1"), acspMembership.getLinks() );
    }

    @Test
    void daoToDtoWithNullInputReturnsNull(){
        Assertions.assertNull( baseMapper.daoToDto( null ) );
    }

    @Test
    void daoToDtoAppliedToPartialDaoSuccessfullyMapsToDto(){
        final var dao = testDataManager.fetchAcspMembersDaos( "TS001" ).getFirst();
        final var dto = baseMapper.daoToDto( dao );

        Assertions.assertEquals( dao.getEtag(), dto.getEtag() );
        Assertions.assertEquals( "TS001", dto.getId() );
        Assertions.assertEquals( "TSU001", dto.getUserId() );
        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, dto.getUserDisplayName() );
        Assertions.assertNull( dto.getUserEmail() );
        Assertions.assertEquals( UserRoleEnum.OWNER, dto.getUserRole() );
        Assertions.assertEquals( "TSA001", dto.getAcspNumber() );
        Assertions.assertNull( dto.getAcspName() );
        Assertions.assertNull( dto.getAcspStatus() );
        Assertions.assertEquals( localDateTimeToNormalisedString( dao.getAddedAt() ), reduceTimestampResolution( dto.getAddedAt().toString() ) );
        Assertions.assertNull( dto.getAddedBy() );
        Assertions.assertNull( dto.getRemovedBy() );
        Assertions.assertNull( dto.getRemovedAt() );
        Assertions.assertEquals( DEFAULT_KIND, dto.getKind() );
        Assertions.assertEquals( "/TS001", dto.getLinks().getSelf() );
    }

    @Test
    void daoToDtoAppliedToCompleteDaoSuccessfullyMapsToDto(){
        final var dao = testDataManager.fetchAcspMembersDaos( "TS002" ).getFirst();
        final var dto = baseMapper.daoToDto( dao );

        Assertions.assertEquals( dao.getEtag(), dto.getEtag() );
        Assertions.assertEquals( "TS002", dto.getId() );
        Assertions.assertEquals( "TSU002", dto.getUserId() );
        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, dto.getUserDisplayName() );
        Assertions.assertNull( dto.getUserEmail() );
        Assertions.assertEquals( UserRoleEnum.ADMIN, dto.getUserRole() );
        Assertions.assertEquals( "TSA001", dto.getAcspNumber() );
        Assertions.assertNull( dto.getAcspName() );
        Assertions.assertNull( dto.getAcspStatus() );
        Assertions.assertEquals( localDateTimeToNormalisedString( dao.getAddedAt() ), reduceTimestampResolution( dto.getAddedAt().toString() ) );
        Assertions.assertEquals( "TSU001", dto.getAddedBy() );
        Assertions.assertEquals( "TSU001", dto.getRemovedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( dao.getRemovedAt() ), reduceTimestampResolution( dto.getRemovedAt().toString() ) );
        Assertions.assertEquals( DEFAULT_KIND, dto.getKind() );
        Assertions.assertEquals( "/TS002", dto.getLinks().getSelf() );
    }

}
