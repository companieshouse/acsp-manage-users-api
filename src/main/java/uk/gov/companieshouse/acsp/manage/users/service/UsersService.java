package uk.gov.companieshouse.acsp.manage.users.service;

import static uk.gov.companieshouse.acsp.manage.users.utils.RequestContextUtil.getXRequestId;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.acsp.manage.users.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.acsp.manage.users.model.AcspMembersDao;
import uk.gov.companieshouse.acsp.manage.users.rest.AccountsUserEndpoint;
import uk.gov.companieshouse.acsp.manage.users.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class UsersService {

  private final AccountsUserEndpoint accountsUserEndpoint;

  private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

  public UsersService( AccountsUserEndpoint accountsUserEndpoint ) {
    this.accountsUserEndpoint = accountsUserEndpoint;
  }

  public User fetchUserDetails( final String userId ) {

      return accountsUserEndpoint.getUserDetails(userId);

  }

  public boolean doesUserExist( final String userId ) {
    try {
      final var user = fetchUserDetails(userId);
      return Objects.nonNull(user);
    } catch ( NotFoundRuntimeException e ) {
      LOG.debugContext(getXRequestId(), String.format("User %s does not exist", userId), null);
      return false;
    } catch ( Exception e ) {
      LOG.errorContext(getXRequestId(), new Exception(String.format("Unexpected error while checking if user %s exists", userId)), null);
      throw e;
    }
  }


  public UsersList searchUserDetails( final List<String> emails ) {
      return accountsUserEndpoint.searchUserDetails(emails);
  }

  public Map<String, User> fetchUserDetails( final Stream<AcspMembersDao> acspMembers ) {

    return acspMembers.map(AcspMembersDao::getUserId)
      .distinct()
      .map(this::fetchUserDetails)
      .collect(Collectors.toMap(User::getUserId, user -> user));

  }

}
