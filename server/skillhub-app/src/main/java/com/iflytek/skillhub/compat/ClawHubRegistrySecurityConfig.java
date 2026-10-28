package com.iflytek.skillhub.compat;

import com.iflytek.skillhub.auth.token.ApiTokenAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

/**
 * Declares a dedicated stateless security chain for public compatibility endpoints used by
 * registry-style clients.
 */
@Configuration
public class ClawHubRegistrySecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain publicLabelFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(
                        new OrRequestMatcher(
                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/api/v1/labels"),
                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/api/web/labels")
                        )
                )
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .requestCache(cache -> cache.disable())
                .securityContext(context -> context.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain clawHubRegistryFilterChain(
            HttpSecurity http,
            ApiTokenAuthenticationFilter apiTokenAuthenticationFilter) throws Exception {
        http
                .securityMatcher(new AndRequestMatcher(
                        new OrRequestMatcher(
                                PathPatternRequestMatcher.withDefaults().matcher("/api/v1/search"),
                                PathPatternRequestMatcher.withDefaults().matcher("/api/v1/download"),
                                PathPatternRequestMatcher.withDefaults().matcher("/api/v1/skills/*")),
                        new NegatedRequestMatcher(new OrRequestMatcher(
                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/v1/skills/package-preview"),
                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/v1/skills/avatar-uploads")))
                ))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .requestCache(cache -> cache.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(apiTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
