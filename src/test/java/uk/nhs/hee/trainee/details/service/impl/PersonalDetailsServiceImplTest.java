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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.nhs.hee.trainee.details.mapper.PersonalDetailsMapper;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;
import uk.nhs.hee.trainee.details.service.PersonalDetailsService;

class PersonalDetailsServiceImplTest {

  private PersonalDetailsService service;

  private TraineeProfileRepository repository;

  @BeforeEach
  void setUp() {
    repository = mock(TraineeProfileRepository.class);
    TraineeProfileServiceImpl profileService = new TraineeProfileServiceImpl(repository);
    service = new PersonalDetailsServiceImpl(profileService,
        Mappers.getMapper(PersonalDetailsMapper.class));
  }

  @Test
  void shouldNotUpdateContactDetailsWhenTraineeIdNotFound() {
    Optional<PersonalDetails> personalDetails = service
        .updateContactDetailsByTisId("notFound", new PersonalDetails());

    assertThat("Unexpected optional isEmpty flag.", personalDetails.isEmpty(), is(true));
    verify(repository).findByTraineeTisId("notFound");
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldUpdateContactDetailsWhenTraineeIdFound() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPersonalDetails(createPersonalDetails("pre", 0));

    when(repository.findByTraineeTisId("40")).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<PersonalDetails> personalDetails = service
        .updateContactDetailsByTisId("40", createPersonalDetails("post", 100));

    assertThat("Unexpected optional isEmpty flag.", personalDetails.isEmpty(), is(false));

    PersonalDetails expectedPersonalDetails = createPersonalDetails("pre", 0);
    expectedPersonalDetails.setTitle("title-post");
    expectedPersonalDetails.setForenames("forenames-post");
    expectedPersonalDetails.setKnownAs("knownAs-post");
    expectedPersonalDetails.setSurname("surname-post");
    expectedPersonalDetails.setMaidenName("maidenName-post");
    expectedPersonalDetails.setTelephoneNumber("telephoneNumber-post");
    expectedPersonalDetails.setMobileNumber("mobileNumber-post");
    expectedPersonalDetails.setEmail("email-post");
    expectedPersonalDetails.setAddress1("address1-post");
    expectedPersonalDetails.setAddress2("address2-post");
    expectedPersonalDetails.setAddress3("address3-post");
    expectedPersonalDetails.setAddress4("address4-post");
    expectedPersonalDetails.setPostCode("postCode-post");

    assertThat("Unexpected personal details.", personalDetails.get(), is(expectedPersonalDetails));
  }

  /**
   * Create an instance of PersonalDetails with default dummy values.
   *
   * @return The dummy entity.
   */
  private PersonalDetails createPersonalDetails(String stringSuffix, int dateAdjustmentDays) {
    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setTitle("title-" + stringSuffix);
    personalDetails.setForenames("forenames-" + stringSuffix);
    personalDetails.setKnownAs("knownAs-" + stringSuffix);
    personalDetails.setSurname("surname-" + stringSuffix);
    personalDetails.setMaidenName("maidenName-" + stringSuffix);
    personalDetails.setTelephoneNumber("telephoneNumber-" + stringSuffix);
    personalDetails.setMobileNumber("mobileNumber-" + stringSuffix);
    personalDetails.setEmail("email-" + stringSuffix);
    personalDetails.setAddress1("address1-" + stringSuffix);
    personalDetails.setAddress2("address2-" + stringSuffix);
    personalDetails.setAddress3("address3-" + stringSuffix);
    personalDetails.setAddress4("address4-" + stringSuffix);
    personalDetails.setPostCode("postCode-" + stringSuffix);
    personalDetails.setPersonOwner("personOwner-" + stringSuffix);
    personalDetails.setDateOfBirth(LocalDate.EPOCH.plusDays(dateAdjustmentDays));
    personalDetails.setGender("gender-" + stringSuffix);
    personalDetails.setQualification("qualification-" + stringSuffix);
    personalDetails.setDateAttained(LocalDate.EPOCH.plusDays(dateAdjustmentDays));
    personalDetails.setMedicalSchool("medicalSchool-" + stringSuffix);
    personalDetails.setGmcNumber("gmcNumber-" + stringSuffix);
    personalDetails.setGmcStatus("gmcStatus-" + stringSuffix);
    personalDetails.setGdcNumber("gdcNumber-" + stringSuffix);
    personalDetails.setGdcStatus("gdcStatus-" + stringSuffix);
    personalDetails.setPublicHealthNumber("publicHealthNumber-" + stringSuffix);
    personalDetails.setEeaResident("eeaResident-" + stringSuffix);
    personalDetails.setPermitToWork("permitToWork-" + stringSuffix);
    personalDetails.setSettled("settled-" + stringSuffix);
    personalDetails.setVisaIssued(LocalDate.EPOCH.plusDays(dateAdjustmentDays));
    personalDetails.setDetailsNumber("detailsNumber-" + stringSuffix);
    personalDetails.setPrevRevalBody("prevRevalBody-" + stringSuffix);
    personalDetails.setCurrRevalDate(LocalDate.EPOCH.plusDays(dateAdjustmentDays));
    personalDetails.setPrevRevalDate(LocalDate.EPOCH.plusDays(dateAdjustmentDays));

    return personalDetails;
  }
}
