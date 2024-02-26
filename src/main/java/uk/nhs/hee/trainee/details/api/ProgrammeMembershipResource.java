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

import com.amazonaws.xray.spring.aop.XRayEnabled;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.nhs.hee.trainee.details.api.util.AuthTokenUtil;
import uk.nhs.hee.trainee.details.dto.ProgrammeMembershipDto;
import uk.nhs.hee.trainee.details.mapper.ProgrammeMembershipMapper;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.service.ProgrammeMembershipService;
import uk.nhs.hee.trainee.details.service.RabbitPublishService;

@Slf4j
@RestController
@RequestMapping("/api/programme-membership")
@XRayEnabled
public class ProgrammeMembershipResource {

  private final ProgrammeMembershipService service;
  private final ProgrammeMembershipMapper mapper;
  private final RabbitPublishService rabbitPublishService;

  /**
   * ProgrammeMembershipResource class constructor.
   */
  public ProgrammeMembershipResource(ProgrammeMembershipService service,
       ProgrammeMembershipMapper mapper, RabbitPublishService rabbitPublishService) {
    this.service = service;
    this.mapper = mapper;
    this.rabbitPublishService = rabbitPublishService;
  }

  /**
   * Update the programme membership for the trainee, creates the parent profile if it does not
   * already exist.
   *
   * @param traineeTisId The ID of the trainee to update.
   * @param dto          The person details to update with.
   * @return The updated or created Programme Membership.
   */
  @PatchMapping("/{traineeTisId}")
  public ResponseEntity<ProgrammeMembershipDto> updateProgrammeMembership(
      @PathVariable(name = "traineeTisId") String traineeTisId,
      @RequestBody @Validated ProgrammeMembershipDto dto) {
    log.info("Update programme membership of trainee with TIS ID {}", traineeTisId);
    ProgrammeMembership entity = mapper.toEntity(dto);
    Optional<ProgrammeMembership> optionalEntity = service
        .updateProgrammeMembershipForTrainee(traineeTisId, entity);
    entity = optionalEntity
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trainee not found."));
    return ResponseEntity.ok(mapper.toDto(entity));
  }

  /**
   * Delete the programme memberships for the trainee.
   *
   * @param traineeTisId The ID of the trainee to update.
   * @return True if the programme memberships were deleted.
   */
  @DeleteMapping("/{traineeTisId}")
  public ResponseEntity<Boolean> deleteProgrammeMemberships(
      @PathVariable(name = "traineeTisId") String traineeTisId) {
    log.info("Delete all programme memberships of trainee with TIS ID {}", traineeTisId);
    try {
      boolean foundTrainee = service.deleteProgrammeMembershipsForTrainee(traineeTisId);
      if (!foundTrainee) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trainee not found.");
      }
    } catch (IllegalArgumentException | InvalidDataAccessApiUsageException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getLocalizedMessage());
      // other exceptions are possible, e.g. DataAccessException if MongoDB is down
    }
    return ResponseEntity.ok(true);
  }

  /**
   * Delete a programme membership of the trainee.
   *
   * @param traineeTisId          The ID of the trainee to update.
   * @param programmeMembershipId The ID of the programme membership to delete.
   * @return True if the programme membership was deleted.
   */
  @DeleteMapping("/{traineeTisId}/{programmeMembershipId}")
  public ResponseEntity<Boolean> deleteProgrammeMembership(
      @PathVariable(name = "traineeTisId") String traineeTisId,
      @PathVariable(name = "programmeMembershipId") String programmeMembershipId) {
    log.info("Delete programme membership {} of trainee with TIS ID {}", programmeMembershipId,
        traineeTisId);
    try {
      boolean found = service.deleteProgrammeMembershipForTrainee(traineeTisId,
          programmeMembershipId);
      if (!found) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ProgrammeMembership not found.");
      }
    } catch (IllegalArgumentException | InvalidDataAccessApiUsageException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getLocalizedMessage());
      // other exceptions are possible, e.g. DataAccessException if MongoDB is down
    }
    return ResponseEntity.ok(true);
  }

  /**
   * Sign Condition of Joining with the given programme membership, setting version to the latest
   * Gold Guide version, and signedAt to current time.
   *
   * @param programmeMembershipId The ID of the programme membership for signing COJ.
   * @return The updated Programme Membership.
   */
  @PostMapping("/{programmeMembershipId}/sign-coj")
  public ResponseEntity<ProgrammeMembershipDto> signCoj(
      @PathVariable String programmeMembershipId,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
    log.info("Signing COJ with Programme Membership ID {}", programmeMembershipId);

    String traineeTisId;
    try {
      traineeTisId = AuthTokenUtil.getTraineeTisId(token);
    } catch (IOException e) {
      log.warn("Unable to read tisId from token.", e);
      return ResponseEntity.badRequest().build();
    }

    Optional<ProgrammeMembership> optionalEntity = service
        .signProgrammeMembershipCoj(traineeTisId, programmeMembershipId);
    ProgrammeMembership entity = optionalEntity
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Trainee with Programme Membership " + programmeMembershipId + " not found."));

    rabbitPublishService.publishCojSignedEvent(entity);

    return ResponseEntity.ok(mapper.toDto(entity));
  }

  /**
   * Determine whether the given programme membership represents a 'new starter' event or not.
   *
   * @param traineeTisId          The ID of the trainee.
   * @param programmeMembershipId The ID of the programme membership to assess.
   * @return True if the programme membership is a new starter, otherwise false.
   */
  @GetMapping("/isnewstarter/{traineeTisId}/{programmeMembershipId}")
  public ResponseEntity<Boolean> isNewStarter(
      @PathVariable(name = "traineeTisId") String traineeTisId,
      @PathVariable(name = "programmeMembershipId") String programmeMembershipId) {
    log.info("Assess new starter status: programme membership {} of trainee with TIS ID {}",
        programmeMembershipId, traineeTisId);
    try {
      boolean isNewStarter = service.isNewStarter(traineeTisId, programmeMembershipId);
      return ResponseEntity.ok(isNewStarter);
    } catch (IllegalArgumentException | InvalidDataAccessApiUsageException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getLocalizedMessage());
      // other exceptions are possible, e.g. DataAccessException if MongoDB is down
    }
  }
}
