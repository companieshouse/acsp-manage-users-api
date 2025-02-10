package uk.gov.companieshouse.acsp.manage.users.configuration;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;

import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import uk.gov.companieshouse.acsp.manage.users.filter.RoleAssignmentFilter;
import uk.gov.companieshouse.acsp.manage.users.service.AcspMembersService;
import uk.gov.companieshouse.acsp.manage.users.service.UsersService;
import uk.gov.companieshouse.api.filter.CustomCorsFilter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Autowired
    private UsersService usersService;

    @Autowired
    private AcspMembersService acspMembersService;

    private static final Supplier<List<String>> externalMethods = () -> List.of( GET.name() );
    private static final String ACSP_OWNER_ROLE = "ACSP_OWNER";
    private static final String ACSP_ADMIN_ROLE = "ACSP_ADMIN";
    private static final String ACSP_STANDARD_ROLE = "ACSP_STANDARD";
    private static final String KEY_ROLE = "KEY";
    private static final String ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE = "ADMIN_WITH_ACSP_SEARCH_PRIVILEGE";

    @Bean
    public SecurityFilterChain filterChain( final HttpSecurity http ) throws Exception {
        http.cors( AbstractHttpConfigurer::disable )
                .sessionManagement( s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS ) )
                .csrf( AbstractHttpConfigurer::disable )
                .addFilterBefore( new CustomCorsFilter( externalMethods.get() ), CsrfFilter.class )
                .addFilterAfter( new RoleAssignmentFilter( usersService, acspMembersService ), CsrfFilter.class )
                .authorizeHttpRequests( request -> request
                        .requestMatchers( GET, "/acsp-manage-users-api/healthcheck" ).permitAll()
                        .requestMatchers( GET, "/user/acsps/memberships" ).hasAnyRole( ACSP_OWNER_ROLE, ACSP_ADMIN_ROLE, ACSP_STANDARD_ROLE )
                        .requestMatchers( POST, "/acsps/*/memberships" ).hasAnyRole( ACSP_OWNER_ROLE, ACSP_ADMIN_ROLE, ACSP_STANDARD_ROLE, KEY_ROLE )
                        .requestMatchers( PATCH, "/acsps/memberships/*" ).hasAnyRole( ACSP_OWNER_ROLE, ACSP_ADMIN_ROLE, ACSP_STANDARD_ROLE, KEY_ROLE )
                        .requestMatchers( GET, "/acsps/memberships/*" ).hasAnyRole( ACSP_OWNER_ROLE, ACSP_ADMIN_ROLE, ACSP_STANDARD_ROLE, KEY_ROLE )
                        .requestMatchers( GET, "/acsps/*/memberships" ).hasAnyRole( ACSP_OWNER_ROLE, ACSP_ADMIN_ROLE, ACSP_STANDARD_ROLE, KEY_ROLE, ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE )
                        .requestMatchers( POST, "/acsps/*/memberships/lookup" ).hasAnyRole( ACSP_OWNER_ROLE, ACSP_ADMIN_ROLE, ACSP_STANDARD_ROLE, KEY_ROLE, ADMIN_WITH_ACSP_SEARCH_PRIVILEGE_ROLE )
                        .anyRequest().denyAll()
                );
        return http.build();
    }

}