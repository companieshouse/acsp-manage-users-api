package uk.gov.companieshouse.acsp.manage.users.rest;

import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.acspprofile.AcspProfile;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.acspprofile.PrivateAcspProfileResourceHandler;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;

@Service
public class AcspProfileEndpoint {

    private final PrivateAcspProfileResourceHandler privateAcspProfileResourceHandler;

    public AcspProfileEndpoint(PrivateAcspProfileResourceHandler privateAcspProfileResourceHandler) {
        this.privateAcspProfileResourceHandler = privateAcspProfileResourceHandler;
    }

    @Retryable( maxAttempts = 2, retryFor = ApiErrorResponseException.class )
    public ApiResponse<AcspProfile> getAcspInfo( final String acspNumber ) throws ApiErrorResponseException, URIValidationException {
        final var getAcspInfoUrl = String.format( "/authorised-corporate-service-providers/%s", acspNumber );
        return privateAcspProfileResourceHandler.getAcspInfo( getAcspInfoUrl ).execute();
    }

}
