package com.se201411.project.mooncake.security.config;

import com.se201411.project.mooncake.security.service.AdminBootstrapService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("bootstrap-admin")
public class AdminBootstrapRunner implements ApplicationRunner {
    private final AdminBootstrapService service;
    private final ConfigurableApplicationContext context;

    public AdminBootstrapRunner(AdminBootstrapService service, ConfigurableApplicationContext context) {
        this.service = service;
        this.context = context;
    }

    public void run(ApplicationArguments args) {
        service.createIfAbsent(context.getEnvironment().getProperty("BOOTSTRAP_ADMIN_EMAIL"),
                context.getEnvironment().getProperty("BOOTSTRAP_ADMIN_PASSWORD"));
        SpringApplication.exit(context, () -> 0);
    }
}
