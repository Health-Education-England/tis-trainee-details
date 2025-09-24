/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.nhs.hee.trainee.details.dto.CctCalculationDetailDto;
import uk.nhs.hee.trainee.details.dto.UserDetails;
import uk.nhs.hee.trainee.details.dto.validation.Create;
import uk.nhs.hee.trainee.details.service.CctService;
import uk.nhs.hee.trainee.details.service.TraineeProfileService;

/**
 * A REST controller for CCT related endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/cct")
@XRayEnabled
public class CctResource {

  private final CctService service;
  private final TraineeProfileService traineeProfileService;

  /**
   * Construct a REST controller for CCT related endpoints.
   *
   * @param service               A service providing CCT functionality.
   * @param traineeProfileService A service providing trainee profile functionality.
   */
  public CctResource(CctService service, TraineeProfileService traineeProfileService) {
    this.traineeProfileService = traineeProfileService;
    this.service = service;
  }

  /**
   * Get a list of CCT calculation summaries for the current user.
   *
   * @return The found CCT calculation summaries.
   */
  @GetMapping("/calculation")
  public ResponseEntity<List<CctCalculationDetailDto>> getCalculationDetails() {
    log.info("Request to get details for all CCT calculations");
    List<CctCalculationDetailDto> calculations = service.getCalculations();
    return ResponseEntity.ok(calculations);
  }

  /**
   * Get the details of a CCT calculation with the given ID.
   *
   * @param id The ID of the calculation to return.
   * @return The found CCT calculation details.
   */
  @GetMapping("/calculation/{id}")
  public ResponseEntity<CctCalculationDetailDto> getCalculationDetails(@PathVariable UUID id) {
    log.info("Request to get details for CCT calculation[{}]", id);
    Optional<CctCalculationDetailDto> calculation = service.getCalculation(id);
    return ResponseEntity.of(calculation);
  }

  /**
   * Update an existing CCT calculation with the given ID.
   *
   * @param id The ID of the calculation to update.
   * @return The updated CCT calculation details, or bad request if inconsistent ids.
   */
  @PutMapping("/calculation/{id}")
  public ResponseEntity<CctCalculationDetailDto> updateCalculationDetails(@PathVariable UUID id,
      @Validated @RequestBody CctCalculationDetailDto calculation)
      throws MethodArgumentNotValidException {
    log.info("Request to update CCT calculation[{}]", id);
    Optional<CctCalculationDetailDto> savedCalculation;

    if (calculation.id() == null) {
      log.warn("Not updating CCT calculation because of missing id (use POST to create)");
      return ResponseEntity.badRequest().build();
    } else if (calculation.id().compareTo(id) != 0) {
      log.warn("Not updating CCT calculation because of id mismatch [{}] != [{}]", id,
          calculation.id());
      return ResponseEntity.badRequest().build();
    } else {
      savedCalculation = service.updateCalculation(id, calculation);
    }

    return ResponseEntity.of(savedCalculation);
  }

  /**
   * Delete an existing CCT calculation with the given ID.
   *
   * @param id The ID of the calculation to delete.
   * @return True if the calculation was deleted, false if not found.
   */
  @DeleteMapping("/calculation/{id}")
  public ResponseEntity<Boolean> deleteCalculation(@PathVariable UUID id) {
    log.info("Request to delete CCT calculation[{}]", id);
    try {
      boolean foundCalculation = service.deleteCalculation(id);
      if (!foundCalculation) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CCT calculation not found.");
      }
    } catch (IllegalArgumentException | InvalidDataAccessApiUsageException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getLocalizedMessage());
    }
    return ResponseEntity.ok(true);
  }

  /**
   * Create a new CCT calculation with the given details.
   *
   * @param calculation The CCT calculation details.
   * @return The created CCT calculation details.
   */
  @PostMapping("/calculation")
  public ResponseEntity<CctCalculationDetailDto> createCalculation(
      @Validated(Create.class) @RequestBody CctCalculationDetailDto calculation) {
    log.info("Request to create CCT calculation [{}]", calculation.name());
    CctCalculationDetailDto savedCalculation = service.createCalculation(calculation);

    log.info("Created CCT calculation [{}] with id [{}]", savedCalculation.name(),
        savedCalculation.id());
    return ResponseEntity.created(URI.create("/api/cct/calculation/" + savedCalculation.id()))
        .body(savedCalculation);
  }

  /**
   * Move all CCT calculations from one trainee to another.
   *
   * @param fromTraineeId The TIS ID of the trainee to move calculations from.
   * @param toTraineeId The TIS ID of the trainee to move calculations to.
   * @return True if the calculations were moved, bad request if the target trainee does not exist.
   */
  @PatchMapping("/move/{fromTraineeId}/{toTraineeId}")
  public ResponseEntity<Boolean> moveCalculations(@PathVariable String fromTraineeId,
      @PathVariable String toTraineeId) {
    log.info("Request to move CCT calculations from trainee {} to trainee {}",
        fromTraineeId, toTraineeId);

    Optional<UserDetails> toUserDetails
        = traineeProfileService.getTraineeDetailsByTisId(toTraineeId);
    if (toUserDetails.isEmpty()) {
      log.warn("Not moving CCT calculations because toTraineeId not found [{}]", toTraineeId);
      return ResponseEntity.badRequest().build();
    }

    service.moveCalculations(fromTraineeId, toTraineeId);
    return ResponseEntity.ok(true);
  }
}
