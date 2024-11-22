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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.nhs.hee.trainee.details.dto.GmcDetailsDto;
import uk.nhs.hee.trainee.details.mapper.TraineeProfileMapperImpl;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;

class PersonalDetailsServiceTest {

  private static final String TITLE = "title-";
  private static final String FORENAMES = "forenames-";
  private static final String KNOWN_AS = "knownAs-";
  private static final String SURNAME = "surname-";
  private static final String MAIDEN_NAME = "maidenName-";
  private static final String TELEPHONE_NUMBER = "telephoneNumber-";
  private static final String MOBILE_NUMBER = "mobileNumber-";
  private static final String EMAIL = "contact-";
  private static final String ADDRESS_1 = "address1-";
  private static final String ADDRESS_2 = "address2-";
  private static final String ADDRESS_3 = "address3-";
  private static final String ADDRESS_4 = "address4-";
  private static final String POST_CODE = "postCode-";
  private static final String PERSON_OWNER = "personOwner-";
  private static final String GENDER = "gender-";
  private static final LocalDate DATE = LocalDate.EPOCH;
  private static final String QUALIFICATION = "qualification-";
  private static final String MEDICAL_SCHOOL = "medicalSchool-";
  private static final String GMC_NUMBER = "gmcNumber-";
  private static final String GMC_STATUS = "gmcStatus-";
  private static final String GDC_NUMBER = "gdcNumber-";
  private static final String GDC_STATUS = "gdcStatus-";
  private static final String PUBLIC_HEALTH_NUMBER = "publicHealthNumber-";
  private static final String EEA_RESIDENT = "eeaResident-";
  private static final String PERMIT_TO_WORK = "permitToWork-";
  private static final String SETTLED = "settled-";
  private static final String DETAILS_NUMBER = "detailsNumber-";
  private static final String PREV_REVAL_BODY = "prevRevalBody-";
  private static final String TRAINEE_TIS_ID = "40";
  private static final String MODIFIED_SUFFIX = "post";
  private static final String ORIGINAL_SUFFIX = "pre";
  private static final int ONE_HUNDRED = 100;

  private PersonalDetailsService service;
  private TraineeProfileRepository repository;
  private EventPublishService eventService;

  @BeforeEach
  void setUp() {
    repository = mock(TraineeProfileRepository.class);
    eventService = mock(EventPublishService.class);
    service = new PersonalDetailsService(repository, new TraineeProfileMapperImpl(), eventService);
  }

  @Test
  void shouldCreateTraineeProfileWhenTraineeIdNotFound() {
    ArgumentCaptor<TraineeProfile> profileCaptor = ArgumentCaptor.forClass(TraineeProfile.class);

    when(repository.save(profileCaptor.capture()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PersonalDetails personalDetails = service.createProfileOrUpdateBasicDetailsByTisId("notFound",
        createPersonalDetails(MODIFIED_SUFFIX, 100));

    PersonalDetails expectedPersonalDetails = new PersonalDetails();
    expectedPersonalDetails.setPublicHealthNumber(PUBLIC_HEALTH_NUMBER + MODIFIED_SUFFIX);
    assertThat("Unexpected personal details.", personalDetails, is(expectedPersonalDetails));

    TraineeProfile traineeProfile = profileCaptor.getValue();
    assertThat("Unexpected id.", traineeProfile.getId(), nullValue());
    assertThat("Unexpected trainee TIS id.", traineeProfile.getTraineeTisId(), is("notFound"));
  }

  @Test
  void shouldPublishEventWhenTraineeProfileCreated() {
    TraineeProfile savedTraineeProfile = new TraineeProfile();
    savedTraineeProfile.setTraineeTisId(TRAINEE_TIS_ID);
    when(repository.save(any())).thenReturn(savedTraineeProfile);

    service.createProfileOrUpdateBasicDetailsByTisId("notFound",
        createPersonalDetails(MODIFIED_SUFFIX, 0));

    ArgumentCaptor<TraineeProfile> profileCaptor = ArgumentCaptor.forClass(TraineeProfile.class);
    verify(eventService).publishProfileCreateEvent(profileCaptor.capture());

    TraineeProfile eventTraineeProfile = profileCaptor.getValue();
    assertThat("Unexpected profile.", eventTraineeProfile, sameInstance(savedTraineeProfile));
  }

  @Test
  void shouldNotPublishEventWhenTraineeProfileUpdated() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPersonalDetails(createPersonalDetails(ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId("40")).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    service.createProfileOrUpdateBasicDetailsByTisId("40",
        createPersonalDetails(MODIFIED_SUFFIX, 100));

    verifyNoInteractions(eventService);
  }

  @Test
  void shouldUpdateBasicDetailsWhenTraineeIdFound() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPersonalDetails(createPersonalDetails(ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId("40")).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    PersonalDetails personalDetails = service.createProfileOrUpdateBasicDetailsByTisId("40",
        createPersonalDetails(MODIFIED_SUFFIX, 100));

    PersonalDetails expectedPersonalDetails = createPersonalDetails(ORIGINAL_SUFFIX, 0);
    expectedPersonalDetails.setPublicHealthNumber(PUBLIC_HEALTH_NUMBER + MODIFIED_SUFFIX);

    assertThat("Unexpected personal details.", personalDetails, is(expectedPersonalDetails));
  }

  @Test
  void shouldPopulateBasicDetailsWhenTraineeSkeletonFound() {
    TraineeProfile traineeProfile = new TraineeProfile();

    when(repository.findByTraineeTisId("40")).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    PersonalDetails personalDetails = service.createProfileOrUpdateBasicDetailsByTisId("40",
        createPersonalDetails(MODIFIED_SUFFIX, 100));

    PersonalDetails expectedPersonalDetails = new PersonalDetails();
    expectedPersonalDetails.setPublicHealthNumber(PUBLIC_HEALTH_NUMBER + MODIFIED_SUFFIX);

    assertThat("Unexpected personal details.", personalDetails, is(expectedPersonalDetails));
  }

  @Test
  void shouldNotUpdateGdcDetailsWhenTraineeIdNotFound() {
    Optional<PersonalDetails> personalDetails = service
        .updateGdcDetailsByTisId("notFound", new PersonalDetails());

    assertThat("Unexpected optional isEmpty flag.", personalDetails.isEmpty(), is(true));
    verify(repository).findByTraineeTisId("notFound");
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldUpdateGdcDetailsWhenTraineeIdFound() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPersonalDetails(createPersonalDetails(ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId("40")).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<PersonalDetails> personalDetails = service
        .updateGdcDetailsByTisId("40", createPersonalDetails(MODIFIED_SUFFIX, 100));

    assertThat("Unexpected optional isEmpty flag.", personalDetails.isEmpty(), is(false));

    PersonalDetails expectedPersonalDetails = createPersonalDetails(ORIGINAL_SUFFIX, 0);
    expectedPersonalDetails.setGdcNumber(GDC_NUMBER + MODIFIED_SUFFIX);
    expectedPersonalDetails.setGdcStatus(GDC_STATUS + MODIFIED_SUFFIX);

    assertThat("Unexpected personal details.", personalDetails.get(), is(expectedPersonalDetails));
  }

  @Test
  void shouldPopulateGdcDetailsWhenTraineeSkeletonFound() {
    TraineeProfile traineeProfile = new TraineeProfile();

    when(repository.findByTraineeTisId("40")).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<PersonalDetails> personalDetails = service
        .updateGdcDetailsByTisId("40", createPersonalDetails(MODIFIED_SUFFIX, 100));

    assertThat("Unexpected optional isEmpty flag.", personalDetails.isEmpty(), is(false));

    PersonalDetails expectedPersonalDetails = new PersonalDetails();
    expectedPersonalDetails.setGdcNumber(GDC_NUMBER + MODIFIED_SUFFIX);
    expectedPersonalDetails.setGdcStatus(GDC_STATUS + MODIFIED_SUFFIX);

    assertThat("Unexpected personal details.", personalDetails.get(), is(expectedPersonalDetails));
  }

  @Test
  void shouldNotUpdateGmcDetailsWhenTraineeIdNotFound() {
    Optional<PersonalDetails> personalDetails = service.updateGmcDetailsByTisId("notFound",
        new PersonalDetails());

    assertThat("Unexpected optional isEmpty flag.", personalDetails.isEmpty(), is(true));
    verify(repository).findByTraineeTisId("notFound");
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldUpdateGmcDetailsWhenTraineeIdFound() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPersonalDetails(createPersonalDetails(ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId("40")).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<PersonalDetails> personalDetails = service.updateGmcDetailsByTisId("40",
        createPersonalDetails(MODIFIED_SUFFIX, 100));

    assertThat("Unexpected optional isEmpty flag.", personalDetails.isEmpty(), is(false));

    PersonalDetails expectedPersonalDetails = createPersonalDetails(ORIGINAL_SUFFIX, 0);
    expectedPersonalDetails.setGmcNumber(GMC_NUMBER + MODIFIED_SUFFIX);
    expectedPersonalDetails.setGmcStatus(GMC_STATUS + MODIFIED_SUFFIX);

    assertThat("Unexpected personal details.", personalDetails.get(), is(expectedPersonalDetails));
  }

  @Test
  void shouldPopulateGmcDetailsWhenTraineeSkeletonFound() {
    TraineeProfile traineeProfile = new TraineeProfile();

    when(repository.findByTraineeTisId("40")).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<PersonalDetails> personalDetails = service.updateGmcDetailsByTisId("40",
        createPersonalDetails(MODIFIED_SUFFIX, 100));

    assertThat("Unexpected optional isEmpty flag.", personalDetails.isEmpty(), is(false));

    PersonalDetails expectedPersonalDetails = new PersonalDetails();
    expectedPersonalDetails.setGmcNumber(GMC_NUMBER + MODIFIED_SUFFIX);
    expectedPersonalDetails.setGmcStatus(GMC_STATUS + MODIFIED_SUFFIX);

    assertThat("Unexpected personal details.", personalDetails.get(), is(expectedPersonalDetails));
  }

  @Test
  void shouldNotPublishEventWhenGmcDetailsNotProvidedByTrainee() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPersonalDetails(createPersonalDetails(ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId("40")).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    service.updateGmcDetailsByTisId("40", createPersonalDetails(MODIFIED_SUFFIX, 100));

    verifyNoInteractions(eventService);
  }

  @Test
  void shouldPublishEventWhenGmcDetailsProvidedByTrainee() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPersonalDetails(createPersonalDetails(ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId("40")).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    GmcDetailsDto gmcDetails = GmcDetailsDto.builder()
        .gmcNumber(GMC_NUMBER + MODIFIED_SUFFIX)
        .gmcStatus(GMC_STATUS + MODIFIED_SUFFIX)
        .build();

    service.updateGmcDetailsWithTraineeProvidedDetails("40", gmcDetails);

    ArgumentCaptor<GmcDetailsDto> gmcDetailsCaptor = ArgumentCaptor.captor();
    verify(eventService).publishGmcDetailsProvidedEvent(eq("40"), gmcDetailsCaptor.capture());

    GmcDetailsDto eventDetails = gmcDetailsCaptor.getValue();
    assertThat("Unexpected GMC number.", eventDetails.gmcNumber(),
        is(GMC_NUMBER + MODIFIED_SUFFIX));
    assertThat("Unexpected GMC status.", eventDetails.gmcStatus(),
        is(GMC_STATUS + MODIFIED_SUFFIX));
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
    traineeProfile.setPersonalDetails(createPersonalDetails(ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId("40")).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<PersonalDetails> personalDetails = service
        .updateContactDetailsByTisId("40", createPersonalDetails(MODIFIED_SUFFIX, 100));

    assertThat("Unexpected optional isEmpty flag.", personalDetails.isEmpty(), is(false));

    PersonalDetails expectedPersonalDetails = createPersonalDetails(ORIGINAL_SUFFIX, 0);
    expectedPersonalDetails.setTitle(TITLE + MODIFIED_SUFFIX);
    expectedPersonalDetails.setForenames(FORENAMES + MODIFIED_SUFFIX);
    expectedPersonalDetails.setKnownAs(KNOWN_AS + MODIFIED_SUFFIX);
    expectedPersonalDetails.setSurname(SURNAME + MODIFIED_SUFFIX);
    expectedPersonalDetails.setMaidenName(MAIDEN_NAME + MODIFIED_SUFFIX);
    expectedPersonalDetails.setTelephoneNumber(TELEPHONE_NUMBER + MODIFIED_SUFFIX);
    expectedPersonalDetails.setMobileNumber(MOBILE_NUMBER + MODIFIED_SUFFIX);
    expectedPersonalDetails.setEmail(EMAIL + MODIFIED_SUFFIX);
    expectedPersonalDetails.setAddress1(ADDRESS_1 + MODIFIED_SUFFIX);
    expectedPersonalDetails.setAddress2(ADDRESS_2 + MODIFIED_SUFFIX);
    expectedPersonalDetails.setAddress3(ADDRESS_3 + MODIFIED_SUFFIX);
    expectedPersonalDetails.setAddress4(ADDRESS_4 + MODIFIED_SUFFIX);
    expectedPersonalDetails.setPostCode(POST_CODE + MODIFIED_SUFFIX);

    assertThat("Unexpected personal details.", personalDetails.get(), is(expectedPersonalDetails));
  }

  @Test
  void shouldPopulateContactDetailsWhenTraineeSkeletonFound() {
    TraineeProfile traineeProfile = new TraineeProfile();

    when(repository.findByTraineeTisId("40")).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<PersonalDetails> personalDetails = service
        .updateContactDetailsByTisId("40", createPersonalDetails(MODIFIED_SUFFIX, 100));

    assertThat("Unexpected optional isEmpty flag.", personalDetails.isEmpty(), is(false));

    PersonalDetails expectedPersonalDetails = new PersonalDetails();
    expectedPersonalDetails.setTitle(TITLE + MODIFIED_SUFFIX);
    expectedPersonalDetails.setForenames(FORENAMES + MODIFIED_SUFFIX);
    expectedPersonalDetails.setKnownAs(KNOWN_AS + MODIFIED_SUFFIX);
    expectedPersonalDetails.setSurname(SURNAME + MODIFIED_SUFFIX);
    expectedPersonalDetails.setMaidenName(MAIDEN_NAME + MODIFIED_SUFFIX);
    expectedPersonalDetails.setTelephoneNumber(TELEPHONE_NUMBER + MODIFIED_SUFFIX);
    expectedPersonalDetails.setMobileNumber(MOBILE_NUMBER + MODIFIED_SUFFIX);
    expectedPersonalDetails.setEmail(EMAIL + MODIFIED_SUFFIX);
    expectedPersonalDetails.setAddress1(ADDRESS_1 + MODIFIED_SUFFIX);
    expectedPersonalDetails.setAddress2(ADDRESS_2 + MODIFIED_SUFFIX);
    expectedPersonalDetails.setAddress3(ADDRESS_3 + MODIFIED_SUFFIX);
    expectedPersonalDetails.setAddress4(ADDRESS_4 + MODIFIED_SUFFIX);
    expectedPersonalDetails.setPostCode(POST_CODE + MODIFIED_SUFFIX);

    assertThat("Unexpected personal details.", personalDetails.get(), is(expectedPersonalDetails));
  }

  @Test
  void shouldNotUpdatePersonOwnerDetailsWhenTraineeIdNotFound() {
    Optional<PersonalDetails> personalDetails = service
        .updatePersonOwnerByTisId("notFound", new PersonalDetails());

    assertThat("Unexpected optional isEmpty flag.", personalDetails.isEmpty(), is(true));
    verify(repository).findByTraineeTisId("notFound");
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotUpdatePersonOwnerDetailsWhenTraineeIdFoundAndNewOwnerNull() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPersonalDetails(createPersonalDetails(ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId("40")).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    PersonalDetails newPersonalDetails = createPersonalDetails(MODIFIED_SUFFIX, 100);
    newPersonalDetails.setPersonOwner(null);

    Optional<PersonalDetails> personalDetails = service
        .updatePersonOwnerByTisId("40", newPersonalDetails);

    assertThat("Unexpected optional isEmpty flag.", personalDetails.isEmpty(), is(false));

    PersonalDetails expectedPersonalDetails = createPersonalDetails(ORIGINAL_SUFFIX, 0);
    expectedPersonalDetails.setPersonOwner(PERSON_OWNER + ORIGINAL_SUFFIX);

    assertThat("Unexpected personal details.", personalDetails.get(), is(expectedPersonalDetails));
  }

  @Test
  void shouldUpdatePersonOwnerDetailsWhenTraineeIdFoundAndNewOwnerNotNull() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPersonalDetails(createPersonalDetails(ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId("40")).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<PersonalDetails> personalDetails = service
        .updatePersonOwnerByTisId("40", createPersonalDetails(MODIFIED_SUFFIX, 100));

    assertThat("Unexpected optional isEmpty flag.", personalDetails.isEmpty(), is(false));

    PersonalDetails expectedPersonalDetails = createPersonalDetails(ORIGINAL_SUFFIX, 0);
    expectedPersonalDetails.setPersonOwner(PERSON_OWNER + MODIFIED_SUFFIX);

    assertThat("Unexpected personal details.", personalDetails.get(), is(expectedPersonalDetails));
  }

  @Test
  void shouldPopulatePersonOwnerDetailsWhenTraineeSkeletonFound() {
    TraineeProfile traineeProfile = new TraineeProfile();

    when(repository.findByTraineeTisId("40")).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<PersonalDetails> personalDetails = service
        .updatePersonOwnerByTisId("40", createPersonalDetails(MODIFIED_SUFFIX, 100));

    assertThat("Unexpected optional isEmpty flag.", personalDetails.isEmpty(), is(false));

    PersonalDetails expectedPersonalDetails = new PersonalDetails();
    expectedPersonalDetails.setPersonOwner(PERSON_OWNER + MODIFIED_SUFFIX);

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

  @Test
  void shouldPopulatePersonalInfoWhenTraineeSkeletonFound() {
    TraineeProfile traineeProfile = new TraineeProfile();

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<PersonalDetails> personalDetails = service
        .updatePersonalInfoByTisId(
            TRAINEE_TIS_ID, createPersonalDetails(MODIFIED_SUFFIX, ONE_HUNDRED));

    assertTrue(personalDetails.isPresent(), "Expected a PersonalDetails to be returned");

    PersonalDetails expectedPersonalDetails = new PersonalDetails();
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
