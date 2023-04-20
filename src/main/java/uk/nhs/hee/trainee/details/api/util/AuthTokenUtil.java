package uk.nhs.hee.trainee.details.api.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuthTokenUtil {

  private static final String TIS_ID_ATTRIBUTE = "custom:tisId";

  private static final ObjectMapper mapper = new ObjectMapper();

  private AuthTokenUtil() {
  }

  /**
   * Get the trainee's TIS ID from the provided token.
   *
   * @param token The token to use.
   * @return The trainee's TIS ID.
   * @throws IOException If the token's payload was not a Map.
   */
  public static String getTraineeTisId(String token) throws IOException {
    String[] tokenSections = token.split("\\.");
    byte[] payloadBytes = Base64.getUrlDecoder()
        .decode(tokenSections[1].getBytes(StandardCharsets.UTF_8));

    Map<?, ?> payload = mapper.readValue(payloadBytes, Map.class);
    return (String) payload.get(TIS_ID_ATTRIBUTE);
  }
}
