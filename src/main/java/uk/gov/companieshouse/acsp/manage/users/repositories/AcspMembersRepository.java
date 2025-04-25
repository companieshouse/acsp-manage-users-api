package uk.gov.companieshouse.acsp.manage.users.repositories;

import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcspMembersRepository extends MongoRepository<AcspMembersDao, String> {

    @Query( "{ 'user_id': ?0 }" )
    List<AcspMembersDao> fetchActiveAndRemovedMembershipsForUserId( final String userId );

    @Query( "{ '$or': [ { 'user_id': { '$ne': null, '$eq': ?0 } }, { 'user_email': { '$ne': null, '$eq': ?1 } } ], 'status': { $in: ?2 } }" )
    List<AcspMembersDao> fetchMembershipsForUserAndStatus( final String userId, final String userEmail, final Set<String> statuses );

    @Query( "{ 'user_id': ?0, 'status': 'active' }" )
    Optional<AcspMembersDao> fetchActiveMembershipForUserId( final String userId );

    @Query( "{ 'acsp_number': ?0 }" )
    Page<AcspMembersDao> fetchActiveAndRemovedMembershipsForAcspNumber( final String acspNumber, final Pageable pageable );

    @Query( "{ 'acsp_number': ?0, 'status': 'active' }" )
    Page<AcspMembersDao> fetchActiveMembershipsForAcspNumber( final String acspNumber, final Pageable pageable );

    @Query( "{ 'acsp_number': ?0, 'user_role': ?1 }" )
    Page<AcspMembersDao> fetchActiveAndRemovedMembershipsForAcspNumberAndUserRole( final String acspNumber, final String userRole, final Pageable pageable );

    @Query( "{ 'acsp_number': ?0, 'status': 'active', 'user_role': ?1 }" )
    Page<AcspMembersDao> fetchActiveMembershipsForAcspNumberAndUserRole( final String acspNumber, final String userRole, final Pageable pageable );

    @Query( "{ 'user_id': ?0, 'acsp_number': ?1 }" )
    List<AcspMembersDao> fetchActiveAndRemovedMemberships( final String userId, final String acspNumber );

    @Query( "{ 'user_id': ?0, 'acsp_number': ?1, 'status': 'active' }" )
    Optional<AcspMembersDao> fetchActiveMembership( final String userId, final String acspNumber );

    @Query( value = "{ 'acsp_number': ?0, 'user_role': 'owner', 'status': 'active' }", count = true )
    int fetchNumberOfActiveOwners( final String acspNumber );

    @Query( "{ '_id': ?0 }" )
    int updateAcspMembership( final String membershipId, final Update update );

}
