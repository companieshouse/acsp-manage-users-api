package uk.gov.companieshouse.acsp.manage.users.membership;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Storage extends MongoRepository<StorageModel, String> {

    @Query( "{ 'user_id': ?0 }" )
    List<StorageModel> fetchActiveAndRemovedMembershipsForUserId( final String userId );

    @Query( "{ 'user_id': ?0, 'status': 'active' }" )
    Optional<StorageModel> fetchActiveMembershipForUserId( final String userId );

    @Query( "{ 'acsp_number': ?0 }" )
    Page<StorageModel> fetchActiveAndRemovedMembershipsForAcspNumber( final String acspNumber, final Pageable pageable );

    @Query( "{ 'acsp_number': ?0, 'status': 'active' }" )
    Page<StorageModel> fetchActiveMembershipsForAcspNumber( final String acspNumber, final Pageable pageable );

    @Query( "{ 'acsp_number': ?0, 'user_role': ?1 }" )
    Page<StorageModel> fetchActiveAndRemovedMembershipsForAcspNumberAndUserRole( final String acspNumber, final String userRole, final Pageable pageable );

    @Query( "{ 'acsp_number': ?0, 'status': 'active', 'user_role': ?1 }" )
    Page<StorageModel> fetchActiveMembershipsForAcspNumberAndUserRole( final String acspNumber, final String userRole, final Pageable pageable );

    @Query( "{ 'user_id': ?0, 'acsp_number': ?1 }" )
    List<StorageModel> fetchActiveAndRemovedMemberships( final String userId, final String acspNumber );

    @Query( "{ 'user_id': ?0, 'acsp_number': ?1, 'status': 'active' }" )
    Optional<StorageModel> fetchActiveMembership( final String userId, final String acspNumber );

    @Query( value = "{ 'acsp_number': ?0, 'user_role': 'owner', 'status': 'active' }", count = true )
    int fetchNumberOfActiveOwners( final String acspNumber );

    @Query( "{ '_id': ?0 }" )
    int updateAcspMembership( final String membershipId, final Update update );

}
