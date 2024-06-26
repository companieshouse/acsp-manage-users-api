package uk.gov.companieshouse.acsp.manage.users.model;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document( "acsp_data" )
public class AcspDataDao {

    @Id
    private String id;

    @NotNull
    @Field( "acsp_name" )
    private String acspName;

    @NotNull
    @Field( "acsp_status" )
    private String acspStatus;

    @Version
    private Integer version;

    public AcspDataDao(){};

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAcspName() {
        return acspName;
    }

    public void setAcspName(String acspName) {
        this.acspName = acspName;
    }

    public String getAcspStatus() {
        return acspStatus;
    }

    public void setAcspStatus(String acspStatus) {
        this.acspStatus = acspStatus;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "AcspDataDao{" +
                "id='" + id + '\'' +
                ", acspName='" + acspName + '\'' +
                ", acspStatus='" + acspStatus + '\'' +
                ", version=" + version +
                '}';
    }

}
