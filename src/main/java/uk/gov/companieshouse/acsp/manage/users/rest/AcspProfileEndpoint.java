package uk.gov.companieshouse.acsp.manage.users.rest;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getXRequestId;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acspprofile.AcspProfile;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.acspprofile.PrivateAcspProfileResourceHandler;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class AcspProfileEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    private final PrivateAcspProfileResourceHandler privateAcspProfileResourceHandler;

    public AcspProfileEndpoint(PrivateAcspProfileResourceHandler privateAcspProfileResourceHandler) {
        this.privateAcspProfileResourceHandler = privateAcspProfileResourceHandler;
    }

    public AcspProfile getAcspInfo( final String acspNumber ) {
        final var xRequestId = getXRequestId();
        final var getAcspInfoUrl = String.format( "/authorised-corporate-service-providers/%s", acspNumber );
      try {
        LOG.infoContext(xRequestId, String.format(
          "Sending request to acsp-profile-data-api: GET /authorised-corporate-service-providers/{acsp_number}. Attempting to retrieve acsp: %s",
          acspNumber), null);
        final var response = privateAcspProfileResourceHandler.getAcspInfo( getAcspInfoUrl )
          .execute();
        if(response.hasErrors()){
          throw new InternalServerErrorRuntimeException("acsp-manage-users-api - Failed to find Acsp Profile");
        }
        return response.getData();
      } catch ( ApiErrorResponseException exception ) {
          if ( exception.getStatusCode() == 404 ) {
              LOG.errorContext(xRequestId, String.format("Could not find profile for Acsp id: %s", acspNumber), exception, null);
              throw new NotFoundRuntimeException("acsp-manage-users-api", "Failed to find Acsp Profile");
          } else {
              LOG.errorContext(xRequestId, String.format("Failed to retrieve profile for Acsp id: %s", acspNumber), exception, null);
              throw new InternalServerErrorRuntimeException("Failed to retrieve Acsp Profile");
          }
      } catch ( URIValidationException exception ) {
          LOG.errorContext(xRequestId, String.format("Failed to fetch profile for Acsp %s, because uri was incorrectly formatted", acspNumber), exception,
            null);
          throw new InternalServerErrorRuntimeException("Invalid uri for acsp-profile-data-api service");
      } catch ( Exception exception ) {
        LOG.errorContext(xRequestId, String.format("Failed to fetch profile for Acsp %s", acspNumber), exception,
          null);
        throw new InternalServerErrorRuntimeException("Error from acsp-profile-data-api service");
      } finally {
          LOG.infoContext(getXRequestId(),
            "Finished request to acsp-profile-data-api: GET /authorised-corporate-service-providers/{acsp_number}.", null);
      }
    }

}
