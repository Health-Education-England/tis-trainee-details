package uk.nhs.hee.trainee.details.service;

import static io.jsonwebtoken.SignatureAlgorithm.HS256;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.trainee.details.model.Placement;

class JwtServiceTest {
  private static final String TOKEN_ISSUER = "issuer";
  private static final String TOKEN_AUDIENCE = "audience";
  private static final String TOKEN_SIGNING_KEY = "signing key";
  private static final SignatureAlgorithm SA = HS256;
  private JwtService service;
  private Placement placement;
  private String token;
  private String header;
  private String payload;
  private String signature;

  @BeforeEach
  void setup() {
    service = new JwtService(TOKEN_ISSUER, TOKEN_AUDIENCE, TOKEN_SIGNING_KEY);
    placement = new Placement();
    placement.setTisId("test");
    token = service.generatePlacementToken(placement);
    String[] chunks = token.split("\\.");
    Base64.Decoder decoder = Base64.getUrlDecoder();

    header = new String(decoder.decode(chunks[0]));
    payload = new String(decoder.decode(chunks[1]));
    signature = new String(decoder.decode(chunks[2]));
  }

  @Test
  void tokenIsStructuredCorrectly() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();

    JsonNode headerNode = mapper.readTree(header);
    assertThat("Header alg is incorrect",
        headerNode.get("alg").asText("missing"), is(SA.getValue()));

    JsonNode payloadNode = mapper.readTree(payload);
    assertThat("Payload aud is incorrect",
        payloadNode.get("aud").asText("missing"), is(TOKEN_AUDIENCE));
    assertThat("Payload iss is incorrect",
        payloadNode.get("iss").asText("missing"), is(TOKEN_ISSUER));
    //TODO: could check that nbf and iat are in the recent past, and exp is in the future?
    assertThat("Payload nbf is incorrect",
        payloadNode.get("nbf").asLong(0)>0, is(true));
    assertThat("Payload exp is incorrect",
        payloadNode.get("exp").asLong(0)>0, is(true));
    assertThat("Payload iat is incorrect",
        payloadNode.get("iat").asLong(0)>0, is(true));
  }
  @Test
  void generatePlacementTokenHasCorrectDetails() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();

    JsonNode payloadNode = mapper.readTree(payload);
    //TODO rework when real data in payload:
    assertThat("Payload givenName is incorrect",
        payloadNode.get("givenName").asText("missing"), is("Joe"));
    assertThat("Payload familyName is incorrect",
        payloadNode.get("familyName").asText("missing"), is("Bloggs"));
    assertThat("Payload birthDate is incorrect",
        payloadNode.get("birthDate").asText("missing"), is("1980-08-21"));
  }

  @Test
  void generateProgrammeMembershipTokenHasCorrectDetails() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();

    JsonNode payloadNode = mapper.readTree(payload);
    //TODO rework when real data in payload:
    assertThat("Payload givenName is incorrect",
        payloadNode.get("givenName").asText("missing"), is("Joe"));
    assertThat("Payload familyName is incorrect",
        payloadNode.get("familyName").asText("missing"), is("Bloggs"));
    assertThat("Payload birthDate is incorrect",
        payloadNode.get("birthDate").asText("missing"), is("1980-08-21"));
  }

  @Test
  void canVerifyValidToken() {
    assertThat("Token could not be verified", service.canVerifyToken(token), is (true));
  }

  @Test
  void canGetTokenPayload() throws JsonProcessingException {
    String payload = service.getTokenPayload(token);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode payloadNode = mapper.readTree(payload);
    //TODO rework when real data in payload:
    assertThat("Payload givenName is incorrect",
        payloadNode.get("givenName").asText("missing"), is("Joe"));
    assertThat("Payload familyName is incorrect",
        payloadNode.get("familyName").asText("missing"), is("Bloggs"));
    assertThat("Payload birthDate is incorrect",
        payloadNode.get("birthDate").asText("missing"), is("1980-08-21"));
  }
}
