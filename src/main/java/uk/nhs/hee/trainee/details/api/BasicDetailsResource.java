/*
 * The MIT License (MIT)
 *
 * Copyright 2020 Crown Copyright (Health Education England)
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

import com.amazonaws.xray.spring.aop.XRayEnabled;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.trainee.details.api.util.AuthTokenUtil;
import uk.nhs.hee.trainee.details.dto.GmcDetailsDto;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsDto;
import uk.nhs.hee.trainee.details.dto.validation.UserUpdate;
import uk.nhs.hee.trainee.details.mapper.PersonalDetailsMapper;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.service.PersonalDetailsService;

@Slf4j
@RestController
@RequestMapping("/api/basic-details")
@XRayEnabled
public class BasicDetailsResource {

  private final PersonalDetailsService service;
  private final PersonalDetailsMapper mapper;

  public BasicDetailsResource(PersonalDetailsService service, PersonalDetailsMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  /**
   * Update the person details for the trainee, creates the parent profile if it does not already
   * exist.
   *
   * @param tisId The ID of the trainee to update.
   * @param dto   The person details to update with.
   * @return The updated or created PersonalDetails.
   */
  @PatchMapping("/{tisId}")
  public ResponseEntity<PersonalDetailsDto> updateBasicDetails(
      @PathVariable(name = "tisId") String tisId, @RequestBody PersonalDetailsDto dto) {
    log.info("Update basic details of trainee with TIS ID {}", tisId);
    PersonalDetails entity = mapper.toEntity(dto);
    entity = service.createProfileOrUpdateBasicDetailsByTisId(tisId, entity);
    return ResponseEntity.ok(mapper.toDto(entity));
  }

  /**
   * Update the GMC number for the authenticated trainee.
   *
   * @param token      The authentication token.
   * @param gmcDetails The new GMC details.
   * @return The updated PersonalDetails.
   */
  @PutMapping("/gmc-number")
  public ResponseEntity<PersonalDetailsDto> updateGmcNumber(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
      @Validated(UserUpdate.class) @RequestBody GmcDetailsDto gmcDetails) {
    String tisId;
    try {
      tisId = AuthTokenUtil.getTraineeTisId(token);
    } catch (IOException e) {
      log.warn("Unable to read tisId from token.", e);
      return ResponseEntity.badRequest().build();
    }

    log.info("Updating GMC number of trainee {}.", tisId);

    // Default all GMCs to registered until we can properly prompt/determine the correct status.
    GmcDetailsDto updatedGmcDetails = GmcDetailsDto.builder()
        .gmcNumber(gmcDetails.gmcNumber())
        .gmcStatus("Registered with Licence")
        .build();

    Optional<PersonalDetails> entity = service.updateGmcDetailsWithTraineeProvidedDetails(tisId,
        updatedGmcDetails);
    Optional<PersonalDetailsDto> dto = entity.map(mapper::toDto);
    return ResponseEntity.of(dto);
  }
}
