package uk.gov.companieshouse.acsp.manage.users.model;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

@Document( "acsp_members" )
@CompoundIndex( name = "acsp_user_idx", def = "{ 'acsp_number': 1, 'user_id': 1 }", unique = true )
public class AcspMembersDao {

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
    private UserRoleEnum userRole;

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

    @NotNull
    private String etag;

    @Version
    private Integer version;

    public AcspMembersDao(){}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAcspNumber() {
        return acspNumber;
    }

    public void setAcspNumber(String acspNumber) {
        this.acspNumber = acspNumber;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public UserRoleEnum getUserRole() {
        return userRole;
    }

    public void setUserRole(
            UserRoleEnum userRole) {
        this.userRole = userRole;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    public LocalDateTime getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(LocalDateTime removedAt) {
        this.removedAt = removedAt;
    }

    public String getRemovedBy() {
        return removedBy;
    }

    public void setRemovedBy(String removedBy) {
        this.removedBy = removedBy;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
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
                ", etag='" + etag + '\'' +
                ", version=" + version +
                '}';
    }

}
