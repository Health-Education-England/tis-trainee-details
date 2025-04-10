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

import lombok.extern.slf4j.Slf4j;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.hee.trainee.details.dto.TraineeProfileDto;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
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

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "personalDetails.gmcNumber", source = "gmcNumber")
  @Mapping(target = "personalDetails.gmcStatus", source = "gmcStatus")
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
}
