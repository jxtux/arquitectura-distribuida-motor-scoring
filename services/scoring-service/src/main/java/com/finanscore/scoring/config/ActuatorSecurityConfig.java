package com.finanscore.scoring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Scoring no expone API pública; solo health/metrics quedan accesibles dentro de la red de plataforma. */
@Configuration
public class ActuatorSecurityConfig {
    @Bean
    SecurityFilterChain scoringSecurity(HttpSecurity http) throws Exception {
        return http.csrf(c->c.disable())
            .authorizeHttpRequests(a->a
                .requestMatchers("/actuator/health/**","/actuator/prometheus","/actuator/info").permitAll()
                .anyRequest().denyAll())
            .build();
    }
}
