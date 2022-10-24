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

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import uk.nhs.hee.trainee.details.dto.PlacementDto;
import uk.nhs.hee.trainee.details.mapper.PlacementMapper;
import uk.nhs.hee.trainee.details.model.ParResponse;
import uk.nhs.hee.trainee.details.model.Placement;
import uk.nhs.hee.trainee.details.service.JwtService;

@Slf4j
@RestController
@RequestMapping("/api/placementcredential")
public class PlacementCredential {

  private static final Integer CONNECT_TIMEOUT = 2000;
  private static final Integer READ_TIMEOUT = 5000;

  private final PlacementMapper mapper;

  private final String clientId;
  private final String clientSecret;
  private final String redirectUri;
  private final String parEndpoint;

  private final RestTemplateBuilder restTemplateBuilder;
  private final JwtService jwtService;


  public PlacementCredential(PlacementMapper mapper,
                             @Value("${dsp.client-id}") String clientId,
                             @Value("${dsp.client-secret}") String clientSecret,
                             @Value("${dsp.redirect-uri}") String redirectUri,
                             @Value("${dsp.par-endpoint}") String parEndpoint,
                             RestTemplateBuilder restTemplateBuilder,
                             JwtService jwtService) {
    this.mapper = mapper;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.redirectUri = redirectUri;
    this.parEndpoint = parEndpoint;
    this.restTemplateBuilder = restTemplateBuilder;
    this.jwtService = jwtService;
  }

  /**
   * Get the PAR response, including the request URI. We assume the trainee has been verified.
   * This would be called by the front-end when the user clicks on the 'Add placement to wallet'
   * button.
   *
   * TODO: the placement that is passed from the front-end will be a JWT not a DTO so that we can
   * verify that it has not been tampered with.
   *
   * @param placementTisId The ID of the placement.
   * @param dto          The placement details to issue as a credential.
   * @return The PAR response, or server 500 error if the request times out
   */
  @PostMapping("/par/{placementTisId}")
  public ParResponse getCredentialParUri(
      @PathVariable(name = "placementTisId") String placementTisId,
      @RequestBody @Validated PlacementDto dto) {
    log.info("Get placement credential request URI for placement with TIS ID {}", placementTisId);

    Placement entity = mapper.toEntity(dto);

    RestTemplate restTemplate = this.restTemplateBuilder
        .setConnectTimeout(Duration.ofMillis(CONNECT_TIMEOUT))
        .setReadTimeout(Duration.ofMillis(READ_TIMEOUT))
        .build();

    URI parUri = URI.create(this.parEndpoint);

    String scope = "issue.TestCredential"; //TODO set up issue.Placement
    String nonce = UUID.randomUUID().toString();
    String state = ""; //TODO
    String idTokenHint = jwtService.generatePlacementToken(entity);

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
    bodyPair.add("state", state);

    HttpEntity<MultiValueMap<String, String>> parRequest = new HttpEntity<>(bodyPair, headers);

    ResponseEntity<ParResponse> parResponse = restTemplate.postForEntity(parUri, parRequest, ParResponse.class);
    if(parResponse.getStatusCode() == HttpStatus.CREATED && parResponse.getBody() != null) {
      return parResponse.getBody();
    } else {
      return null;
    }

    //Now Front-end makes Authorize request with request_uri - QR code shown to user for approval, etc.
    //the authorize response then redirects to e.g. /profile?code=xxx&state=yyy
    //That page would in turn need to call the API below to get the actual details of the token,
    //if desired
  }

  /**
   * Get the ID token for an authorized / issued placement credential.
   *
   * @param placementTisId The ID of the placement.
   * @param code         The auth code
   * @param state        state
   * @return The token for the credential
   */
//TODO

}
