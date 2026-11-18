package be.leuven.leuvengo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LeuvenGoApplication {
    public static void main(String[] args) {
        SpringApplication.run(LeuvenGoApplication.class, args);
    }
}
