package ocop.vn.ocop_tourims_platform.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import ocop.vn.ocop_tourims_platform.common.ApiResponse;
import ocop.vn.ocop_tourims_platform.common.ErrorCode;
import ocop.vn.ocop_tourims_platform.service.JWTService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, JWTService jwtService, ObjectMapper objectMapper) throws Exception {
    JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
    authorities.setAuthoritiesClaimName("roles");
    authorities.setAuthorityPrefix("ROLE_");
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authorities);

    AuthenticationEntryPoint entryPoint =
        (request, response, exception) ->
            writeError(
                objectMapper,
                request,
                response,
                exception instanceof InvalidBearerTokenException
                    ? ErrorCode.INVALID_TOKEN
                    : ErrorCode.UNAUTHENTICATED);
    AccessDeniedHandler deniedHandler =
        (request, response, exception) ->
            writeError(objectMapper, request, response, ErrorCode.ACCESS_DENIED);

    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/auths/register",
                        "/api/v1/auths/login",
                        "/api/v1/auths/refresh",
                        "/api/v1/pois/directions")
                    .permitAll()
                    .requestMatchers("/api/v1/management/**")
                    .hasAnyRole("ADMIN", "PARTNER")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/home/filters",
                        "/api/v1/pois",
                        "/api/v1/pois/**",
                        "/api/v1/search/suggestions")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            errors ->
                errors.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
        .oauth2ResourceServer(
            resource ->
                resource
                    .authenticationEntryPoint(entryPoint)
                    .accessDeniedHandler(deniedHandler)
                    .jwt(
                        jwt ->
                            jwt.decoder(jwtService::verifyAccessToken)
                                .jwtAuthenticationConverter(converter)))
        .build();
  }

  private void writeError(
      ObjectMapper mapper,
      HttpServletRequest request,
      HttpServletResponse response,
      ErrorCode error)
      throws IOException {
    response.setStatus(error.getStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader("Cache-Control", "no-store");
    ApiResponse<Void> body =
        ApiResponse.error(
            error.getStatus(), error.getMessage(), error.name(), null, request.getRequestURI());
    response.getWriter().write(mapper.writeValueAsString(body));
  }
}
