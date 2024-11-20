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
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.trainee.details.dto.CctCalculationDetailDto;
import uk.nhs.hee.trainee.details.dto.CctCalculationSummaryDto;
import uk.nhs.hee.trainee.details.dto.validation.Create;
import uk.nhs.hee.trainee.details.service.CctService;

/**
 * A REST controller for CCT related endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/cct")
@XRayEnabled
public class CctResource {

  private final CctService service;

  /**
   * Construct a REST controller for CCT related endpoints.
   *
   * @param service A service providing CCT functionality.
   */
  public CctResource(CctService service) {
    this.service = service;
  }

  /**
   * Get a list of CCT calculation summaries for the current user.
   *
   * @return The found CCT calculation summaries.
   */
  @GetMapping("/calculation")
  public ResponseEntity<List<CctCalculationSummaryDto>> getCalculationSummaries() {
    log.info("Request to get a summary for all CCT calculations");
    List<CctCalculationSummaryDto> calculations = service.getCalculations();
    return ResponseEntity.ok(calculations);
  }

  /**
   * Get the details of a CCT calculation with the given ID.
   *
   * @param id The ID of the calculation to return.
   * @return The found CCT calculation details.
   */
  @GetMapping("/calculation/{id}")
  public ResponseEntity<CctCalculationDetailDto> getCalculationDetails(@PathVariable ObjectId id) {
    log.info("Request to get details for CCT calculation[{}]", id);
    Optional<CctCalculationDetailDto> calculation = service.getCalculation(id);
    return ResponseEntity.of(calculation);
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

  @PostMapping("/calculate")
  public ResponseEntity<CctCalculationDetailDto> calculateCctDate(
      @RequestBody CctCalculationDetailDto calculation) {
    log.info("Request to calculate CCT date [{}]", calculation.name());
    Optional<CctCalculationDetailDto> cctDateCalculation = service.calculateCctDate(calculation);
    if (cctDateCalculation.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.of(cctDateCalculation);
  }
}
