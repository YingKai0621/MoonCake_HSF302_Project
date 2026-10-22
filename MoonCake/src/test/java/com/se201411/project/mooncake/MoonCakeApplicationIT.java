package com.se201411.project.mooncake;

import com.se201411.project.mooncake.entity.VolumeDiscountTier;
import com.se201411.project.mooncake.enums.DiscountGroup;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
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
        List<VolumeDiscountTier> activeGiftBoxTiers = activeTiers(DiscountGroup.GIFT_BOX);
        List<VolumeDiscountTier> activeMooncakeTiers = activeTiers(DiscountGroup.MOONCAKE);

        assertThat(databaseName).isEqualTo(EXPECTED_DATABASE);
        assertThat(tierCount).isEqualTo(10L);
        assertThat(activeGiftBoxTiers)
            .extracting(VolumeDiscountTier::getMinQuantity)
            .containsExactly(5, 20, 50, 100, 200, 500);
        assertThat(activeGiftBoxTiers)
            .extracting(VolumeDiscountTier::getMaxQuantity)
            .containsExactly(19, 49, 99, 199, 499, null);
        assertThat(activeGiftBoxTiers)
            .extracting(tier -> tier.getDiscountPercent().intValueExact())
            .containsExactly(5, 8, 10, 12, 15, 18);
        assertThat(activeMooncakeTiers)
            .extracting(VolumeDiscountTier::getMinQuantity)
            .containsExactly(50, 100);
        assertThat(activeMooncakeTiers)
            .extracting(tier -> tier.getDiscountPercent().intValueExact())
            .containsExactly(5, 10);
    }

    private List<VolumeDiscountTier> activeTiers(DiscountGroup group) {
        return entityManager.createQuery("""
            select tier
            from VolumeDiscountTier tier
            where tier.active = true
              and tier.discountGroup = :group
            order by tier.minQuantity
            """, VolumeDiscountTier.class)
            .setParameter("group", group)
            .getResultList();
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
