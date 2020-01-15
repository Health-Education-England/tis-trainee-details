package uk.nhs.hee.trainee.details;

import java.util.Collections;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static springfox.documentation.builders.PathSelectors.regex;

@SpringBootApplication
@EnableSwagger2
public class TisTraineeDetailsApplication {
  private static final Logger log = LoggerFactory.getLogger(TisTraineeDetailsApplication.class);
	public static void main(String[] args) {
		log.info("INSIDE TisTraineeDetailsApplication main() METHOD");
		SpringApplication.run(TisTraineeDetailsApplication.class, args);
	}

  @Bean
  public Docket swaggerConfiguration(){

    return new Docket(DocumentationType.SWAGGER_2)
      .select()
      .paths(regex("/api.*"))
      .apis(RequestHandlerSelectors.basePackage("uk.nhs.hee.trainee.details"))
      .build()
      .apiInfo(apiDetails());
  }

  private ApiInfo apiDetails() {
    return new ApiInfo(
      "Trainee UI API",
      "The TIS-TRAINEE-DETAILS microservice's APIs",
      "1.0",
      "These will be consumed by React or other microservices",
      new springfox.documentation.service.Contact("Contact", "TIS Dev Team", "aaaa@hee.nhs.uk"),
      "API Licence",
      "TIS",
      Collections.emptyList());
  }
}
