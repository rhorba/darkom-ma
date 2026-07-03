package com.darkom.common.config;

import com.darkom.common.security.JwtAuthenticationFilter;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless JWT bearer auth (ADR-3, docs/architecture-darkom.md). /api/v1/auth/** issues and
 * refreshes tokens and is public; everything else requires a valid access token. CORS allows only
 * the Angular dev origin (app.cors.allowed-origin) with credentials, since the refresh token
 * travels as an HttpOnly cookie.
 */
@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain filterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtFilter,
      @Qualifier("corsConfigurationSource") CorsConfigurationSource corsSource)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsSource))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health", "/api/v1/auth/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/properties")
                    .hasRole("LANDLORD")
                    .requestMatchers(
                        "/api/v1/properties", "/api/v1/properties/**", "/api/v1/units/**")
                    .hasAnyRole("LANDLORD", "PROPERTY_MANAGER")
                    .requestMatchers(HttpMethod.POST, "/api/v1/leases")
                    .hasAnyRole("LANDLORD", "PROPERTY_MANAGER")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(corsProperties.getAllowedOrigin()));
    configuration.setAllowedMethods(
        List.of(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name()));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
