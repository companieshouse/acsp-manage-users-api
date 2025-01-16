package uk.gov.companieshouse.acsp.manage.users.service;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getXRequestId;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.acsp.manage.users.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.rest.AcspProfileEndpoint;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acspprofile.AcspProfile;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class AcspProfileService {

  private final AcspProfileEndpoint acspProfileEndpoint;

  private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

  public AcspProfileService( final AcspProfileEndpoint acspProfileEndpoint ) {
    this.acspProfileEndpoint = acspProfileEndpoint;
  }

  public AcspProfile fetchAcspProfile( final String acspNumber ) {

    final var xRequestId = getXRequestId();

    try {
      LOG.infoContext(xRequestId, String.format(
        "Sending request to acsp-profile-data-api: GET /authorised-corporate-service-providers/{acsp_number}. Attempting to retrieve acsp: %s",
        acspNumber), null);
      return acspProfileEndpoint.getAcspInfo(acspNumber).getData();
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
      LOG.errorContext(xRequestId, String.format("Failed to retrieve profile for Acsp %s", acspNumber), exception, null);
      throw new InternalServerErrorRuntimeException("Failed to retrieve Acsp Profile");
    }
  }


  public Map<String, AcspProfile> fetchAcspProfiles( final Stream<AcspMembersDao> acspMembers ) {

    return acspMembers.map(AcspMembersDao::getAcspNumber)
      .distinct()
      .parallel()
      .map(this::fetchAcspProfile)
      .collect(Collectors.toMap(AcspProfile::getNumber, acspProfile -> acspProfile));

  }

}
