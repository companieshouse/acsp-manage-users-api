package uk.gov.companieshouse.acsp.manage.users.common;

import static uk.gov.companieshouse.GenerateEtagUtil.generateEtag;
import static uk.gov.companieshouse.acsp.manage.users.common.ParsingUtils.localDateTimeToOffsetDateTime;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import uk.gov.companieshouse.acsp.manage.users.model.AcspDataDao;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.AcspStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum;
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
            acspMembersDao.setUserRole( UserRoleEnum.OWNER.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 1 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 1 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "TS001", ToyStoryBuzzAcspMembersDao );

        final Supplier<AcspMembersDao> ToyStoryWoodyAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "TS002" );
            acspMembersDao.setAcspNumber( "TSA001" );
            acspMembersDao.setUserId( "TSU002" );
            acspMembersDao.setUserRole( UserRoleEnum.ADMIN.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusMonths( 11 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusMonths( 11 ) );
            acspMembersDao.setAddedBy( "TSU001" );
            acspMembersDao.setRemovedAt( LocalDateTime.now().minusMonths( 10 ) );
            acspMembersDao.setRemovedBy( "TSU001" );
            acspMembersDao.setStatus( MembershipStatusEnum.REMOVED.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "TS002", ToyStoryWoodyAcspMembersDao );

        final Supplier<AcspMembersDao> NetflixBuzzAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "NF001" );
            acspMembersDao.setAcspNumber( "NFA001" );
            acspMembersDao.setUserId( "TSU001" );
            acspMembersDao.setUserRole( UserRoleEnum.ADMIN.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusMonths( 5 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusMonths( 5 ) );
            acspMembersDao.setAddedBy( "TSU002" );
            acspMembersDao.setRemovedAt( LocalDateTime.now().minusMonths( 4 ) );
            acspMembersDao.setRemovedBy( "TSU002" );
            acspMembersDao.setStatus( MembershipStatusEnum.REMOVED.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "NF001", NetflixBuzzAcspMembersDao );

        final Supplier<AcspMembersDao> NetflixWoodyAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "NF002" );
            acspMembersDao.setAcspNumber( "NFA001" );
            acspMembersDao.setUserId( "TSU002" );
            acspMembersDao.setUserRole( UserRoleEnum.OWNER.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 2 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 2 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "NF002", NetflixWoodyAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyJimmyAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM001" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU001" );
            acspMembersDao.setUserRole( UserRoleEnum.OWNER.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 10 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 10 ) );
            acspMembersDao.setRemovedBy( "COMU002" );
            acspMembersDao.setRemovedAt( LocalDateTime.now().minusYears( 8 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.REMOVED.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM001", ComedyJimmyAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyShaunAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM002" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU002" );
            acspMembersDao.setUserRole( UserRoleEnum.OWNER.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 9 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 9 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM002", ComedyShaunAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyDavidAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM003" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU003" );
            acspMembersDao.setUserRole( UserRoleEnum.ADMIN.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 8 ) );
            acspMembersDao.setAddedBy( "COMU002" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 8 ) );
            acspMembersDao.setRemovedBy( "COMU002" );
            acspMembersDao.setRemovedAt( LocalDateTime.now().minusYears( 7 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.REMOVED.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM003", ComedyDavidAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyCharlieAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM004" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU004" );
            acspMembersDao.setUserRole( UserRoleEnum.ADMIN.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 8 ) );
            acspMembersDao.setAddedBy( "COMU002" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 8 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM004", ComedyCharlieAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyKatherineAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM005" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU005" );
            acspMembersDao.setUserRole( UserRoleEnum.ADMIN.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 7 ) );
            acspMembersDao.setAddedBy( "COMU004" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 7 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue());
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM005", ComedyKatherineAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyRussellAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM006" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU006" );
            acspMembersDao.setUserRole( UserRoleEnum.STANDARD.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 6 ) );
            acspMembersDao.setAddedBy( "COMU002" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 6 ) );
            acspMembersDao.setRemovedBy( "COMU002" );
            acspMembersDao.setRemovedAt( LocalDateTime.now().minusYears( 5 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.REMOVED.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM006", ComedyRussellAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyFrankieAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM007" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU007" );
            acspMembersDao.setUserRole( UserRoleEnum.STANDARD.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 3 ) );
            acspMembersDao.setAddedBy( "COMU002" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 3 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM007", ComedyFrankieAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyMickyAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM008" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU008" );
            acspMembersDao.setUserRole( UserRoleEnum.STANDARD.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 2 ) );
            acspMembersDao.setAddedBy( "COMU004" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 2 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM008", ComedyMickyAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyStephenAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM009" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU009" );
            acspMembersDao.setUserRole( UserRoleEnum.OWNER.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 20 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 20 ) );
            acspMembersDao.setRemovedBy( "COMU002" );
            acspMembersDao.setRemovedAt( LocalDateTime.now().minusYears( 1 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.REMOVED.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM009", ComedyStephenAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyAlanAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM010" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU010" );
            acspMembersDao.setUserRole( UserRoleEnum.OWNER.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 19 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 19 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM010", ComedyAlanAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyDaraAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM011" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU011" );
            acspMembersDao.setUserRole( UserRoleEnum.ADMIN.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setAddedBy( "COMU002" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setRemovedBy( "COMU004" );
            acspMembersDao.setRemovedAt( LocalDateTime.now().minusYears( 1 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.REMOVED.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM011", ComedyDaraAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyJackAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM012" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU012" );
            acspMembersDao.setUserRole( UserRoleEnum.ADMIN.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setAddedBy( "COMU002" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM012", ComedyJackAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyJonAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM013" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU013" );
            acspMembersDao.setUserRole( UserRoleEnum.ADMIN.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setAddedBy( "COMU004" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM013", ComedyJonAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyMichealAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM014" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU014" );
            acspMembersDao.setUserRole( UserRoleEnum.STANDARD.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setAddedBy( "COMU002" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setEtag( generateEtag() );
            acspMembersDao.setRemovedBy( "COMU004" );
            acspMembersDao.setRemovedAt( LocalDateTime.now().minusYears( 2 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.REMOVED.getValue() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM014", ComedyMichealAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyJoAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM015" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU015" );
            acspMembersDao.setUserRole( UserRoleEnum.STANDARD.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setAddedBy( "COMU002" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM015", ComedyJoAcspMembersDao );

        final Supplier<AcspMembersDao> ComedyHenningAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "COM016" );
            acspMembersDao.setAcspNumber( "COMA001" );
            acspMembersDao.setUserId( "COMU016" );
            acspMembersDao.setUserRole( UserRoleEnum.STANDARD.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setAddedBy( "COMU004" );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "COM016", ComedyHenningAcspMembersDao );

        final Supplier<AcspMembersDao> WitcherGeraltAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "WIT001" );
            acspMembersDao.setAcspNumber( "WITA001" );
            acspMembersDao.setUserId( "WITU001" );
            acspMembersDao.setUserRole( UserRoleEnum.OWNER.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 20 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 20 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "WIT001", WitcherGeraltAcspMembersDao );

        final Supplier<AcspMembersDao> WitcherYenneferAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "WIT002" );
            acspMembersDao.setAcspNumber( "WITA001" );
            acspMembersDao.setUserId( "WITU002" );
            acspMembersDao.setUserRole( UserRoleEnum.ADMIN.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusMonths( 11 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusMonths( 11 ) );
            acspMembersDao.setAddedBy( "WITU001" );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "WIT002", WitcherYenneferAcspMembersDao );

        final Supplier<AcspMembersDao> WitcherDandelionAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "WIT003" );
            acspMembersDao.setAcspNumber( "WITA001" );
            acspMembersDao.setUserId( "WITU003" );
            acspMembersDao.setUserRole( UserRoleEnum.STANDARD.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusMonths( 10 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusMonths( 10 ) );
            acspMembersDao.setAddedBy( "WITU002" );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "WIT003", WitcherDandelionAcspMembersDao );

        final Supplier<AcspMembersDao> WitcherDemoAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "WIT004" );
            acspMembersDao.setAcspNumber( "WITA001" );
            acspMembersDao.setUserId( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
            acspMembersDao.setUserRole( UserRoleEnum.OWNER.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 21 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 21 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "WIT004", WitcherDemoAcspMembersDao );

        final Supplier<AcspMembersDao> NeighboursKarlAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "NEI001" );
            acspMembersDao.setAcspNumber( "NEIA001" );
            acspMembersDao.setUserId( "NEIU001" );
            acspMembersDao.setUserRole( UserRoleEnum.OWNER.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 25 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 25 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "NEI001", NeighboursKarlAcspMembersDao );

        final Supplier<AcspMembersDao> NeighboursHaroldAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "NEI002" );
            acspMembersDao.setAcspNumber( "NEIA001" );
            acspMembersDao.setUserId( "NEIU002" );
            acspMembersDao.setUserRole( UserRoleEnum.ADMIN.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 11 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 11 ) );
            acspMembersDao.setAddedBy( "NEIU001" );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "NEI002", NeighboursHaroldAcspMembersDao );

        final Supplier<AcspMembersDao> NeighboursToadieAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "NEI003" );
            acspMembersDao.setAcspNumber( "NEIA001" );
            acspMembersDao.setUserId( "NEIU003" );
            acspMembersDao.setUserRole( UserRoleEnum.STANDARD.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setAddedBy( "NEIU002" );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "NEI003", NeighboursToadieAcspMembersDao );

        final Supplier<AcspMembersDao> NeighboursDemoAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "NEI004" );
            acspMembersDao.setAcspNumber( "NEIA001" );
            acspMembersDao.setUserId( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
            acspMembersDao.setUserRole( UserRoleEnum.ADMIN.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 26 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 26 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "NEI004", NeighboursDemoAcspMembersDao );

        final Supplier<AcspMembersDao> XmenWolverineAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "XME001" );
            acspMembersDao.setAcspNumber( "XMEA001" );
            acspMembersDao.setUserId( "XMEU001" );
            acspMembersDao.setUserRole( UserRoleEnum.OWNER.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 14 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 14 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "XME001", XmenWolverineAcspMembersDao );

        final Supplier<AcspMembersDao> XmenCyclopsAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "XME002" );
            acspMembersDao.setAcspNumber( "XMEA001" );
            acspMembersDao.setUserId( "XMEU002" );
            acspMembersDao.setUserRole( UserRoleEnum.ADMIN.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 11 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 11 ) );
            acspMembersDao.setAddedBy( "XMEU001" );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "XME002", XmenCyclopsAcspMembersDao );

        final Supplier<AcspMembersDao> XmenGambitAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "XME003" );
            acspMembersDao.setAcspNumber( "XMEA001" );
            acspMembersDao.setUserId( "XMEU003" );
            acspMembersDao.setUserRole( UserRoleEnum.STANDARD.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 4 ) );
            acspMembersDao.setAddedBy( "XMEU002" );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "XME003", XmenGambitAcspMembersDao );

        final Supplier<AcspMembersDao> XmenDemoAcspMembersDao = () -> {
            final var acspMembersDao = new AcspMembersDao();
            acspMembersDao.setId( "XME004" );
            acspMembersDao.setAcspNumber( "XMEA001" );
            acspMembersDao.setUserId( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
            acspMembersDao.setUserRole( UserRoleEnum.STANDARD.getValue() );
            acspMembersDao.setCreatedAt( LocalDateTime.now().minusYears( 15 ) );
            acspMembersDao.setAddedAt( LocalDateTime.now().minusYears( 15 ) );
            acspMembersDao.setStatus( MembershipStatusEnum.ACTIVE.getValue() );
            acspMembersDao.setEtag( generateEtag() );
            return acspMembersDao;
        };
        acspMembersDaoSuppliers.put( "XME004", XmenDemoAcspMembersDao );
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

        final Supplier<User> geraltUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "WITU001" );
            userDto.setEmail( "geralt@witcher.com" );
            userDto.setDisplayName( "Geralt of Rivia" );
            return userDto;
        };
        userDtoSuppliers.put( "WITU001", geraltUserDto );

        final Supplier<User> yenneferUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "WITU002" );
            userDto.setEmail( "yennefer@witcher.com" );
            userDto.setDisplayName( "Yennefer of Vengerberg" );
            return userDto;
        };
        userDtoSuppliers.put( "WITU002", yenneferUserDto );

        final Supplier<User> dandelionUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "WITU003" );
            userDto.setEmail( "dandelion@witcher.com" );
            return userDto;
        };
        userDtoSuppliers.put( "WITU003", dandelionUserDto );

        final Supplier<User> karlUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "NEIU001" );
            userDto.setEmail( "karl.kennedy@neighbours.com" );
            return userDto;
        };
        userDtoSuppliers.put( "NEIU001", karlUserDto );

        final Supplier<User> haroldUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "NEIU002" );
            userDto.setEmail( "harold.bishop@neighbours.com" );
            userDto.setDisplayName( "Harold Bishop" );
            return userDto;
        };
        userDtoSuppliers.put( "NEIU002", haroldUserDto );

        final Supplier<User> toadieUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "NEIU003" );
            userDto.setEmail( "toadie@neighbours.com" );
            userDto.setDisplayName( "Toadie" );
            return userDto;
        };
        userDtoSuppliers.put( "NEIU003", toadieUserDto );

        final Supplier<User> wolverineUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "XMEU001" );
            userDto.setEmail( "wolverine@xmen.com" );
            userDto.setDisplayName( "Wolverine" );
            return userDto;
        };
        userDtoSuppliers.put( "XMEU001", wolverineUserDto );

        final Supplier<User> cyclopsUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "XMEU002" );
            userDto.setEmail( "cyclops@xmen.com" );
            return userDto;
        };
        userDtoSuppliers.put( "XMEU002", cyclopsUserDto );

        final Supplier<User> gambitUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "XMEU003" );
            userDto.setEmail( "gambit@xmen.com" );
            userDto.setDisplayName( "Gambit" );
            return userDto;
        };
        userDtoSuppliers.put( "XMEU003", gambitUserDto );

        final Supplier<User> demoUserDto = () -> {
            final var userDto = new User();
            userDto.setUserId( "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
            userDto.setEmail( "demo@ch.gov.uk" );
            return userDto;
        };
        userDtoSuppliers.put( "67ZeMsvAEgkBWs7tNKacdrPvOmQ", demoUserDto );

    }

    private void instantiateAcspDataDaoSuppliers(){
        final Supplier<AcspDataDao> ToyStoryAcspDataDao = () -> {
            final var acspDataDao = new AcspDataDao();
            acspDataDao.setId("TSA001");
            acspDataDao.setAcspName( "Toy Story" );
            acspDataDao.setAcspStatus("active");
            return acspDataDao;
        };
        acspDataDaoSuppliers.put( "TSA001", ToyStoryAcspDataDao );

        final Supplier<AcspDataDao> NetflixAcspDataDao = () -> {
            final var acspDataDao = new AcspDataDao();
            acspDataDao.setId("NFA001");
            acspDataDao.setAcspName( "Netflix" );
            acspDataDao.setAcspStatus("active");
            return acspDataDao;
        };
        acspDataDaoSuppliers.put( "NFA001", NetflixAcspDataDao );

        final Supplier<AcspDataDao> comedyAcspDataDao = () -> {
            final var acspDataDao = new AcspDataDao();
            acspDataDao.setId("COMA001");
            acspDataDao.setAcspName( "Comedy" );
            acspDataDao.setAcspStatus("active");
            return acspDataDao;
        };
        acspDataDaoSuppliers.put( "COMA001", comedyAcspDataDao );

        final Supplier<AcspDataDao> witcherDataDao = () -> {
            final var acspDataDao = new AcspDataDao();
            acspDataDao.setId("WITA001");
            acspDataDao.setAcspName( "Witcher" );
            acspDataDao.setAcspStatus("active");
            return acspDataDao;
        };
        acspDataDaoSuppliers.put( "WITA001", witcherDataDao );

        final Supplier<AcspDataDao> neighboursDataDao = () -> {
            final var acspDataDao = new AcspDataDao();
            acspDataDao.setId("NEIA001");
            acspDataDao.setAcspName( "Neighbours" );
            acspDataDao.setAcspStatus("active");
            return acspDataDao;
        };
        acspDataDaoSuppliers.put( "NEIA001", neighboursDataDao );

        final Supplier<AcspDataDao> xmenDataDao = () -> {
            final var acspDataDao = new AcspDataDao();
            acspDataDao.setId("XMEA001");
            acspDataDao.setAcspName( "XMen" );
            acspDataDao.setAcspStatus("active");
            return acspDataDao;
        };
        acspDataDaoSuppliers.put( "XMEA001", xmenDataDao );
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

    private AcspMembership fetchAcspMembershipDto( final String id ){
        final var acspMembersDao = fetchAcspMembersDaos( id ).getFirst();
        final var userData = fetchUserDtos( acspMembersDao.getUserId() ).getFirst();
        final var acspData = fetchAcspDataDaos( acspMembersDao.getAcspNumber() ).getFirst();

        return new AcspMembership()
                .id( acspMembersDao.getId() )
                .userId( acspMembersDao.getUserId() )
                .userEmail( userData.getEmail() )
                .userDisplayName( Objects.isNull( userData.getDisplayName() ) ? "Not Provided" : userData.getDisplayName() )
                .acspNumber( acspMembersDao.getAcspNumber() )
                .acspName( acspData.getAcspName() )
                .acspStatus( AcspStatusEnum.fromValue( acspData.getAcspStatus() ) )
                .userRole( UserRoleEnum.fromValue( acspMembersDao.getUserRole() ) )
                .membershipStatus( MembershipStatusEnum.fromValue( acspMembersDao.getStatus() ) )
                .addedAt( localDateTimeToOffsetDateTime( acspMembersDao.getAddedAt() ) )
                .addedBy( acspMembersDao.getAddedBy() )
                .removedAt( localDateTimeToOffsetDateTime( acspMembersDao.getRemovedAt() ) )
                .removedBy( acspMembersDao.getRemovedBy() )
                .kind( "acsp-membership" )
                .etag( acspMembersDao.getEtag() );
    }


    public List<AcspMembership> fetchAcspMembershipDtos( final String... ids ){
        final var acspMembershipDtos = new LinkedList<AcspMembership>();
        for ( final String id: ids ) {
            final var acspMembershipDto = fetchAcspMembershipDto( id );
            acspMembershipDtos.add( acspMembershipDto );
        }
        return acspMembershipDtos;
    }



}