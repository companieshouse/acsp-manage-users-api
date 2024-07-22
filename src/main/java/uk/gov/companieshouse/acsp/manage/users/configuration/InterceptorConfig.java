package uk.gov.companieshouse.acsp.manage.users.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.acsp.manage.users.interceptor.AuthorizationInterceptor;
import uk.gov.companieshouse.acsp.manage.users.interceptor.LoggingInterceptor;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

  private static final String ACSP_MEMBERSHIPS_BASE = "/acsps/**/memberships";
  private static final String ACSP_MEMBERSHIP_BASE = "/acsps/memberships/**";
  private static final String USER_ACSP_MEMBERSHIPS = "/user/acsps/memberships";
  private static final String HEALTH_CHECK_ENDPOINT = "/**/healthcheck";

    private final LoggingInterceptor loggingInterceptor;
    private final AuthorizationInterceptor authorizationInterceptor;

  public InterceptorConfig(
      LoggingInterceptor loggingInterceptor, AuthorizationInterceptor authorizationInterceptor) {
        this.loggingInterceptor = loggingInterceptor;
        this.authorizationInterceptor = authorizationInterceptor;
    }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
        addLoggingInterceptor(registry);
    addAuthorizationInterceptors(registry);
    }

  private void addLoggingInterceptor(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor);
    }

  private void addAuthorizationInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(authorizationInterceptor)
        .addPathPatterns(
            ACSP_MEMBERSHIPS_BASE,
            ACSP_MEMBERSHIPS_BASE + "/**",
            ACSP_MEMBERSHIP_BASE,
            USER_ACSP_MEMBERSHIPS)
        .excludePathPatterns(HEALTH_CHECK_ENDPOINT);
    }
}