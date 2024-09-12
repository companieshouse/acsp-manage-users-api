package uk.gov.companieshouse.acsp.manage.users.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.acsp.manage.users.utils.ApiClientUtil;
import uk.gov.companieshouse.api.acspprofile.AcspProfile;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.acspprofile.request.PrivateAcspProfileAcspInfoGet;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;

@Service
public class AcspProfileEndpoint {

    @Value( "${api.url}" )
    private String apiUrl;

    private final ApiClientUtil apiClientUtil;

    @Autowired
    public AcspProfileEndpoint( final ApiClientUtil apiClientUtil ) {
        this.apiClientUtil = apiClientUtil;
    }

    public PrivateAcspProfileAcspInfoGet createGetAcspInfoRequest( final String acspNumber ){
        final var getAcspInfoUrl = String.format( "/authorised-corporate-service-providers/%s", acspNumber );
        return apiClientUtil.getInternalApiClient( apiUrl )
                .privateAcspProfileResourceHandler()
                .getAcspInfo( getAcspInfoUrl );
    }

    public ApiResponse<AcspProfile> getAcspInfo( final String acspNumber ) throws ApiErrorResponseException, URIValidationException {
        return createGetAcspInfoRequest( acspNumber ).execute();
    }

}
