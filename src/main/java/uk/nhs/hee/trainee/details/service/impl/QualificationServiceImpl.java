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

package uk.nhs.hee.trainee.details.service.impl;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.mapper.QualificationMapper;
import uk.nhs.hee.trainee.details.model.Qualification;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.service.QualificationService;
import uk.nhs.hee.trainee.details.service.TraineeProfileService;

@Service
public class QualificationServiceImpl implements QualificationService {

  private final TraineeProfileService service;
  private QualificationMapper mapper;

  QualificationServiceImpl(TraineeProfileService service, QualificationMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @Override
  public Optional<Qualification> updateQualificationByTisId(String tisId,
      Qualification qualification) {
    TraineeProfile traineeProfile = service.getTraineeProfileByTraineeTisId(tisId);

    if (traineeProfile == null) {
      return Optional.empty();
    }

    List<Qualification> existingQualifications = traineeProfile.getQualifications();

    for (Qualification existingQualification : existingQualifications) {

      if (existingQualification.getTisId().equals(qualification.getTisId())) {
        mapper.updateQualification(existingQualification, qualification);
        service.save(traineeProfile);
        return Optional.of(existingQualification);
      }
    }

    existingQualifications.add(qualification);
    service.save(traineeProfile);
    return Optional.of(qualification);
  }
}
