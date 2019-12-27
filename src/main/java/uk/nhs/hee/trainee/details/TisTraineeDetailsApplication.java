package uk.nhs.hee.trainee.details;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class TisTraineeDetailsApplication {
  private static final Logger log = LoggerFactory.getLogger(TisTraineeDetailsApplication.class);
	public static void main(String[] args) {
		log.info("INSIDE TisTraineeDetailsApplication main() METHOD");
		SpringApplication.run(TisTraineeDetailsApplication.class, args);
	}

}
