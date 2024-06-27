package uk.gov.companieshouse.acsp.manage.users.common;

import static uk.gov.companieshouse.GenerateEtagUtil.generateEtag;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

public class TestDataManager {

    private static TestDataManager instance = null;

    public static TestDataManager getInstance(){
        if ( Objects.isNull( instance ) ){
            instance = new TestDataManager();
        }
        return instance;
    }

    private final Map<String, Supplier<AcspMembersDao>> acspMembersDaoSuppliers = new HashMap<>();
    private final Map<String, Supplier<User>> userDtoSuppliers = new HashMap<>();
    private final Map<String, Supplier<AcspDataDao>> acspDataDaoSuppliers = new HashMap<>();

    private void instantiateAcspMembersDaoSuppliers(){
        final Supplier<AcspMembersDao> ToyStoryBuzzAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "TS001" );
            acspMembersDao.setAcspNumber( "TSA001" );
            acspMembersDao.setUserId( "TSU001" );
            acspMembersDao.setUserRole( UserRoleEnum.OWNER );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 1 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 1 ) );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "TS001", ToyStoryBuzzAcspMembersDao );

        final Supplier<AcspMembersDao> ToyStoryWoodyAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "TS002" );
            acspMembersDao.setAcspNumber( "TSA001" );
            acspMembersDao.setUserId( "TSU002" );
            acspMembersDao.setUserRole( UserRoleEnum.ADMIN );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusMonths( 11 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusMonths( 11 ) );
            acspMembersDao.setAddedBy( "TSU001" );
            acspMembersDao.setRemovedAt( LocalDateTime.now().minusMonths( 10 ) );
            acspMembersDao.setRemovedBy( "TSU001" );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "TS002", ToyStoryWoodyAcspMembersDao );
    }

    private void instantiateUserDtoSuppliers(){
        final Supplier<User> buzzUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "TSU001" );
            userDto.setEmail( "buzz.lightyear@toystory.com" );
            return userDto;
        };
        userDtoSuppliers.put( "TSU001", buzzUserDto );

        final Supplier<User> woodyUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "TSU002" );
            userDto.setEmail( "woody@toystory.com" );
            userDto.setDisplayName( "Woody" );
            return userDto;
        };
        userDtoSuppliers.put( "TSU002", woodyUserDto );
    }

    private void instantiateAcspDataDaoSuppliers(){
        final Supplier<AcspDataDao> ToyStoryAcspDataDao = () -> {
            final var acspDataDao = new AcspDataDao();
            acspDataDao.setId("TSA001");
            acspDataDao.setAcspName( "Toy Story" );
            acspDataDao.setAcspStatus( "active" );
            return acspDataDao;
        };

        acspDataDaoSuppliers.put( "TSA001", ToyStoryAcspDataDao );
    }

    private TestDataManager(){
        instantiateAcspMembersDaoSuppliers();
        instantiateUserDtoSuppliers();
        instantiateAcspDataDaoSuppliers();
    }

    public List<AcspMembersDao> fetchAcspMembersDaos( final String... ids ){
        return Arrays.stream( ids )
                     .map( acspMembersDaoSuppliers::get )
                     .map( Supplier::get )
                     .collect( Collectors.toList() );
    }

    public List<User> fetchUserDtos( final String... ids ){
        return Arrays.stream( ids )
                     .map( userDtoSuppliers::get )
                     .map( Supplier::get )
                     .collect( Collectors.toList() );
    }

    public List<AcspDataDao> fetchAcspDataDaos( final String... ids ){
        return Arrays.stream( ids )
                .map( acspDataDaoSuppliers::get )
                .map( Supplier::get )
                .collect( Collectors.toList() );
    }

}