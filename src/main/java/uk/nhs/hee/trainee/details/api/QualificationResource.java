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

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.HeadersBuilder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.nhs.hee.trainee.details.dto.QualificationDto;
import uk.nhs.hee.trainee.details.mapper.QualificationMapper;
import uk.nhs.hee.trainee.details.model.Qualification;
import uk.nhs.hee.trainee.details.service.QualificationService;

@Slf4j
@RestController
@RequestMapping("/api/qualification")
public class QualificationResource {

  private final QualificationService service;
  private final QualificationMapper mapper;

  public QualificationResource(QualificationService service, QualificationMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  /**
   * Update the person details for the trainee, creates the parent profile if it does not already
   * exist.
   *
   * @param traineeTisId The ID of the trainee to update.
   * @param dto          The person details to update with.
   * @return The updated or created Qualification.
   */
  @PatchMapping("/{traineeTisId}")
  public ResponseEntity<QualificationDto> updateQualification(
      @PathVariable(name = "traineeTisId") String traineeTisId,
      @RequestBody @Validated QualificationDto dto) {
    log.trace("Update qualifications of trainee with TIS ID {}", traineeTisId);
    Qualification entity = mapper.toEntity(dto);
    Optional<Qualification> optionalEntity = service
        .updateQualificationByTisId(traineeTisId, entity);
    entity = optionalEntity
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trainee not found."));
    return ResponseEntity.ok(mapper.toDto(entity));
  }

  /**
   * Delete the qualification for the given trainee and qualification IDs.
   *
   * @param traineeTisId    The trainee ID to delete the qualification of.
   * @param qualificationId The qualification ID to delete from the trainee.
   * @return A 204 if successful, else a 404.
   */
  @DeleteMapping("/{traineeTisId}/{qualificationId}")
  public ResponseEntity<Void> deleteQualification(@PathVariable String traineeTisId,
      @PathVariable String qualificationId) {
    log.trace("Delete qualification {} from trainee {}.", qualificationId, traineeTisId);
    boolean updated = service.deleteQualification(traineeTisId, qualificationId);

    HeadersBuilder<? extends HeadersBuilder<?>> response =
        updated ? ResponseEntity.noContent() : ResponseEntity.notFound();
    return response.build();
  }
}
