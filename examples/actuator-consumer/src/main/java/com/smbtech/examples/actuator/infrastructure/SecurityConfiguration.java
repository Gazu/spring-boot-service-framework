package com.smbtech.examples.actuator.infrastructure;

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .requestMatchers("/api/dummy")
                                        .permitAll()
                                        .requestMatchers(EndpointRequest.to("health", "info"))
                                        .permitAll()
                                        .requestMatchers(
                                                EndpointRequest.to("serviceframework", "metrics"))
                                        .hasRole("ACTUATOR")
                                        .anyRequest()
                                        .denyAll())
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
