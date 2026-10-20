package com.se201411.project.mooncake;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@ContextConfiguration(initializers = MoonCakeApplicationIT.TestDatabaseGuard.class)
class MoonCakeApplicationIT {

    private static final String EXPECTED_DATABASE = "MoonCakeTestDB";
    private static final Pattern DATABASE_NAME_PATTERN = Pattern.compile(
        "(?:^|;)databaseName=([^;]+)",
        Pattern.CASE_INSENSITIVE
    );

    @Autowired
    private EntityManager entityManager;

    @Test
    void mappingsMatchFlywaySchemaAndSeedDataIsReadable() {
        String databaseName = (String) entityManager
            .createNativeQuery("SELECT DB_NAME()", String.class)
            .getSingleResult();
        Long tierCount = entityManager
            .createQuery("select count(t) from VolumeDiscountTier t", Long.class)
            .getSingleResult();

        assertThat(databaseName).isEqualTo(EXPECTED_DATABASE);
        assertThat(tierCount).isEqualTo(4L);
    }

    static final class TestDatabaseGuard
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext context) {
            String databaseName = context.getEnvironment()
                .getProperty("TEST_DB_NAME", EXPECTED_DATABASE);
            if (!EXPECTED_DATABASE.equals(databaseName)) {
                throw new IllegalStateException(
                    "Integration tests may run only against " + EXPECTED_DATABASE
                );
            }

            String datasourceUrl = context.getEnvironment()
                .getRequiredProperty("spring.datasource.url");
            Matcher matcher = DATABASE_NAME_PATTERN.matcher(datasourceUrl);
            if (!matcher.find() || !EXPECTED_DATABASE.equals(matcher.group(1))) {
                throw new IllegalStateException(
                    "Integration datasource URL must target " + EXPECTED_DATABASE
                );
            }
        }
    }
}
