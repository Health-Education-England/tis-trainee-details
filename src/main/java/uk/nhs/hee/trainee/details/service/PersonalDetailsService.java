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

import java.util.Optional;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.mapper.TraineeProfileMapper;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;

@Service
public class PersonalDetailsService {

  private final TraineeProfileRepository repository;
  private final TraineeProfileMapper mapper;

  PersonalDetailsService(TraineeProfileRepository repository, TraineeProfileMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  /**
   * Update the basic details for the trainee with the given TIS ID, or create a new profile if the
   * trainee does not exist.
   *
   * @param tisId           The TIS id of the trainee.
   * @param personalDetails The personal details to add to the trainee.
   * @return The updated or created personal details.
   */
  public PersonalDetails createProfileOrUpdateBasicDetailsByTisId(String tisId,
      PersonalDetails personalDetails) {
    Optional<PersonalDetails> updatedDetails = updatePersonalDetailsByTisId(tisId, personalDetails,
        mapper::updateBasicDetails);

    if (updatedDetails.isEmpty()) {
      TraineeProfile traineeProfile = new TraineeProfile();
      traineeProfile.setTraineeTisId(tisId);
      mapper.updateBasicDetails(traineeProfile, personalDetails);
      TraineeProfile savedProfile = repository.save(traineeProfile);
      return savedProfile.getPersonalDetails();
    }

    return updatedDetails.get();
  }

  /**
   * Update the GDC details for the trainee with the given TIS ID.
   *
   * @param tisId           The TIS id of the trainee.
   * @param personalDetails The personal details to add to the trainee.
   * @return The updated personal details or empty if a trainee with the ID was not found.
   */
  public Optional<PersonalDetails> updateGdcDetailsByTisId(String tisId,
      PersonalDetails personalDetails) {
    return updatePersonalDetailsByTisId(tisId, personalDetails, mapper::updateGdcDetails);
  }

  /**
   * Update the GMC details for the trainee with the given TIS ID.
   *
   * @param tisId           The TIS id of the trainee.
   * @param personalDetails The personal details to add to the trainee.
   * @return The updated personal details or empty if a trainee with the ID was not found.
   */
  public Optional<PersonalDetails> updateGmcDetailsByTisId(String tisId,
      PersonalDetails personalDetails) {
    return updatePersonalDetailsByTisId(tisId, personalDetails, mapper::updateGmcDetails);
  }

  /**
   * Update the person owner for the trainee with the given TIS ID.
   *
   * @param tisId           The TIS id of the trainee.
   * @param personalDetails The personal details to add to the trainee.
   * @return The updated personal details or empty if a trainee with the ID was not found.
   */
  public Optional<PersonalDetails> updatePersonOwnerByTisId(String tisId,
      PersonalDetails personalDetails) {
    return updatePersonalDetailsByTisId(tisId, personalDetails, mapper::updatePersonOwner);
  }

  /**
   * Update the contact details for the trainee with the given TIS ID.
   *
   * @param tisId           The TIS id of the trainee.
   * @param personalDetails The personal details to add to the trainee.
   * @return The updated personal details or empty if a trainee with the ID was not found.
   */
  public Optional<PersonalDetails> updateContactDetailsByTisId(String tisId,
      PersonalDetails personalDetails) {
    return updatePersonalDetailsByTisId(tisId, personalDetails, mapper::updateContactDetails);
  }

  /**
   * Update the personal info for the trainee with the given TIS ID.
   *
   * @param tisId           The TIS id of the trainee.
   * @param personalDetails The personal details to add to the trainee.
   * @return The updated personal details or empty if a trainee with the ID was not found.
   */
  public Optional<PersonalDetails> updatePersonalInfoByTisId(String tisId,
      PersonalDetails personalDetails) {
    return updatePersonalDetailsByTisId(tisId, personalDetails, mapper::updatePersonalInfo);
  }

  /**
   * Update the Personal Details entity for the given TIS ID.
   *
   * @param tisId           The TIS id of the trainee.
   * @param personalDetails The personal details to add to the trainee.
   * @param updateFunction  The function to use to update the personal details.
   * @return The updated personal details or empty if a trainee with the ID was not found.
   */
  private Optional<PersonalDetails> updatePersonalDetailsByTisId(String tisId,
      PersonalDetails personalDetails, BiConsumer<TraineeProfile, PersonalDetails> updateFunction) {
    TraineeProfile traineeProfile = repository.findByTraineeTisId(tisId);

    if (traineeProfile == null) {
      return Optional.empty();
    }

    updateFunction.accept(traineeProfile, personalDetails);
    return Optional.of(repository.save(traineeProfile).getPersonalDetails());
  }
}
