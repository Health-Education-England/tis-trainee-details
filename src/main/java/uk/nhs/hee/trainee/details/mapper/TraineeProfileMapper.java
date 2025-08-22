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

package uk.nhs.hee.trainee.details.mapper;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.hee.trainee.details.dto.TraineeProfileDto;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.model.Placement;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.Qualification;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.service.TrainingNumberGenerator;

/**
 * A mapper to convert Trainee Profiles between entity and DTO representations.
 */
@Slf4j
@Mapper(componentModel = "spring", uses = {
    PersonalDetailsMapper.class,
    PlacementMapper.class,
    ProgrammeMembershipMapper.class
})
public abstract class TraineeProfileMapper {

  @Autowired
  protected TrainingNumberGenerator trainingNumberGenerator;

  public abstract TraineeProfileDto toDto(TraineeProfile traineeProfile);

  /**
   * Generate training numbers for all programme memberships in the profile.
   *
   * @param dto The trainee profile to generate training numbers for.
   */
  @AfterMapping
  protected void generateTrainingNumbers(@MappingTarget TraineeProfileDto dto) {
    try {
      trainingNumberGenerator.populateTrainingNumbers(dto);
    } catch (RuntimeException e) {
      // Failure to populate the training number should never block the profile being returned.
      log.error("Caught and ignoring training number generation runtime error:", e);
    }
  }

  @Mapping(target = "version", ignore = true)
  public abstract TraineeProfile toEntity(TraineeProfileDto traineeProfileDto);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "personalDetails.publicHealthNumber", source = "publicHealthNumber")
  @Mapping(target = "personalDetails.role", source = "role")
  public abstract void updateBasicDetails(@MappingTarget TraineeProfile target,
      PersonalDetails source);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "personalDetails.title", source = "title")
  @Mapping(target = "personalDetails.forenames", source = "forenames")
  @Mapping(target = "personalDetails.knownAs", source = "knownAs")
  @Mapping(target = "personalDetails.surname", source = "surname")
  @Mapping(target = "personalDetails.maidenName", source = "maidenName")
  @Mapping(target = "personalDetails.telephoneNumber", source = "telephoneNumber")
  @Mapping(target = "personalDetails.mobileNumber", source = "mobileNumber")
  @Mapping(target = "personalDetails.email", source = "email")
  @Mapping(target = "personalDetails.address1", source = "address1")
  @Mapping(target = "personalDetails.address2", source = "address2")
  @Mapping(target = "personalDetails.address3", source = "address3")
  @Mapping(target = "personalDetails.address4", source = "address4")
  @Mapping(target = "personalDetails.postCode", source = "postCode")
  public abstract void updateContactDetails(@MappingTarget TraineeProfile target,
      PersonalDetails source);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "personalDetails.gdcNumber", source = "gdcNumber")
  @Mapping(target = "personalDetails.gdcStatus", source = "gdcStatus")
  public abstract void updateGdcDetails(@MappingTarget TraineeProfile target,
      PersonalDetails source);

  /**
   * Pre GMC update process to set default GMC to registered if GMC number is updated.
   *
   * @param target The existing trainee profile.
   * @param source The new personal details.
   */
  @Named("updateGmc")
  @BeforeMapping
  protected void perUpdateGmcDetails(@MappingTarget TraineeProfile target,
      PersonalDetails source) {
    if (target.getPersonalDetails() != null) {
      // If the gmcNumber is updated
      if (!target.getPersonalDetails().getGmcNumber().equals(source.getGmcNumber())) {
        // Default all GMCs to registered until we can properly prompt/determine the correct status.
        target.getPersonalDetails().setGmcStatus("Registered with Licence");
      }
    } else if (source.getGmcNumber() != null) {
      // If target personal details is null and source GMC number has a new value
      target.setPersonalDetails(new PersonalDetails());
      target.getPersonalDetails().setGmcStatus("Registered with Licence");
    }
  }

  @BeanMapping(ignoreByDefault = true, qualifiedByName = "updateGmc")
  @Mapping(target = "personalDetails.gmcNumber", source = "gmcNumber")
  public abstract void updateGmcDetails(@MappingTarget TraineeProfile target,
      PersonalDetails source);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "personalDetails.personOwner", source = "personOwner")
  public abstract void updatePersonOwner(@MappingTarget TraineeProfile target,
      PersonalDetails source);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "personalDetails.dateOfBirth", source = "dateOfBirth")
  @Mapping(target = "personalDetails.gender", source = "gender")
  public abstract void updatePersonalInfo(@MappingTarget TraineeProfile target,
      PersonalDetails source);

  public abstract TraineeProfile cloneTraineeProfile(TraineeProfile source);

  public abstract PersonalDetails clonePersonalDetails(PersonalDetails source);

  public abstract List<Qualification> cloneQualifications(List<Qualification> source);

  public abstract List<ProgrammeMembership> cloneProgrammeMemberships(
      List<ProgrammeMembership> source);

  public abstract List<Placement> clonePlacements(List<Placement> source);
}
