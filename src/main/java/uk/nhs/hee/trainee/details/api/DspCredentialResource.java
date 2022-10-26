/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
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

package uk.nhs.hee.trainee.details.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.SignatureException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import uk.nhs.hee.trainee.details.model.Placement;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.dsp.IssueTokenResponse;
import uk.nhs.hee.trainee.details.model.dsp.ParResponse;
import uk.nhs.hee.trainee.details.service.JwtService;
import uk.nhs.hee.trainee.details.service.PlacementService;
import uk.nhs.hee.trainee.details.service.ProgrammeMembershipService;

@Slf4j
@RestController
@RequestMapping("/api/credential")
public class DspCredentialResource {

  private static final Integer CONNECT_TIMEOUT = 2000;
  private static final Integer READ_TIMEOUT = 5000;
  private static final String STATE = "someString"; //TODO consider this
  private static final String TIS_ID_ATTRIBUTE = "custom:tisId";

  private final ObjectMapper objectMapper;
  private final PlacementService placementService;
  private final ProgrammeMembershipService programmeMembershipService;
  private final String clientId;
  private final String clientSecret;
  private final String redirectUri;
  private final String parEndpoint;
  private final String issueEndpoint;
  private final String tokenEndpoint;
  private final RestTemplateBuilder restTemplateBuilder;
  private final JwtService jwtService;


  public DspCredentialResource(PlacementService placementService,
                               ProgrammeMembershipService programmeMembershipService,
                               ObjectMapper objectMapper,
                               @Value("${dsp.client-id}") String clientId,
                               @Value("${dsp.client-secret}") String clientSecret,
                               @Value("${dsp.redirect-uri}") String redirectUri,
                               @Value("${dsp.par-endpoint}") String parEndpoint,
                               @Value("${dsp.issue-endpoint}") String issueEndpoint,
                               @Value("${dsp.token.issue-endpoint}") String tokenEndpoint,
                               RestTemplateBuilder restTemplateBuilder,
                               JwtService jwtService) {
    this.placementService = placementService;
    this.programmeMembershipService = programmeMembershipService;
    this.objectMapper = objectMapper;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.redirectUri = redirectUri;
    this.parEndpoint = parEndpoint;
    this.issueEndpoint = issueEndpoint;
    this.tokenEndpoint = tokenEndpoint;
    this.restTemplateBuilder = restTemplateBuilder;
    this.jwtService = jwtService;
  }

  /**
   * Get the PAR response, including the request URI. We assume the trainee has been verified. This
   * would be called by the front-end when the user clicks on the 'Add to wallet' button for a
   * placement or programme membership.
   * <p>
   * TODO: pass the content from the front-end (as a JWT so that we can verify that it has not
   * been tampered with).
   *
   * @param credentialType 'placement' or 'programmemembership'
   * @param tisId The ID of the placement / programme membership.
   * @return The PAR response, or server 500 error if the request times out
   */
  @GetMapping(value = "/par/{credentialType:placement|programmemembership}/{tisId}")
  public ResponseEntity<ParResponse> getCredentialParUri(
      @PathVariable(name = "credentialType") String credentialType,
      @PathVariable(name = "tisId") String tisId,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
    String traineeTisId;
    //TODO sanitise to prevent log injection
    log.info("Get credential request URI for {} with TIS ID {}", credentialType, tisId);

    String[] tokenSections = token.split("\\.");
    byte[] payloadBytes = Base64.getUrlDecoder()
        .decode(tokenSections[1].getBytes(StandardCharsets.UTF_8));
    //TODO: should we verify the token signature hash? We'd need the Cognito JWK for the userpool I think.
    // But the endpoint is not publicly accessible so maybe this is ok.
    try {
      Map<?, ?> payload = objectMapper.readValue(payloadBytes, Map.class);
      traineeTisId = (String) payload.get(TIS_ID_ATTRIBUTE);
    } catch (IOException e) {
      log.warn("Unable to read trainee tisId from token.", e);
      return ResponseEntity.badRequest().build();
    }

    String idTokenHint = "";
    String scope = "";

    if (credentialType.equalsIgnoreCase("placement")) {
      Optional<Placement> placement = placementService.getPlacementForTrainee(traineeTisId, tisId);
      if (placement.isEmpty()) {
        log.warn("Unable to find placement with ID {} for trainee with ID {}",
            tisId, traineeTisId);
        return ResponseEntity.unprocessableEntity().build();
      }
      idTokenHint = jwtService.generatePlacementToken(placement.get());
      scope = "issue.TestCredential"; //TODO set up issue.Placement

    } else if (credentialType.equalsIgnoreCase("programmemembership")) {
      Optional<ProgrammeMembership> programmeMembership
          = programmeMembershipService.getProgrammeMembershipForTrainee(traineeTisId, tisId);
      if (programmeMembership.isEmpty()) {
        log.warn("Unable to find programme membership with ID {} for trainee with ID {}",
            tisId, traineeTisId);
        return ResponseEntity.unprocessableEntity().build();
      }
      idTokenHint = jwtService.generateProgrammeMembershipToken(programmeMembership.get());
      scope = "issue.TestCredential"; //TODO set up issue.ProgrammeMembership
    }

    RestTemplate restTemplate = this.restTemplateBuilder
        .setConnectTimeout(Duration.ofMillis(CONNECT_TIMEOUT))
        .setReadTimeout(Duration.ofMillis(READ_TIMEOUT))
        .build();

    URI parUri = URI.create(this.parEndpoint);
    String nonce = UUID.randomUUID().toString();
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> bodyPair = new LinkedMultiValueMap<>();
    bodyPair.add("client_id", this.clientId);
    bodyPair.add("client_secret", this.clientSecret);
    bodyPair.add("redirect_uri", this.redirectUri);
    bodyPair.add("scope", scope);
    bodyPair.add("id_token_hint", idTokenHint);
    bodyPair.add("nonce", nonce);
    bodyPair.add("state", STATE);

    HttpEntity<MultiValueMap<String, String>> parRequest = new HttpEntity<>(bodyPair, headers);

    ResponseEntity<ParResponse> parResponse = restTemplate.postForEntity(parUri, parRequest,
        ParResponse.class);
    if (parResponse.getStatusCode() == HttpStatus.CREATED && parResponse.getBody() != null) {
      String location = String.format("%s?client_id=%s&request_uri=%s", issueEndpoint, clientId,
          parResponse.getBody().getRequestUri());
      return ResponseEntity.created(URI.create(location)).build();
    } else {
      return ResponseEntity.internalServerError().build();
    }

    //Now Front-end makes Authorize request with request_uri - QR code shown to user for approval, etc.
    //the 'authorize' response then redirects to e.g. /profile?code=xxx&state=yyy
    //That page would in turn need to call the API below to get the actual details of the token,
    //if desired
  }

  /**
   * Get the credential content from an issued credential.
   *
   * @param code  The auth code
   * @param state The state used in the original PAR call
   * @return The credential contents JSON
   * @throws SignatureException if the JWT token signature is invalid
   */
  @GetMapping("/payload")
  public ResponseEntity<String> getCredentialPayload(@RequestParam String code,
      @RequestParam String state)
      throws SignatureException {
    log.info("Get details for issued credential with code {}", code);

    if (!STATE.equals(state)) {
      log.warn("State does not match that of original PAR request");
      return ResponseEntity.badRequest().build();
    }

    RestTemplate restTemplate = this.restTemplateBuilder
        .setConnectTimeout(Duration.ofMillis(CONNECT_TIMEOUT))
        .setReadTimeout(Duration.ofMillis(READ_TIMEOUT))
        .build();

    URI tokenUri = URI.create(this.tokenEndpoint);

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> bodyPair = new LinkedMultiValueMap<>();
    bodyPair.add("grant_type", "authorization_code");
    bodyPair.add("client_id", this.clientId);
    bodyPair.add("client_secret", this.clientSecret);
    bodyPair.add("redirect_uri", this.redirectUri);
    bodyPair.add("code", code);

    HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(bodyPair, headers);

    ResponseEntity<IssueTokenResponse> tokenResponse
        = restTemplate.postForEntity(tokenUri, tokenRequest, IssueTokenResponse.class);

    if (tokenResponse.getStatusCode() == HttpStatus.OK && tokenResponse.getBody() != null) {
      IssueTokenResponse token = tokenResponse.getBody();
      return ResponseEntity.ok(jwtService.getTokenPayload(token.getIdToken()));
    } else {
      return ResponseEntity.internalServerError().build();
    }
  }
}
