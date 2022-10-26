package uk.nhs.hee.trainee.details.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.hee.trainee.details.model.Placement;

@SpringBootTest
@SpringBootConfiguration
class JwtServiceTest {
  private JwtService service;

  @BeforeEach
  void setupService() {
    service = new JwtService("dummy", "dummy", "dummy");
  }

  @Test
  void CanVerifyValidToken() {
    Placement placement = new Placement();
    placement.setTisId("test");
    String token = service.generatePlacementToken(placement);

    String payload = service.getTokenPayload(token);
    String x = "1";
  }
}
