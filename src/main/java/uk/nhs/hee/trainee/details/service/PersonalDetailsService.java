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

import com.amazonaws.xray.spring.aop.XRayEnabled;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.dto.EmailUpdateDto;
import uk.nhs.hee.trainee.details.dto.GmcDetailsDto;
import uk.nhs.hee.trainee.details.mapper.TraineeProfileMapper;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.model.PersonalDetailsUpdated;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;

@Slf4j
@Service
@XRayEnabled
public class PersonalDetailsService {

  private final TraineeProfileRepository repository;
  private final TraineeProfileMapper mapper;
  private final EventPublishService eventService;

  PersonalDetailsService(TraineeProfileRepository repository, TraineeProfileMapper mapper,
      EventPublishService eventService) {
    this.repository = repository;
    this.mapper = mapper;
    this.eventService = eventService;
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
    PersonalDetailsUpdated updatedDetails = updatePersonalDetailsByTisId(tisId, personalDetails,
        mapper::updateBasicDetails);
    Optional<PersonalDetails> updatedPersonalDetails =  updatedDetails.getPersonalDetails();

    if (updatedPersonalDetails.isEmpty()) {
      TraineeProfile traineeProfile = new TraineeProfile();
      traineeProfile.setTraineeTisId(tisId);
      mapper.updateBasicDetails(traineeProfile, personalDetails);
      TraineeProfile savedProfile = repository.save(traineeProfile);

      eventService.publishProfileCreateEvent(savedProfile);
      return savedProfile.getPersonalDetails();
    } else {
      return updatedPersonalDetails.get();
    }
  }

  /**
   * Update the GDC details for the trainee with the given TIS ID.
   *
   * @param tisId           The TIS id of the trainee.
   * @param personalDetails The personal details to add to the trainee.
   * @return The updated personal details or empty if a trainee with the ID was not found.
   */
  public PersonalDetailsUpdated updateGdcDetailsByTisId(String tisId,
      PersonalDetails personalDetails) {
    return updatePersonalDetailsByTisId(tisId, personalDetails, mapper::updateGdcDetails);
  }

  /**
   * Update the GMC details of a trainee using the details.
   *
   * @param tisId      The TIS id of the trainee.
   * @param gmcDetails The GMC details to update the personal detail with.
   * @return The updated personal details or empty if a trainee with the ID was not found.
   */
  public Optional<PersonalDetails> updateGmcDetailsWithTraineeProvidedDetails(String tisId,
      GmcDetailsDto gmcDetails) {
    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(gmcDetails.gmcNumber());

    PersonalDetailsUpdated updatedDetails = updateGmcDetailsByTisId(tisId, personalDetails);

    updatedDetails.getPersonalDetails().ifPresent(details -> {
      // if gmcNumber is updated
      if (updatedDetails.isUpdated()) {
        // don't publish to TIS if it is a test user
        if (tisId.startsWith("-")) {
          log.warn("Tester trainee ID {} provided. Ignore GMC number update.", tisId);
        }
        else {
          log.info("Trainee {} updated their GMC number to {}.", tisId, details.getGmcNumber());
          eventService.publishGmcDetailsProvidedEvent(tisId, gmcDetails);
        }
      }
    });

    return updatedDetails.getPersonalDetails();
  }

  /**
   * Update the GMC details for the trainee with the given TIS ID.
   *
   * @param tisId           The TIS id of the trainee.
   * @param personalDetails The personal details to add to the trainee.
   * @return The updated personal details or empty if a trainee with the ID was not found.
   */
  public PersonalDetailsUpdated updateGmcDetailsByTisId(String tisId,
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
  public PersonalDetailsUpdated updatePersonOwnerByTisId(String tisId,
      PersonalDetails personalDetails) {
    BiConsumer<TraineeProfile, PersonalDetails> updateFunction;

    if (personalDetails.getPersonOwner() == null) {
      log.info("Person owner null for profile ID '{}', retaining existing owner.", tisId);
      updateFunction = (profile, details) -> {
      };
    } else {
      updateFunction = mapper::updatePersonOwner;
    }

    return updatePersonalDetailsByTisId(tisId, personalDetails, updateFunction);
  }

  /**
   * Update the contact details for the trainee with the given TIS ID.
   *
   * @param tisId           The TIS id of the trainee.
   * @param personalDetails The personal details to add to the trainee.
   * @return The updated personal details or empty if a trainee with the ID was not found.
   */
  public PersonalDetailsUpdated updateContactDetailsByTisId(String tisId,
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
  public PersonalDetailsUpdated updatePersonalInfoByTisId(String tisId,
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
  private PersonalDetailsUpdated updatePersonalDetailsByTisId(String tisId,
      PersonalDetails personalDetails, BiConsumer<TraineeProfile, PersonalDetails> updateFunction) {
    TraineeProfile traineeProfile = repository.findByTraineeTisId(tisId);
    if (traineeProfile == null) {
      return new PersonalDetailsUpdated(false, Optional.empty());
    }

    TraineeProfile originalDto = mapper.cloneTraineeProfile(traineeProfile);
    updateFunction.accept(traineeProfile, personalDetails);

    if (traineeProfile.equals(originalDto)) {
      log.info("No new changes in traineeProfile for {}, ignore update.", tisId);
      return new PersonalDetailsUpdated(false,
          Optional.of(originalDto.getPersonalDetails()));
    }
    return new PersonalDetailsUpdated(true,
        Optional.of(repository.save(traineeProfile).getPersonalDetails()));
  }

  /**
   * Check if a changed email address is used by any other trainee profiles.
   *
   * @param tisId The TIS id of the current trainee (which is excluded).
   * @param email The email address to check.
   * @return true if unique, false if not unique.
   */
  public boolean isEmailChangeUnique(String tisId, String email) {
    List<TraineeProfile> profilesWithEmail = repository.findAllByTraineeEmail(email);
    if (!profilesWithEmail.isEmpty()) {
      log.warn("Email address {} for trainee {} is already in use (by at least one other trainee, " +
              "or is unchanged from their current email).",
          tisId, email);
      return false;
    }
    return true;
  }

  /**
   * Request update of the email address of a trainee using the details.
   *
   * @param tisId        The TIS id of the trainee.
   * @param emailDetails The email address to update the personal detail with.
   * @return true if request was made, otherwise false.
   */
  public boolean requestUpdateEmailWithTraineeProvidedDetails(String tisId,
      EmailUpdateDto emailDetails) {

    TraineeProfile traineeProfile = repository.findByTraineeTisId(tisId);
    if (traineeProfile == null) {
      log.warn("Trainee profile not found for TIS ID {}. Cannot request email update.", tisId);
      return false;
    }
    // don't publish to TIS if it is a test user
    if (tisId.startsWith("-")) {
      log.warn("Tester trainee ID {} provided. Ignore email update.", tisId);
      return false;
    }
    log.info("Requesting update email to {} for trainee {}.", emailDetails.getEmail(), tisId);
    eventService.publishEmailDetailsProvidedEvent(tisId, emailDetails);
    return true;
  }
}
