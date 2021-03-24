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
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.nhs.hee.trainee.details.dto.PlacementDto;
import uk.nhs.hee.trainee.details.mapper.PlacementMapper;
import uk.nhs.hee.trainee.details.model.Placement;
import uk.nhs.hee.trainee.details.service.PlacementService;

@Slf4j
@RestController
@RequestMapping("/api/placement")
public class PlacementResource {

  private PlacementService service;
  private PlacementMapper mapper;

  public PlacementResource(PlacementService service, PlacementMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  /**
   * Update the placement for the trainee, creates the parent profile if it does not already exist.
   *
   * @param traineeTisId The ID of the trainee to update.
   * @param dto          The person details to update with.
   * @return The updated or created Qualification.
   */
  @PatchMapping("/{traineeTisId}")
  public ResponseEntity<PlacementDto> updateQualification(
      @PathVariable(name = "traineeTisId") String traineeTisId,
      @RequestBody @Validated PlacementDto dto) {
    log.trace("Update placement of trainee with TIS ID {}", traineeTisId);
    Placement entity = mapper.toEntity(dto);
    Optional<Placement> optionalEntity = service.updatePlacementForTrainee(traineeTisId, entity);
    entity = optionalEntity
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trainee not found."));
    return ResponseEntity.ok(mapper.toDto(entity));
  }

  /**
   * Delete the placement for the trainee.
   *
   * @param traineeTisId    The ID of the trainee to update.
   * @param placementTisId  The ID of the placement to delete.
   * @return True if the placement was deleted.
   */
  @DeleteMapping("/{traineeTisId}/{placementTisId}")
  public ResponseEntity<Boolean> deletePlacement(
      @PathVariable(name = "traineeTisId") String traineeTisId,
      @PathVariable(name = "placementTisId") String placementTisId) {
    log.trace("Delete placement with TIS ID {} of trainee with TIS ID {}",
        placementTisId, traineeTisId);
    try {
      boolean foundTraineeAndPlacement =
          service.deletePlacementForTrainee(traineeTisId, placementTisId);
      if (!foundTraineeAndPlacement) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trainee or placement not found.");
      }
    } catch (IllegalArgumentException | InvalidDataAccessApiUsageException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getLocalizedMessage());
      // other exceptions are possible, e.g. DataAccessException if MongoDB is down
    }
    return ResponseEntity.ok(true);
  }
}
