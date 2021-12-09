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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.dto.enumeration.Status;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.model.Qualification;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;

@Service
public class TraineeProfileService {

  private final TraineeProfileRepository repository;

  TraineeProfileService(TraineeProfileRepository repository) {
    this.repository = repository;
  }

  /**
   * Get the trainee profile associated with the given TIS ID.
   *
   * @param traineeTisId The TIS ID of the trainee.
   * @return The trainee's profile.
   */
  public TraineeProfile getTraineeProfileByTraineeTisId(String traineeTisId) {
    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);

    if (traineeProfile != null) {
      traineeProfile.getQualifications().sort(Comparator.comparing(
          Qualification::getDateAttained, Comparator.nullsLast(Comparator.reverseOrder()))
      );

      // TODO: Remove when FE can handle collection of qualifications directly.
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

    return traineeProfile;
  }

  /**
   * Get the trainee IDs associated with the given email.
   *
   * @param email The email address of the trainee.
   * @return The trainee TIS IDs.
   */
  public List<String> getTraineeTisIdsByByEmail(String email) {
    List<TraineeProfile> traineeProfile = repository.findAllByTraineeEmail(email.toLowerCase());
    return traineeProfile.stream()
        .map(TraineeProfile::getTraineeTisId)
        .collect(Collectors.toList());
  }

  /**
   * Remove past programmes from the trainee profile.
   *
   * @param traineeProfile The trainee profile to modify.
   * @return The modified trainee profile.
   */
  public TraineeProfile hidePastProgrammes(TraineeProfile traineeProfile) {
    traineeProfile.getProgrammeMemberships().removeIf(c -> c.getStatus() == Status.PAST);
    return traineeProfile;
  }

  /**
   * Remove past placements from the trainee profile.
   *
   * @param traineeProfile The trainee profile to modify.
   * @return The modified trainee profile.
   */
  public TraineeProfile hidePastPlacements(TraineeProfile traineeProfile) {
    traineeProfile.getPlacements().removeIf(c -> c.getStatus() == Status.PAST);
    return traineeProfile;
  }
}
