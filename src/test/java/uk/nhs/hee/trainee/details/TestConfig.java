package uk.nhs.hee.trainee.details;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import uk.nhs.hee.trainee.details.config.ApplicationProperties;

@ComponentScan(basePackages = {"uk.nhs.trainee.details"})
@EnableAutoConfiguration()
@EnableConfigurationProperties({ApplicationProperties.class})
public class TestConfig {

}
