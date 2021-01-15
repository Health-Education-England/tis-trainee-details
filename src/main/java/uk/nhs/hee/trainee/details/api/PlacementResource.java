package uk.nhs.hee.trainee.details.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uk.nhs.hee.trainee.details.dto.PlacementDto;
import uk.nhs.hee.trainee.details.mapper.PlacementMapper;
import uk.nhs.hee.trainee.details.model.Placement;
import uk.nhs.hee.trainee.details.service.PlacementService;

import java.util.Optional;

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
   * Update the person details for the trainee, creates the parent profile if it does not already
   * exist.
   *
   * @param traineeTisId The ID of the trainee to update.
   * @param dto          The person details to update with.
   * @return The updated or created Placement.
   */
  @PatchMapping("/{traineeTisId}")
  public ResponseEntity<PlacementDto> updatePlacement(
      @PathVariable(name = "traineeTisId") String traineeTisId,
      @RequestBody @Validated PlacementDto dto) {
    log.trace("Update basic details of trainee with TIS ID {}", traineeTisId);
    Placement entity = mapper.toEntity(dto);
    Optional<Placement> optionalEntity = service
        .updatePlacementByTisId(traineeTisId, entity);
    entity = optionalEntity
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trainee not found."));
    return ResponseEntity.ok(mapper.toDto(entity));
  }
}
