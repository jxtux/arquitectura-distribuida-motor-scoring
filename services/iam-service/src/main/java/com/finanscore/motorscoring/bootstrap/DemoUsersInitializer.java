package com.finanscore.motorscoring.bootstrap;

import com.finanscore.motorscoring.application.security.model.*;
import com.finanscore.motorscoring.application.security.port.out.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import java.time.*;
import java.util.*;

@Configuration
public class DemoUsersInitializer {
    @Bean
    CommandLineRunner demoUsers(
            UserAccountRepositoryPort users,
            LocalCredentialRepositoryPort credentials,
            AuthorizationRepositoryPort auth,
            PasswordHasherPort hasher,
            @Value("${iam.demo-password}") String demoPassword,
            @Value("${iam.admin-email:admin@finanscore.local}") String adminEmail,
            @Value("${iam.admin-password}") String adminPassword,
            @Value("${iam.admin-display-name:Administrador Demo}") String adminDisplayName) {
        return args -> {
            var data=List.of(
                new String[]{"Usuario Demo Uno","demo1@finanscore.local"},
                new String[]{"Usuario Demo Dos","demo2@finanscore.local"},
                new String[]{"Usuario Demo Tres","demo3@finanscore.local"},
                new String[]{"Usuario Demo Cuatro","demo4@finanscore.local"},
                new String[]{"Usuario Demo Cinco","demo5@finanscore.local"}
            );
            for(var d:data){
                var existing=users.findByEmail(d[1]);
                if(existing.isPresent()){auth.assignRole(existing.get().id(),"USER");continue;}
                var u=users.save(new UserAccount(null,d[0],d[1],UserStatus.MFA_SETUP_REQUIRED,true,Instant.now(),null));
                credentials.save(new LocalCredential(null,u.id(),hasher.hash(demoPassword),0,null,Instant.now()));
                auth.assignRole(u.id(),"USER");
            }

            var admin=users.findByEmail(adminEmail).orElseGet(() -> {
                var created=users.save(new UserAccount(null,adminDisplayName,adminEmail,UserStatus.MFA_SETUP_REQUIRED,true,Instant.now(),null));
                credentials.save(new LocalCredential(null,created.id(),hasher.hash(adminPassword),0,null,Instant.now()));
                return created;
            });
            auth.assignRole(admin.id(),"ADMIN");
        };
    }
}
