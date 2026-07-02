package com.darkom.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Minimal baseline: only actuator health is public. Role-based JWT auth
 * (ADR-3, docs/architecture-darkom.md) lands in Epic 1 (Story 1.1) and will
 * replace the blanket authenticated() rule below with per-endpoint rules.
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/actuator/health").permitAll()
				.anyRequest().authenticated());
		return http.build();
	}
}
