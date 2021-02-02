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

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.nhs.hee.trainee.details.dto.ProgrammeMembershipDto;
import uk.nhs.hee.trainee.details.mapper.ProgrammeMembershipMapper;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.service.ProgrammeMembershipService;

@Slf4j
@RestController
@RequestMapping("/api/programme-membership")
public class ProgrammeMembershipResource {

  private ProgrammeMembershipService service;
  private ProgrammeMembershipMapper mapper;

  public ProgrammeMembershipResource(ProgrammeMembershipService service,
      ProgrammeMembershipMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  /**
   * Update the programme membership for the trainee, creates the parent profile if it does not
   * already exist.
   *
   * @param traineeTisId The ID of the trainee to update.
   * @param dto          The person details to update with.
   * @return The updated or created Qualification.
   */
  @PatchMapping("/{traineeTisId}")
  public ResponseEntity<ProgrammeMembershipDto> updateQualification(
      @PathVariable(name = "traineeTisId") String traineeTisId,
      @RequestBody @Validated ProgrammeMembershipDto dto) {
    log.trace("Update programme membership of trainee with TIS ID {}", traineeTisId);
    ProgrammeMembership entity = mapper.toEntity(dto);
    Optional<ProgrammeMembership> optionalEntity = service
        .updateProgrammeMembershipForTrainee(traineeTisId, entity);
    entity = optionalEntity
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trainee not found."));
    return ResponseEntity.ok(mapper.toDto(entity));
  }
}
