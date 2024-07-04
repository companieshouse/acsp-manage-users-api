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

        final Supplier<AcspMembersDao> NetflixBuzzAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "NF001" );
            acspMembersDao.setAcspNumber( "NFA001" );
            acspMembersDao.setUserId( "TSU001" );
            acspMembersDao.setUserRole( UserRoleEnum.ADMIN );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusMonths( 5 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusMonths( 5 ) );
            acspMembersDao.setAddedBy( "TSU002" );
            acspMembersDao.setRemovedAt( LocalDateTime.now().minusMonths( 4 ) );
            acspMembersDao.setRemovedBy( "TSU002" );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "NF001", NetflixBuzzAcspMembersDao );

        final Supplier<AcspMembersDao> NetflixWoodyAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "NF002" );
            acspMembersDao.setAcspNumber( "NFA001" );
            acspMembersDao.setUserId( "TSU002" );
            acspMembersDao.setUserRole( UserRoleEnum.OWNER );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 2 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 2 ) );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "NF002", NetflixWoodyAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyJimmyAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM001" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU001" );
            acspMembersDao.setUserRole( UserRoleEnum.OWNER );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 10 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 10 ) );
            acspMembersDao.setRemovedBy( "COMU002" );
            acspMembersDao.setRemovedAt( LocalDateTime.now().minusYears( 8 ) );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM001", ComedyJimmyAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyShaunAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM002" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU002" );
            acspMembersDao.setUserRole( UserRoleEnum.OWNER );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 9 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 9 ) );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM002", ComedyShaunAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyDavidAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM003" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU003" );
            acspMembersDao.setUserRole( UserRoleEnum.ADMIN );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 8 ) );
            acspMembersDao.setAddedBy( "COMU002" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 8 ) );
            acspMembersDao.setRemovedBy( "COMU002" );
            acspMembersDao.setRemovedAt( LocalDateTime.now().minusYears( 7 ) );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM003", ComedyDavidAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyCharlieAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM004" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU004" );
            acspMembersDao.setUserRole( UserRoleEnum.ADMIN );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 8 ) );
            acspMembersDao.setAddedBy( "COMU002" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 8 ) );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM004", ComedyCharlieAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyKatherineAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM005" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU005" );
            acspMembersDao.setUserRole( UserRoleEnum.ADMIN );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 7 ) );
            acspMembersDao.setAddedBy( "COMU004" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 7 ) );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM005", ComedyKatherineAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyRussellAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM006" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU006" );
            acspMembersDao.setUserRole( UserRoleEnum.STANDARD );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 6 ) );
            acspMembersDao.setAddedBy( "COMU002" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 6 ) );
            acspMembersDao.setRemovedBy( "COMU002" );
            acspMembersDao.setRemovedAt( LocalDateTime.now().minusYears( 5 ) );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM006", ComedyRussellAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyFrankieAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM007" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU007" );
            acspMembersDao.setUserRole( UserRoleEnum.STANDARD );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 3 ) );
            acspMembersDao.setAddedBy( "COMU002" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 3 ) );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM007", ComedyFrankieAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyMickyAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM008" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU008" );
            acspMembersDao.setUserRole( UserRoleEnum.STANDARD );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 2 ) );
            acspMembersDao.setAddedBy( "COMU004" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 2 ) );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM008", ComedyMickyAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyStephenAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM009" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU009" );
            acspMembersDao.setUserRole( UserRoleEnum.OWNER );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 20 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 20 ) );
            acspMembersDao.setRemovedBy( "COMU002" );
            acspMembersDao.setRemovedAt( LocalDateTime.now().minusYears( 1 ) );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM009", ComedyStephenAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyAlanAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM010" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU010" );
            acspMembersDao.setUserRole( UserRoleEnum.OWNER );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 19 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 19 ) );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM010", ComedyAlanAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyDaraAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM011" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU011" );
            acspMembersDao.setUserRole( UserRoleEnum.ADMIN );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setAddedBy( "COMU002" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setRemovedBy( "COMU004" );
            acspMembersDao.setRemovedAt( LocalDateTime.now().minusYears( 1 ) );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM011", ComedyDaraAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyJackAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM012" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU012" );
            acspMembersDao.setUserRole( UserRoleEnum.ADMIN );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setAddedBy( "COMU002" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM012", ComedyJackAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyJonAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM013" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU013" );
            acspMembersDao.setUserRole( UserRoleEnum.ADMIN );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setAddedBy( "COMU004" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM013", ComedyJonAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyMichealAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM014" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU014" );
            acspMembersDao.setUserRole( UserRoleEnum.STANDARD );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setAddedBy( "COMU002" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setEtag( generateEtag() );
            acspMembersDao.setRemovedBy( "COMU004" );
            acspMembersDao.setRemovedAt( LocalDateTime.now().minusYears( 2 ) );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM014", ComedyMichealAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyJoAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM015" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU015" );
            acspMembersDao.setUserRole( UserRoleEnum.STANDARD );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setAddedBy( "COMU002" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM015", ComedyJoAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyHenningAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM016" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU016" );
            acspMembersDao.setUserRole( UserRoleEnum.STANDARD );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setAddedBy( "COMU004" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM016", ComedyHenningAcspMembersDao );
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

        final Supplier<User> jimmyUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "COMU001" );
            userDto.setEmail( "jimmy.carr@comedy.com" );
            userDto.setDisplayName( "Jimmy Carr" );
            return userDto;
        };
        userDtoSuppliers.put( "COMU001", jimmyUserDto );

        final Supplier<User> shaunUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "COMU002" );
            userDto.setEmail( "shaun.lock@comedy.com" );
            userDto.setDisplayName( "Shaun Lock" );
            return userDto;
        };
        userDtoSuppliers.put( "COMU002", shaunUserDto );

        final Supplier<User> davidUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "COMU003" );
            userDto.setEmail( "david.mitchell@comedy.com" );
            userDto.setDisplayName( "David Mitchell" );
            return userDto;
        };
        userDtoSuppliers.put( "COMU003", davidUserDto );

        final Supplier<User> charlieUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "COMU004" );
            userDto.setEmail( "charlie.brooker@comedy.com" );
            userDto.setDisplayName( "Charlie Brooker" );
            return userDto;
        };
        userDtoSuppliers.put( "COMU004", charlieUserDto );

        final Supplier<User> katherineUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "COMU005" );
            userDto.setEmail( "kartherine.ryan@comedy.com" );
            userDto.setDisplayName( "Katherine Ryan" );
            return userDto;
        };
        userDtoSuppliers.put( "COMU005", katherineUserDto );

        final Supplier<User> russellUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "COMU006" );
            userDto.setEmail( "russell.brand@comedy.com" );
            userDto.setDisplayName( "Russell Brand" );
            return userDto;
        };
        userDtoSuppliers.put( "COMU006", russellUserDto );

        final Supplier<User> frankieUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "COMU007" );
            userDto.setEmail( "frankie.boyle@comedy.com" );
            userDto.setDisplayName( "Frankie Boyle" );
            return userDto;
        };
        userDtoSuppliers.put( "COMU007", frankieUserDto );

        final Supplier<User> mickyUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "COMU008" );
            userDto.setEmail( "micky.flanagan@comedy.com" );
            userDto.setDisplayName( "Micky Flanagan" );
            return userDto;
        };
        userDtoSuppliers.put( "COMU008", mickyUserDto );

        final Supplier<User> stephenUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "COMU009" );
            userDto.setEmail( "stephen.fry@comedy.com" );
            return userDto;
        };
        userDtoSuppliers.put( "COMU009", stephenUserDto );

        final Supplier<User> alanUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "COMU010" );
            userDto.setEmail( "alan.davies@comedy.com" );
            return userDto;
        };
        userDtoSuppliers.put( "COMU010", alanUserDto );

        final Supplier<User> daraUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "COMU011" );
            userDto.setEmail( "dara.obrien@comedy.com" );
            return userDto;
        };
        userDtoSuppliers.put( "COMU011", daraUserDto );

        final Supplier<User> jackUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "COMU012" );
            userDto.setEmail( "jack.whitehall@comedy.com" );
            return userDto;
        };
        userDtoSuppliers.put( "COMU012", jackUserDto );

        final Supplier<User> jonUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "COMU013" );
            userDto.setEmail( "jon.richardson@comedy.com" );
            return userDto;
        };
        userDtoSuppliers.put( "COMU013", jonUserDto );

        final Supplier<User> michaelUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "COMU014" );
            userDto.setEmail( "michael.mcintyre@comedy.com" );
            return userDto;
        };
        userDtoSuppliers.put( "COMU014", michaelUserDto );

        final Supplier<User> joUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "COMU015" );
            userDto.setEmail( "jo.brand@comedy.com" );
            return userDto;
        };
        userDtoSuppliers.put( "COMU015", joUserDto );

        final Supplier<User> henningUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "COMU016" );
            userDto.setEmail( "henning.wehn@comedy.com" );
            return userDto;
        };
        userDtoSuppliers.put( "COMU016", henningUserDto );

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

        final Supplier<AcspDataDao> NetflixAcspDataDao = () -> {
            final var acspDataDao = new AcspDataDao();
            acspDataDao.setId("NFA001");
            acspDataDao.setAcspName( "Netflix" );
            acspDataDao.setAcspStatus( "active" );
            return acspDataDao;
        };
        acspDataDaoSuppliers.put( "NFA001", NetflixAcspDataDao );

        final Supplier<AcspDataDao> comedyAcspDataDao = () -> {
            final var acspDataDao = new AcspDataDao();
            acspDataDao.setId("COMA001");
            acspDataDao.setAcspName( "Comedy" );
            acspDataDao.setAcspStatus( "active" );
            return acspDataDao;
        };
        acspDataDaoSuppliers.put( "COMA001", comedyAcspDataDao );
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