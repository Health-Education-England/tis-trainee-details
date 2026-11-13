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

package uk.nhs.hee.trainee.details.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.mapper.QualificationMapper;
import uk.nhs.hee.trainee.details.model.Qualification;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;

@Service
public class QualificationService {

  private final TraineeProfileRepository repository;
  private final QualificationMapper mapper;

  QualificationService(TraineeProfileRepository repository, QualificationMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  /**
   * Update the qualification for the trainee with the given TIS ID.
   *
   * @param tisId         The TIS id of the trainee.
   * @param qualification The qualification to update for the trainee.
   * @return The updated qualification or empty if a trainee with the ID was not found.
   */
  public Optional<Qualification> updateQualificationByTisId(String tisId,
      Qualification qualification) {

    TraineeProfile traineeProfile = repository.findByTraineeTisId(tisId);

    if (traineeProfile == null) {
      return Optional.empty();
    }

    List<Qualification> existingQualifications = traineeProfile.getQualifications();

    for (Qualification existingQualification : existingQualifications) {

      if (existingQualification.getTisId().equals(qualification.getTisId())) {
        mapper.updateQualification(existingQualification, qualification);
        repository.save(traineeProfile);
        return Optional.of(existingQualification);
      }
    }

    existingQualifications.add(qualification);
    repository.save(traineeProfile);
    return Optional.of(qualification);
  }

  /**
   * Delete the qualification for the given trainee and qualification IDs.
   *
   * @param traineeTisId    The trainee ID to delete the qualification of.
   * @param qualificationId The qualification ID to delete from the trainee.
   */
  public void deleteQualification(String traineeTisId, String qualificationId) {
    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);

    if (traineeProfile != null) {
      List<Qualification> qualifications = traineeProfile.getQualifications();
      boolean updated = qualifications.removeIf(q -> q.getTisId().equals(qualificationId));

      if (updated) {
        repository.save(traineeProfile);
      }
    }
  }
}
