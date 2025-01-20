package uk.gov.companieshouse.acsp.manage.users.service;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.rest.AcspProfileEndpoint;
import uk.gov.companieshouse.api.acspprofile.AcspProfile;

@Service
public class AcspProfileService {

  private final AcspProfileEndpoint acspProfileEndpoint;

  public AcspProfileService( final AcspProfileEndpoint acspProfileEndpoint ) {
    this.acspProfileEndpoint = acspProfileEndpoint;
  }

  public AcspProfile fetchAcspProfile( final String acspNumber ) {

    return acspProfileEndpoint.getAcspInfo(acspNumber);

  }


  public Map<String, AcspProfile> fetchAcspProfiles( final Stream<AcspMembersDao> acspMembers ) {

    return acspMembers.map(AcspMembersDao::getAcspNumber)
      .distinct()
      .map(this::fetchAcspProfile)
      .collect(Collectors.toMap(AcspProfile::getNumber, acspProfile -> acspProfile));

  }

}
