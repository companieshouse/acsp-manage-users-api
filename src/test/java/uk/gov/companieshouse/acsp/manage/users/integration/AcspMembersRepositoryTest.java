package uk.gov.companieshouse.acsp.manage.users.integration;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.acsp.manage.users.common.TestDataManager;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;
import uk.gov.companieshouse.acsp.manage.users.utils.ApiClientUtil;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

@SpringBootTest
@Testcontainers( parallel = true )
@Tag( "integration-test" )
public class AcspMembersRepositoryTest {

    @Container
    @ServiceConnection
    private static MongoDBContainer container = new MongoDBContainer("mongo:5");

    @Autowired
    private MongoTemplate mongoTemplate;

    @MockBean
    private ApiClientUtil apiClientUtil;

    @MockBean
    private StaticPropertyUtil staticPropertyUtil;

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    @Autowired
    private AcspMembersRepository acspMembersRepository;

    @Test
    void findAllByAcspNumberUserRolesAndUserIdLikeWithNullUserRolesOrNullUserIdThrowsUncategorizedMongoDbException(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016" ) );
        Assertions.assertThrows( UncategorizedMongoDbException.class, () -> acspMembersRepository.findAllByAcspNumberUserRolesAndUserIdLike( "COMA001", null, "", PageRequest.of( 0, 20 ) ) );
        Assertions.assertThrows( UncategorizedMongoDbException.class, () -> acspMembersRepository.findAllByAcspNumberUserRolesAndUserIdLike( "COMA001", Set.of( UserRoleEnum.OWNER, UserRoleEnum.ADMIN, UserRoleEnum.STANDARD ), null, PageRequest.of( 0, 20 ) ) );
    }

    @Test
    void findAllByAcspNumberUserRolesAndUserIdLikeWithNullOrMalformedOrNonexistentAcspNumberOrEmptyRolesOrMalformedOrNonexistentUserIdReturnsEmptyPage(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016" ) );
        Assertions.assertTrue( acspMembersRepository.findAllByAcspNumberUserRolesAndUserIdLike( null, Set.of( UserRoleEnum.OWNER, UserRoleEnum.ADMIN, UserRoleEnum.STANDARD ), "", PageRequest.of( 0, 20 ) ).isEmpty() );
        Assertions.assertTrue( acspMembersRepository.findAllByAcspNumberUserRolesAndUserIdLike( "$$$$$$", Set.of( UserRoleEnum.OWNER, UserRoleEnum.ADMIN, UserRoleEnum.STANDARD ), "", PageRequest.of( 0, 20 ) ).isEmpty() );
        Assertions.assertTrue( acspMembersRepository.findAllByAcspNumberUserRolesAndUserIdLike( "ZZZZZZ", Set.of( UserRoleEnum.OWNER, UserRoleEnum.ADMIN, UserRoleEnum.STANDARD ), "", PageRequest.of( 0, 20 ) ).isEmpty() );
        Assertions.assertTrue( acspMembersRepository.findAllByAcspNumberUserRolesAndUserIdLike( "COMA001", Set.of(), "", PageRequest.of( 0, 20 ) ).isEmpty() );
        Assertions.assertTrue( acspMembersRepository.findAllByAcspNumberUserRolesAndUserIdLike( "COMA001", Set.of( UserRoleEnum.OWNER, UserRoleEnum.ADMIN, UserRoleEnum.STANDARD ), "£££", PageRequest.of( 0, 20 ) ).isEmpty() );
        Assertions.assertTrue( acspMembersRepository.findAllByAcspNumberUserRolesAndUserIdLike( "COMA001", Set.of( UserRoleEnum.OWNER, UserRoleEnum.ADMIN, UserRoleEnum.STANDARD ), "919191", PageRequest.of( 0, 20 ) ).isEmpty() );
    }

    @Test
    void findAllByAcspNumberUserRolesAndUserIdLikeWithNullPageableRetrievesAllAcspMembers(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016" ) );
        Assertions.assertEquals( 16, acspMembersRepository.findAllByAcspNumberUserRolesAndUserIdLike( "COMA001", Set.of( UserRoleEnum.OWNER, UserRoleEnum.ADMIN, UserRoleEnum.STANDARD ), "", null ).getSize() );
    }

    @Test
    void findAllByAcspNumberUserRolesAndUserIdLikeCorrectlyAppliesAcspNumberAndUserRolesFilters(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos( "TS001", "TS002" ) );
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016" ) );

        final var acspMemberIds =
        acspMembersRepository.findAllByAcspNumberUserRolesAndUserIdLike( "COMA001", Set.of( UserRoleEnum.OWNER ), "", PageRequest.of( 0, 20 ) )
                .getContent()
                .stream()
                .map( AcspMembersDao::getId )
                .collect( Collectors.toSet() );

        Assertions.assertEquals( 4, acspMemberIds.size() );
        Assertions.assertTrue( acspMemberIds.containsAll( Set.of( "COM001", "COM002", "COM009", "COM010" ) ) );
    }

    @Test
    void findAllByAcspNumberUserRolesAndUserIdLikeCorrectlyFiltersByUserId(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016" ) );

        final var userIds =
        acspMembersRepository.findAllByAcspNumberUserRolesAndUserIdLike( "COMA001", Set.of( UserRoleEnum.OWNER, UserRoleEnum.ADMIN, UserRoleEnum.STANDARD ), "COMU006", PageRequest.of( 0, 20 ) )
                .getContent()
                .stream()
                .map( AcspMembersDao::getUserId )
                .collect( Collectors.toList() );

        Assertions.assertEquals( 1, userIds.size() );
        Assertions.assertEquals( "COMU006", userIds.getFirst() );
    }

    @Test
    void findAllByAcspNumberUserRolesAndUserIdLikeCorrectlyAppliesRemovedFilter(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016" ) );

        final var acspMemberIds =
        acspMembersRepository.findAllByAcspNumberUserRolesAndUserIdLike( "COMA001", Set.of( UserRoleEnum.OWNER, UserRoleEnum.ADMIN, UserRoleEnum.STANDARD ), "", null, PageRequest.of( 0, 20 ) )
                .getContent()
                .stream()
                .map( AcspMembersDao::getId )
                .collect( Collectors.toSet() );

        Assertions.assertEquals( 10, acspMemberIds.size() );
        Assertions.assertTrue( acspMemberIds.containsAll( Set.of( "COM002", "COM004", "COM005", "COM007", "COM008", "COM010", "COM012", "COM013", "COM015", "COM016" ) ) );
    }

    @Test
    void findAllByAcspNumberUserRolesAndUserIdLikePaginatesCorrectly(){
        acspMembersRepository.insert( testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016" ) );

        final var acspMemberIds =
        acspMembersRepository.findAllByAcspNumberUserRolesAndUserIdLike( "COMA001", Set.of( UserRoleEnum.OWNER, UserRoleEnum.ADMIN, UserRoleEnum.STANDARD ), "", PageRequest.of( 1, 3 ) )
                .stream()
                .map( AcspMembersDao::getId )
                .collect(Collectors.toSet());

        Assertions.assertEquals( 3, acspMemberIds.size() );
        Assertions.assertTrue( acspMemberIds.containsAll( Set.of( "COM004", "COM005", "COM006" ) ) );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection( AcspMembersDao.class );
    }

}
