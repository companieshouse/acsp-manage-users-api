package uk.gov.companieshouse.acsp.manage.users.membership;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

@Document( "acsp_members" )
public class StorageModel {

    @Id
    private String id;

    @NotNull
    @Indexed
    @Field( "acsp_number" )
    private String acspNumber;

    @NotNull
    @Indexed
    @Field( "user_id" )
    private String userId;

    @NotNull
    @Field( "user_role" )
    private String userRole;

    @CreatedDate
    @Field( "created_at" )
    private LocalDateTime createdAt;

    @Field( "added_at" )
    private LocalDateTime addedAt;

    @Field( "added_by" )
    private String addedBy;

    @Field( "removed_at" )
    private LocalDateTime removedAt;

    @Field( "removed_by" )
    private String removedBy;

    private String status;

    @NotNull
    private String etag;

    @Version
    private Integer version;

    public StorageModel(){}

    public void setId( final String id ){
        this.id = id;
    }

    public StorageModel id( final String id ){
        setId( id );
        return this;
    }

    public String getId(){
        return id;
    }

    public void setAcspNumber( final String acspNumber ){
        this.acspNumber = acspNumber;
    }

    public StorageModel acspNumber( final String acspNumber ){
        setAcspNumber( acspNumber );
        return this;
    }

    public String getAcspNumber(){
        return acspNumber;
    }

    public void setUserId( final String userId ) {
        this.userId = userId;
    }

    public StorageModel userId( final String userId ){
        setUserId( userId );
        return this;
    }

    public String getUserId(){
        return userId;
    }

    public void setUserRole( final String userRole ) {
        this.userRole = userRole;
    }

    public StorageModel userRole( final String userRole ){
        setUserRole( userRole );
        return this;
    }

    public UserRoleEnum getUserRole(){
        return UserRoleEnum.fromValue( userRole );
    }

    public void setCreatedAt( final LocalDateTime createdAt ){
        this.createdAt = createdAt;
    }

    public StorageModel createdAt( final LocalDateTime createdAt ){
        setCreatedAt( createdAt );
        return this;
    }

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }

    public void setAddedAt( final LocalDateTime addedAt ){
        this.addedAt = addedAt;
    }

    public StorageModel addedAt( final LocalDateTime addedAt ){
        setAddedAt( addedAt );
        return this;
    }

    public LocalDateTime getAddedAt(){
        return addedAt;
    }

    public void setAddedBy( final String addedBy ) {
        this.addedBy = addedBy;
    }

    public StorageModel addedBy( final String addedBy ){
        setAddedBy( addedBy );
        return this;
    }

    public String getAddedBy(){
        return addedBy;
    }

    public void setRemovedAt( final LocalDateTime removedAt ) {
        this.removedAt = removedAt;
    }

    public StorageModel removedAt( final LocalDateTime removedAt ){
        setRemovedAt( removedAt );
        return this;
    }

    public LocalDateTime getRemovedAt(){
        return removedAt;
    }

    public void setRemovedBy( final String removedBy ) {
        this.removedBy = removedBy;
    }

    public StorageModel removedBy( final String removedBy ){
        setRemovedBy( removedBy );
        return this;
    }

    public String getRemovedBy(){
        return removedBy;
    }

    public void setEtag( final String etag ){
        this.etag = etag;
    }

    public StorageModel etag( final String etag ){
        setEtag( etag );
        return this;
    }

    public String getEtag(){
        return etag;
    }

    public void setVersion( final Integer version ){
        this.version = version;
    }

    public StorageModel version( final Integer version ){
        setVersion( version );
        return this;
    }

    public Integer getVersion(){
        return version;
    }

    public void setStatus( final String status ){
        this.status = status;
    }

    public StorageModel status( final String status ){
        setStatus( status );
        return this;
    }

    public String getStatus(){
        return status;
    }

    @Override
    public String toString(){
        return "AcspMembersDao{" +
                "id='" + id + '\'' +
                ", acspNumber='" + acspNumber + '\'' +
                ", userId='" + userId + '\'' +
                ", userRole=" + userRole +
                ", createdAt=" + createdAt +
                ", addedAt=" + addedAt +
                ", addedBy='" + addedBy + '\'' +
                ", removedAt=" + removedAt +
                ", removedBy='" + removedBy + '\'' +
                ", status=" + status +
                ", etag='" + etag + '\'' +
                ", version=" + version +
                '}';
    }

}
