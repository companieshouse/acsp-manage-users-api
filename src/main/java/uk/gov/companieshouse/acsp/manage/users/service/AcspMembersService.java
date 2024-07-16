package uk.gov.companieshouse.acsp.manage.users.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.acsp.manage.users.repositories.AcspMembersRepository;

@Service
public class AcspMembersService {

  private final AcspMembersRepository acspMembersRepository;

  public AcspMembersService( final AcspMembersRepository acspMembersRepository ) {
      this.acspMembersRepository = acspMembersRepository;
  }

}
