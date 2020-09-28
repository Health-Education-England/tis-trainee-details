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

import java.util.Comparator;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.dto.enumeration.Status;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.model.Qualification;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;
import uk.nhs.hee.trainee.details.service.TraineeProfileService;

@Service
public class TraineeProfileServiceImpl implements TraineeProfileService {

  private final TraineeProfileRepository repository;

  TraineeProfileServiceImpl(TraineeProfileRepository repository) {
    this.repository = repository;
  }

  @Override
  public TraineeProfile getTraineeProfileByTraineeTisId(String traineeTisId) {
    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);

    if (traineeProfile != null) {
      traineeProfile.getQualifications()
          .sort(Comparator.comparing(Qualification::getDateAttained).reversed());

      { // TODO: Remove when FE can handle collection of qualifications directly.
        if (!traineeProfile.getQualifications().isEmpty()) {
          Qualification qualification = traineeProfile.getQualifications().get(0);
          PersonalDetails personalDetails = traineeProfile.getPersonalDetails();

          if (personalDetails == null) {
            personalDetails = new PersonalDetails();
            traineeProfile.setPersonalDetails(personalDetails);
          }

          personalDetails.setQualification(qualification.getQualification());
          personalDetails.setDateAttained(qualification.getDateAttained());
          personalDetails.setMedicalSchool(qualification.getMedicalSchool());
        }
      }
    }

    return traineeProfile;
  }

  @Override
  public TraineeProfile save(TraineeProfile traineeProfile) {
    return repository.save(traineeProfile);
  }

  @Override
  public TraineeProfile hidePastProgrammes(TraineeProfile traineeProfile) {
    traineeProfile.getProgrammeMemberships().removeIf(c -> c.getStatus() == Status.PAST);
    return traineeProfile;
  }

  @Override
  public TraineeProfile hidePastPlacements(TraineeProfile traineeProfile) {
    traineeProfile.getPlacements().removeIf(c -> c.getStatus() == Status.PAST);
    return traineeProfile;
  }
}
