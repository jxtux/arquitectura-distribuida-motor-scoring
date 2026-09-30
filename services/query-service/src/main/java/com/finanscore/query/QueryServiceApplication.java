package com.finanscore.query;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages="com.finanscore.query")
@EntityScan(basePackages="com.finanscore.query")
@EnableJpaRepositories(basePackages="com.finanscore.query")
public class QueryServiceApplication {
    public static void main(String[] args) { SpringApplication.run(QueryServiceApplication.class, args); }
}
