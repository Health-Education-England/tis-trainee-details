package com.transformuk.hee.tis.tcs.service;

import uk.nhs.trainee.details.config.ApplicationProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = {"uk.nhs.trainee.details"})
@EnableAutoConfiguration()
@EnableConfigurationProperties({ApplicationProperties.class})
public class TestConfig {

}
