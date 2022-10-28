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

package uk.nhs.hee.trainee.details.api;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.client.HttpClientErrorException;
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

  /**
   * Instantiate the DSP Credential Resource.
   *
   * @param placementService           the Placement service
   * @param programmeMembershipService the Programme membership service
   * @param objectMapper               the Object mapper
   * @param clientId                   the DSP client ID
   * @param clientSecret               the DSP client secret
   * @param redirectUri                the DSP redirect URI
   * @param parEndpoint                the DSP Pushed Authorization Request (PAR) endpoint
   * @param authorizeEndpoint          the DSP credential Authorize Request endpoint
   * @param tokenEndpoint              the DSP Token Request endpoint
   * @param restTemplateBuilder        the REST template builder
   * @param jwtService                 the JWT service
   */
  public DspCredentialResource(PlacementService placementService,
                               ProgrammeMembershipService programmeMembershipService,
                               ObjectMapper objectMapper,
                               @Value("${dsp.client-id}") String clientId,
                               @Value("${dsp.client-secret}") String clientSecret,
                               @Value("${dsp.redirect-uri}") String redirectUri,
                               @Value("${dsp.par-endpoint}") String parEndpoint,
                               @Value("${dsp.authorize-endpoint}") String authorizeEndpoint,
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
    this.issueEndpoint = authorizeEndpoint;
    this.tokenEndpoint = tokenEndpoint;
    this.restTemplateBuilder = restTemplateBuilder;
    this.jwtService = jwtService;
  }

  /**
   * Get the PAR response, including the request URI. We assume the trainee has been verified. This
   * would be called by the front-end when the user clicks on the 'Add to wallet' button for a
   * placement or programme membership.
   *
   * <p>NOTE: in future we will pass the content from the front-end as a JWT so that we can verify
   * that it has not been tampered with.
   *
   * @param credentialType 'placement' or 'programmemembership'
   * @param tisId The ID of the placement / programme membership.
   * @return The PAR response, or Internal Server Error if the request times out
   */
  @GetMapping(value = "/par/{credentialType:placement|programmemembership}/{tisId}")
  public ResponseEntity<ParResponse> getCredentialParUri(
      @PathVariable(name = "credentialType") String credentialType,
      @PathVariable(name = "tisId") String tisId,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
    String traineeTisId;
    log.info("Get credential request URI for {} with TIS ID {}", credentialType, tisId);

    String[] tokenSections = token.split("\\.");
    byte[] payloadBytes = Base64.getUrlDecoder()
        .decode(tokenSections[1].getBytes(StandardCharsets.UTF_8));

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

    String nonce = UUID.randomUUID().toString();
    MultiValueMap<String, String> bodyPair = new LinkedMultiValueMap<>();
    bodyPair.add("client_id", this.clientId);
    bodyPair.add("client_secret", this.clientSecret);
    bodyPair.add("redirect_uri", this.redirectUri);
    bodyPair.add("scope", scope);
    bodyPair.add("id_token_hint", idTokenHint);
    bodyPair.add("nonce", nonce);
    bodyPair.add("state", STATE);

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    HttpEntity<MultiValueMap<String, String>> parRequest = new HttpEntity<>(bodyPair, headers);
    RestTemplate restTemplate = this.restTemplateBuilder
        .setConnectTimeout(Duration.ofMillis(CONNECT_TIMEOUT))
        .setReadTimeout(Duration.ofMillis(READ_TIMEOUT))
        .build();
    URI parUri = URI.create(this.parEndpoint);

    ResponseEntity<ParResponse> parResponseEntity = restTemplate.postForEntity(parUri, parRequest,
        ParResponse.class);
    if (parResponseEntity.getStatusCode() == HttpStatus.CREATED) {
      ParResponse parResponse = parResponseEntity.getBody();
      if (parResponse != null) {
        String location = String.format("%s?client_id=%s&request_uri=%s", issueEndpoint, clientId,
            parResponse.getRequestUri());
        return ResponseEntity.created(URI.create(location)).build();
      }
    }
    return ResponseEntity.internalServerError().build();
  }

  /**
   * Get the credential content from a (recently) issued credential.
   *
   * @param code  The auth code
   * @param state The state used in the original PAR call
   * @return The credential contents JSON or Bad Request if the code or state are invalid / expired,
   *         or InternalServerError if the gateway response is invalid.
   */
  @GetMapping("/payload")
  public ResponseEntity<String> getCredentialPayload(@RequestParam String code,
      @RequestParam String state) {
    //I wonder if we need @RequestHeader(HttpHeaders.AUTHORIZATION) String token just to be safer?
    log.info("Get details for issued credential with code {}", code);

    if (!STATE.equals(state)) {
      log.info("State does not match that of original PAR request");
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

    ResponseEntity<IssueTokenResponse> tokenResponse;
    try {
      tokenResponse = restTemplate.postForEntity(tokenUri, tokenRequest, IssueTokenResponse.class);
    } catch (HttpClientErrorException e) {
      log.info("Authorization code is invalid or expired");
      return ResponseEntity.badRequest().build();
    }

    if (tokenResponse.getStatusCode() == HttpStatus.OK) {
      IssueTokenResponse token = tokenResponse.getBody();
      if (token != null && token.getIdToken() != null) {
        //we did not sign the credential token: we would need the public key to verify the signature
        return ResponseEntity.ok(jwtService.getTokenPayload(token.getIdToken(), false));
      }
    }
    return ResponseEntity.internalServerError().build();
  }
}
