package be.leuven.leuvengo.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/** Binds the real application.yml + application-prod.yml, as a deploy would. */
class ProdProfileTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(PropertiesOnly.class)
            .withPropertyValues("spring.profiles.active=prod");

    @Test
    void prodRefusesToStartWithoutAllowedOrigins() {
        runner.run(ctx -> assertThat(ctx).hasFailed()
                .getFailure().hasStackTraceContaining("ALLOWED_ORIGINS"));
    }

    @Test
    void prodUsesTheConfiguredOrigins() {
        runner.withPropertyValues("ALLOWED_ORIGINS=https://leuvengo.example")
                .run(ctx -> assertThat(ctx.getBean(LeuvenGoProperties.class).cors().allowedOrigins())
                        .containsExactly("https://leuvengo.example"));
    }

    @Configuration
    @EnableConfigurationProperties(LeuvenGoProperties.class)
    static class PropertiesOnly {}
}
