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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

  public static final String TITLE = "title-";
  public static final String FORENAMES = "forenames-";
  public static final String KNOWN_AS = "knownAs-";
  public static final String SURNAME = "surname-";
  public static final String MAIDEN_NAME = "maidenName-";
  public static final String TELEPHONE_NUMBER = "telephoneNumber-";
  public static final String MOBILE_NUMBER = "mobileNumber-";
  public static final String EMAIL = "email-";
  public static final String ADDRESS_1 = "address1-";
  public static final String ADDRESS_2 = "address2-";
  public static final String ADDRESS_3 = "address3-";
  public static final String ADDRESS_4 = "address4-";
  public static final String POST_CODE = "postCode-";
  public static final String PERSON_OWNER = "personOwner-";
  public static final String GENDER = "gender-";
  public static final LocalDate DATE = LocalDate.EPOCH;
  public static final String QUALIFICATION = "qualification-";
  public static final String MEDICAL_SCHOOL = "medicalSchool-";
  public static final String GMC_NUMBER = "gmcNumber-";
  public static final String GMC_STATUS = "gmcStatus-";
  public static final String GDC_NUMBER = "gdcNumber-";
  public static final String GDC_STATUS = "gdcStatus-";
  public static final String PUBLIC_HEALTH_NUMBER = "publicHealthNumber-";
  public static final String EEA_RESIDENT = "eeaResident-";
  public static final String PERMIT_TO_WORK = "permitToWork-";
  public static final String SETTLED = "settled-";
  public static final String DETAILS_NUMBER = "detailsNumber-";
  public static final String PREV_REVAL_BODY = "prevRevalBody-";
  public static final String TRAINEE_TIS_ID = "40";
  public static final String MODIFIED_SUFFIX = "post";
  public static final String ORIGINAL_SUFFIX = "pre";
  public static final int ONE_HUNDRED = 100;

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

  @Test
  void shouldNotUpdatePersonalInfoWhenTraineeIdNotFound() {
    PersonalDetails personalDetailsMock = mock(PersonalDetails.class);
    Optional<PersonalDetails> personalDetails = service
        .updatePersonalInfoByTisId("notFound", personalDetailsMock);

    assertTrue(personalDetails.isEmpty(), "Expected personal details to have been empty");
    verify(repository).findByTraineeTisId("notFound");
    verifyNoInteractions(personalDetailsMock);
  }

  @Test
  void shouldUpdatePersonalInfoWhenTraineeIdFound() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPersonalDetails(createPersonalDetails(ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<PersonalDetails> personalDetails = service
        .updatePersonalInfoByTisId(
            TRAINEE_TIS_ID, createPersonalDetails(MODIFIED_SUFFIX, ONE_HUNDRED));

    assertTrue(personalDetails.isPresent(), "Expected a PersonalDetails to be returned");

    PersonalDetails expectedPersonalDetails = createPersonalDetails(ORIGINAL_SUFFIX, 0);
    expectedPersonalDetails.setDateOfBirth(DATE.plusDays(ONE_HUNDRED));
    expectedPersonalDetails.setGender(GENDER + MODIFIED_SUFFIX);

    assertThat("Unexpected personal details.", personalDetails.get(), is(expectedPersonalDetails));
  }

  /**
   * Create an instance of PersonalDetails with default dummy values.
   *
   * @return The dummy entity.
   */
  private PersonalDetails createPersonalDetails(String stringSuffix, int dateAdjustmentDays) {
    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setTitle(TITLE + stringSuffix);
    personalDetails.setForenames(FORENAMES + stringSuffix);
    personalDetails.setKnownAs(KNOWN_AS + stringSuffix);
    personalDetails.setSurname(SURNAME + stringSuffix);
    personalDetails.setMaidenName(MAIDEN_NAME + stringSuffix);
    personalDetails.setTelephoneNumber(TELEPHONE_NUMBER + stringSuffix);
    personalDetails.setMobileNumber(MOBILE_NUMBER + stringSuffix);
    personalDetails.setEmail(EMAIL + stringSuffix);
    personalDetails.setAddress1(ADDRESS_1 + stringSuffix);
    personalDetails.setAddress2(ADDRESS_2 + stringSuffix);
    personalDetails.setAddress3(ADDRESS_3 + stringSuffix);
    personalDetails.setAddress4(ADDRESS_4 + stringSuffix);
    personalDetails.setPostCode(POST_CODE + stringSuffix);
    personalDetails.setPersonOwner(PERSON_OWNER + stringSuffix);
    personalDetails.setDateOfBirth(DATE.plusDays(dateAdjustmentDays));
    personalDetails.setGender(GENDER + stringSuffix);
    personalDetails.setQualification(QUALIFICATION + stringSuffix);
    personalDetails.setDateAttained(DATE.plusDays(dateAdjustmentDays));
    personalDetails.setMedicalSchool(MEDICAL_SCHOOL + stringSuffix);
    personalDetails.setGmcNumber(GMC_NUMBER + stringSuffix);
    personalDetails.setGmcStatus(GMC_STATUS + stringSuffix);
    personalDetails.setGdcNumber(GDC_NUMBER + stringSuffix);
    personalDetails.setGdcStatus(GDC_STATUS + stringSuffix);
    personalDetails.setPublicHealthNumber(PUBLIC_HEALTH_NUMBER + stringSuffix);
    personalDetails.setEeaResident(EEA_RESIDENT + stringSuffix);
    personalDetails.setPermitToWork(PERMIT_TO_WORK + stringSuffix);
    personalDetails.setSettled(SETTLED + stringSuffix);
    personalDetails.setVisaIssued(DATE.plusDays(dateAdjustmentDays));
    personalDetails.setDetailsNumber(DETAILS_NUMBER + stringSuffix);
    personalDetails.setPrevRevalBody(PREV_REVAL_BODY + stringSuffix);
    personalDetails.setCurrRevalDate(DATE.plusDays(dateAdjustmentDays));
    personalDetails.setPrevRevalDate(DATE.plusDays(dateAdjustmentDays));

    return personalDetails;
  }
}
