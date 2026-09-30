package com.finanscore.motorscoring.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@SpringBootApplication(scanBasePackages = {"com.finanscore.motorscoring", "com.finanscore.support"})
@EntityScan(basePackages = {"com.finanscore.motorscoring", "com.finanscore.support"})
@EnableJpaRepositories(basePackages = {"com.finanscore.motorscoring", "com.finanscore.support"})
@EnableScheduling
public class IamServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IamServiceApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
