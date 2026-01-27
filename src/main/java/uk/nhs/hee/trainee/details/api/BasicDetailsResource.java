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
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.trainee.details.dto.ContactDetailsUpdateDto;
import uk.nhs.hee.trainee.details.dto.GmcDetailsDto;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsDto;
import uk.nhs.hee.trainee.details.dto.TraineeIdentity;
import uk.nhs.hee.trainee.details.dto.validation.UserUpdate;
import uk.nhs.hee.trainee.details.exception.EmailAlreadyInUseException;
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
  private final TraineeIdentity traineeIdentity;

  public BasicDetailsResource(PersonalDetailsService service, PersonalDetailsMapper mapper,
      TraineeIdentity traineeIdentity) {
    this.service = service;
    this.mapper = mapper;
    this.traineeIdentity = traineeIdentity;
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
   * @param gmcDetails The new GMC details.
   * @return The updated PersonalDetails.
   */
  @PutMapping("/gmc-number")
  public ResponseEntity<PersonalDetailsDto> updateGmcNumber(
      @Validated(UserUpdate.class) @RequestBody GmcDetailsDto gmcDetails) {
    String tisId = traineeIdentity.getTraineeId();

    if (tisId == null) {
      log.warn("No trainee ID provided.");
      return ResponseEntity.badRequest().build();
    }

    log.info("Updating GMC number of trainee {}.", tisId);

    GmcDetailsDto updatedGmcDetails = GmcDetailsDto.builder()
        .gmcNumber(gmcDetails.gmcNumber())
        .build();

    Optional<PersonalDetails> entity = service.updateGmcDetailsWithTraineeProvidedDetails(tisId,
        updatedGmcDetails);
    Optional<PersonalDetailsDto> dto = entity.map(mapper::toDto);
    return ResponseEntity.of(dto);
  }

  /**
   * Submit an update to the email address for the authenticated trainee to TIS.
   *
   * @param contactDetailsUpdateDto The new email address.
   * @return 200  if the email update request could be made, 404 if the trainee could not be found,
   *         400 otherwise.
   */
  @PutMapping("/email-address")
  public ResponseEntity<Void> updateEmailAddress(
      @Validated(UserUpdate.class) @RequestBody ContactDetailsUpdateDto contactDetailsUpdateDto) {
    String tisId = traineeIdentity.getTraineeId();

    if (tisId == null) {
      log.warn("No trainee ID provided.");
      return ResponseEntity.badRequest().build();
    }
    if (!service.isEmailChangeUnique(tisId, contactDetailsUpdateDto.getEmail())) {
      throw new EmailAlreadyInUseException("Email address is already in use.");
    }
    log.info("Submitting email address update request for trainee {} to {}.", tisId,
        contactDetailsUpdateDto.getEmail());
    boolean requested
        = service.requestUpdateEmailWithTraineeProvidedDetails(tisId, contactDetailsUpdateDto);
    if (!requested) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.noContent().build();
  }
}
