package be.leuven.leuvengo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Background jobs; tests switch them off with {@code leuvengo.scheduling.enabled=false}. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "leuvengo.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {}
