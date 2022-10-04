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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.trainee.details.dto.PlacementDto;
import uk.nhs.hee.trainee.details.mapper.PlacementMapper;
import uk.nhs.hee.trainee.details.model.Placement;
import uk.nhs.hee.trainee.details.service.PlacementService;

@Slf4j
@RestController
@RequestMapping("/api/placementcredential")
public class PlacementCredential {

  private final PlacementService service;
  private final PlacementMapper mapper;

  public PlacementCredential(PlacementService service, PlacementMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  /**
   * Get the PAR request URI. We assume the trainee has been verified. This would be called by the
   * front-end when the user clicks on the 'Add placement to wallet' button.
   *
   * @param placementTisId The ID of the placement.
   * @param dto          The placement details to issue as a credential.
   * @return The Request URI, or error code.
   */
  @GetMapping("/par/{placementTisId}")
  public String getCredentialParUri(
      @PathVariable(name = "placementTisId") String placementTisId,
      @RequestBody @Validated PlacementDto dto) {
    log.info("Get placement credential request URI for placement with TIS ID {}", placementTisId);
    Placement entity = mapper.toEntity(dto);

    String client_id = "TODO";
    String client_secret = "TODO";
    String redirect_uri = "/???";
    String scope = "issue.Placement"; //TODO confirm
    String nonce = "";
    String state = "";
    String id_token_hint = entity.toCredentialJwt();
    JSONObject parResponse = POST to OIDC_PAR(client_id, client_secret, redirect_uri, scope, nonce, state, id_token_hint);
    ParResponse response = decode parResponse;

    return response.requestUri;
    //Now Front-end / Sitekit makes Authorize request with requestUri - QR code shown to user for approval, etc.

  }
  

  /**
   * Get the ID token for an authorized / issued placement credential.
   *
   * @param placementTisId The ID of the placement.
   * @param code         The auth code
   * @param state        state
   * @return The token for the credential
   */
  @GetMapping("/token/{placementTisId}")
  public String getCredentialIdToken(
      @PathVariable(name = "placementTisId") String placementTisId,
      @RequestParam String code,
      @RequestParam String state) {
    log.info("Get token for issued credential for placement ID {}", placementTisId);

    //check state has not changed

    String grant_type = "authorization_code";
    String client_id = "TODO";
    String client_secret = "TODO";
    String redirect_uri = "/???";
    JSONObject id_token = POST ISSUING_TOKEN(grant_type, client_id, client_secret, redirect_uri, code);
    //I don't follow how the redirect to /??? should work here. I'm presuming the actual
    //call to /issuing/authorize needs to happen from the back-end, since it contains the client_secret,
    //but how does the redirect work then?

    return id_token.toString();
  }

}
