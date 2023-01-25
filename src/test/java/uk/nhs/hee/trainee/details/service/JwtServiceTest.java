/*
 * The MIT License (MIT)
 *
 * Copyright 2022 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
import uk.nhs.hee.trainee.details.config.DspConfigurationProperties;
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

  @BeforeEach
  void setup() {
    DspConfigurationProperties properties = new DspConfigurationProperties();
    properties.setTokenAudience(TOKEN_AUDIENCE);
    properties.setTokenIssuer(TOKEN_ISSUER);
    properties.setTokenSigningKey(TOKEN_SIGNING_KEY);

    service = new JwtService(properties);
    placement = new Placement();
    placement.setTisId("test");
    token = service.generatePlacementToken(placement);
    String[] chunks = token.split("\\.");
    Base64.Decoder decoder = Base64.getUrlDecoder();

    header = new String(decoder.decode(chunks[0]));
    payload = new String(decoder.decode(chunks[1]));
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
        payloadNode.get("nbf").asLong(0) > 0, is(true));
    assertThat("Payload exp is incorrect",
        payloadNode.get("exp").asLong(0) > 0, is(true));
    assertThat("Payload iat is incorrect",
        payloadNode.get("iat").asLong(0) > 0, is(true));
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
    assertThat("Token could not be verified", service.canVerifyToken(token), is(true));
  }

  @Test
  void canGetTokenPayload() throws JsonProcessingException {
    String payload = service.getTokenPayload(token, false);
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
