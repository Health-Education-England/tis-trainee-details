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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.trainee.details.dto.enumeration.GoldGuideVersion.GG10;
import static uk.nhs.hee.trainee.details.dto.enumeration.GoldGuideVersion.GG9;
import static uk.nhs.hee.trainee.details.service.TraineeProfileService.API_MOVE_ACTIONS;
import static uk.nhs.hee.trainee.details.service.TraineeProfileService.API_MOVE_FORMR;
import static uk.nhs.hee.trainee.details.service.TraineeProfileService.API_MOVE_LTFT;
import static uk.nhs.hee.trainee.details.service.TraineeProfileService.API_MOVE_NOTIFICATIONS;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.nhs.hee.trainee.details.dto.LocalOfficeContact;
import uk.nhs.hee.trainee.details.dto.UserDetails;
import uk.nhs.hee.trainee.details.dto.enumeration.Status;
import uk.nhs.hee.trainee.details.model.ConditionsOfJoining;
import uk.nhs.hee.trainee.details.model.Curriculum;
import uk.nhs.hee.trainee.details.model.LocalOfficeContactType;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.model.Placement;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.Qualification;
import uk.nhs.hee.trainee.details.model.Site;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;

@ExtendWith(MockitoExtension.class)
class TraineeProfileServiceTest {

  private static final String DEFAULT_ID_1 = "DEFAULT_ID_1";
  private static final String DEFAULT_TIS_ID_1 = "123";
  private static final String DEFAULT_ID_2 = "DEFAULT_ID_2";
  private static final String DEFAULT_TIS_ID_2 = "456";

  private static final String PERSON_SURNAME = "Gilliam";
  private static final String PERSON_FORENAME = "Anthony Mara";
  private static final String PERSON_KNOWNAS = "Ivy";
  private static final String PERSON_MAIDENNAME = "N/A";
  private static final String PERSON_TITLE = "Mr";
  private static final String PERSON_PERSONOWNER = "Thames Valley";
  private static final LocalDate PERSON_DATEOFBIRTH = LocalDate.parse("1991-11-11",
      DateTimeFormatter.ofPattern("yyyy-MM-dd"));
  private static final String PERSON_GENDER = "Male";
  private static final String PERSON_QUALIFICATION =
      "MBBS Bachelor of Medicine and Bachelor of Surgery";
  private static final LocalDate PERSON_DATEATTAINED = LocalDate.parse("2018-05-30",
      DateTimeFormatter.ofPattern("yyyy-MM-dd"));
  private static final String PERSON_MEDICALSCHOOL = "University of Science and Technology";
  private static final String PERSON_TELEPHONENUMBER = "01632960363";
  private static final String PERSON_MOBILE = "08465879348";
  private static final String PERSON_EMAIL = "email@email.com";
  private static final String PERSON_ADDRESS1 = "585-6360 Interdum Street";
  private static final String PERSON_ADDRESS2 = "Goulburn";
  private static final String PERSON_ADDRESS3 = "London";
  private static final String PERSON_ADDRESS4 = "UK";
  private static final String PERSON_POSTCODE = "SW1A1AA";
  private static final String PERSON_GMC = "1111111";

  private static final String PROGRAMME_MEMBERSHIP_TISID = UUID.randomUUID().toString();
  private static final String PROGRAMME_TISID = "1";
  private static final String PROGRAMME_NAME = "General Practice";
  private static final String PROGRAMME_NUMBER = "EOE8950";
  private static final String MANAGING_DEANERY = "West Midlands";
  private static final String MANAGING_DEANERY2 = "Thames Valley";

  private static final String CURRICULUM_TISID = "1";
  private static final String CURRICULUM_NAME = "ST3";
  private static final String CURRICULUM_SUBTYPE = "MEDICAL_CURRICULUM";

  private static final String PLACEMENT_TISID1 = "1";
  private static final String PLACEMENT_SITE1 = "Addenbrookes Hospital";
  private static final Status PLACEMENT_STATUS1 = Status.CURRENT;
  private static final String PLACEMENT_TISID2 = "2";
  private static final String PLACEMENT_SITE2 = "Addenbrookes Hospital";
  private static final Status PLACEMENT_STATUS2 = Status.PAST;

  private static final String LO_CONTACT1 = "gmc@local.office";
  private static final String LO_CONTACT2 = "ltft@local.office";

  @InjectMocks
  private TraineeProfileService service;

  @Mock
  private TraineeProfileRepository repository;

  @Mock
  private ProgrammeMembershipService programmeMembershipService;

  @Mock
  private TrainingNumberGenerator trainingNumberGenerator;

  @Mock
  private RestTemplate restTemplate;

  private TraineeProfile traineeProfile = new TraineeProfile();
  private TraineeProfile traineeProfile2 = new TraineeProfile();
  private PersonalDetails personalDetails;
  private ProgrammeMembership programmeMembership;
  private Curriculum curriculum;
  private Placement placement1;
  private Placement placement2;

  /**
   * Set up mocks before each test.
   */
  @BeforeEach
  void setupData() {
    setupPersonalDetailsData();
    setupCurriculumData();
    setupProgrammeMembershipsData();
    setupPlacementData();

    traineeProfile = new TraineeProfile();
    traineeProfile.setId(DEFAULT_ID_1);
    traineeProfile.setTraineeTisId(DEFAULT_TIS_ID_1);
    traineeProfile.setPersonalDetails(personalDetails);
    traineeProfile.setProgrammeMemberships(new ArrayList<>(List.of(programmeMembership)));
    traineeProfile.setPlacements(new ArrayList<>(List.of(placement1, placement2)));

    traineeProfile2 = new TraineeProfile();
    traineeProfile2.setId(DEFAULT_ID_2);
    traineeProfile2.setTraineeTisId(DEFAULT_TIS_ID_2);
    traineeProfile2.setPersonalDetails(personalDetails);
    traineeProfile2.setProgrammeMemberships(new ArrayList<>(List.of(programmeMembership)));
    traineeProfile2.setPlacements(new ArrayList<>(List.of(placement1, placement2)));
  }

  /**
   * Set up data for personalDetails.
   */
  void setupPersonalDetailsData() {
    personalDetails = new PersonalDetails();
    personalDetails.setSurname(PERSON_SURNAME);
    personalDetails.setForenames(PERSON_FORENAME);
    personalDetails.setKnownAs(PERSON_KNOWNAS);
    personalDetails.setMaidenName(PERSON_MAIDENNAME);
    personalDetails.setTitle(PERSON_TITLE);
    personalDetails.setPersonOwner(PERSON_PERSONOWNER);
    personalDetails.setDateOfBirth(PERSON_DATEOFBIRTH);
    personalDetails.setGender(PERSON_GENDER);
    personalDetails.setQualification(PERSON_QUALIFICATION);
    personalDetails.setDateAttained(PERSON_DATEATTAINED);
    personalDetails.setMedicalSchool(PERSON_MEDICALSCHOOL);
    personalDetails.setTelephoneNumber(PERSON_TELEPHONENUMBER);
    personalDetails.setMobileNumber(PERSON_MOBILE);
    personalDetails.setEmail(PERSON_EMAIL);
    personalDetails.setAddress1(PERSON_ADDRESS1);
    personalDetails.setAddress2(PERSON_ADDRESS2);
    personalDetails.setAddress3(PERSON_ADDRESS3);
    personalDetails.setAddress4(PERSON_ADDRESS4);
    personalDetails.setPostCode(PERSON_POSTCODE);
    personalDetails.setGmcNumber(PERSON_GMC);
  }

  /**
   * Set up data for programmeMembership.
   */
  void setupProgrammeMembershipsData() {
    programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId(PROGRAMME_MEMBERSHIP_TISID);
    programmeMembership.setProgrammeTisId(PROGRAMME_TISID);
    programmeMembership.setProgrammeName(PROGRAMME_NAME);
    programmeMembership.setProgrammeNumber(PROGRAMME_NUMBER);
    programmeMembership.setManagingDeanery(MANAGING_DEANERY);
    programmeMembership.setCurricula(List.of(curriculum));
  }

  /**
   * Set up data for curriculum.
   */
  void setupCurriculumData() {
    curriculum = new Curriculum();
    curriculum.setCurriculumTisId(CURRICULUM_TISID);
    curriculum.setCurriculumName(CURRICULUM_NAME);
    curriculum.setCurriculumSubType(CURRICULUM_SUBTYPE);
  }

  /**
   * Set up data for placement.
   */
  void setupPlacementData() {
    placement1 = new Placement();
    placement1.setTisId(PLACEMENT_TISID1);
    placement1.setStatus(PLACEMENT_STATUS1);

    Site site1 = new Site();
    site1.setName(PLACEMENT_SITE1);
    placement1.setSite(site1);

    placement2 = new Placement();
    placement2.setTisId(PLACEMENT_TISID2);
    placement2.setStatus(PLACEMENT_STATUS2);

    Site site2 = new Site();
    site2.setName(PLACEMENT_SITE2);
    placement2.setSite(site2);
  }

  @Test
  void getTraineeProfileByTraineeTisIdShouldReturnNullWhenNotFound() {
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(null);
    TraineeProfile returnedTraineeProfile = service
        .getTraineeProfileByTraineeTisId(DEFAULT_TIS_ID_1);
    assertThat(returnedTraineeProfile, nullValue());
  }

  @Test
  void getTraineeProfileByTraineeTisIdShouldReturnTraineeProfile() {
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(traineeProfile);
    TraineeProfile returnedTraineeProfile = service
        .getTraineeProfileByTraineeTisId(DEFAULT_TIS_ID_1);
    assertThat(returnedTraineeProfile, is(traineeProfile));
  }

  @Test
  void shouldSortQualificationsInDescendingOrder() {
    Qualification qualification1 = new Qualification();
    qualification1.setDateAttained(LocalDate.now());
    Qualification qualification2 = new Qualification();
    qualification2.setDateAttained(LocalDate.now().plusDays(100));
    Qualification qualification3 = new Qualification();
    qualification3.setDateAttained(LocalDate.now().minusDays(100));

    List<Qualification> qualifications = Arrays
        .asList(qualification1, qualification2, qualification3);
    traineeProfile = new TraineeProfile();
    traineeProfile.setQualifications(qualifications);

    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(traineeProfile);

    service.getTraineeProfileByTraineeTisId(DEFAULT_TIS_ID_1);

    assertThat("Unexpected qualification, check order is correct.",
        traineeProfile.getQualifications().get(0), is(qualification2));
    assertThat("Unexpected qualification, check order is correct.",
        traineeProfile.getQualifications().get(1), is(qualification1));
    assertThat("Unexpected qualification, check order is correct.",
        traineeProfile.getQualifications().get(2), is(qualification3));
  }

  @Test
  void shouldSortQualificationsWithNullsLast() {
    Qualification qualification1 = new Qualification();
    Qualification qualification2 = new Qualification();
    qualification2.setDateAttained(LocalDate.now().plusDays(100));
    Qualification qualification3 = new Qualification();
    qualification3.setDateAttained(LocalDate.now().minusDays(100));

    List<Qualification> qualifications = Arrays
        .asList(qualification1, qualification2, qualification3);
    traineeProfile = new TraineeProfile();
    traineeProfile.setQualifications(qualifications);

    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(traineeProfile);

    service.getTraineeProfileByTraineeTisId(DEFAULT_TIS_ID_1);

    assertThat("Unexpected qualification, check order is correct.",
        traineeProfile.getQualifications().get(0), is(qualification2));
    assertThat("Unexpected qualification, check order is correct.",
        traineeProfile.getQualifications().get(1), is(qualification3));
    assertThat("Unexpected qualification, check order is correct.",
        traineeProfile.getQualifications().get(2), is(qualification1));
  }

  @Test
  void shouldPopulatePersonalDetailsWithLatestQualification() {
    Qualification qualification1 = new Qualification();
    qualification1.setDateAttained(LocalDate.now());
    Qualification qualification2 = new Qualification();
    qualification2.setQualification("qualification2");
    qualification2.setDateAttained(LocalDate.now().plusDays(100));
    qualification2.setMedicalSchool("medicalSchool2");

    List<Qualification> qualifications = Arrays.asList(qualification1, qualification2);
    traineeProfile.setQualifications(qualifications);

    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(traineeProfile);

    service.getTraineeProfileByTraineeTisId(DEFAULT_TIS_ID_1);

    PersonalDetails personalDetails = traineeProfile.getPersonalDetails();
    assertThat("Unexpected qualification, check order is correct.",
        personalDetails.getQualification(), is("qualification2"));
    assertThat("Unexpected qualification, check order is correct.",
        personalDetails.getDateAttained(), is(LocalDate.now().plusDays(100)));
    assertThat("Unexpected qualification, check order is correct.",
        personalDetails.getMedicalSchool(), is("medicalSchool2"));
  }

  @Test
  void shouldUseLatestCojVersionWhenNoCoj() {
    programmeMembership.setConditionsOfJoining(null);

    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(this.traineeProfile);

    service.getTraineeProfileByTraineeTisId(DEFAULT_TIS_ID_1);

    ConditionsOfJoining coj = programmeMembership.getConditionsOfJoining();
    assertThat("Unexpected Conditions of Joining", coj, notNullValue());
    assertThat("Unexpected CoJ signed at timestamp", coj.signedAt(), nullValue());
    assertThat("Unexpected CoJ version", coj.version(), is(GG10));
  }

  @Test
  void shouldUseLatestCojVersionWhenCojNotSigned() {
    programmeMembership.setConditionsOfJoining(new ConditionsOfJoining(null, GG9, null));

    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(this.traineeProfile);

    service.getTraineeProfileByTraineeTisId(DEFAULT_TIS_ID_1);

    ConditionsOfJoining coj = programmeMembership.getConditionsOfJoining();
    assertThat("Unexpected Conditions of Joining", coj, notNullValue());
    assertThat("Unexpected CoJ signed at timestamp", coj.signedAt(), nullValue());
    assertThat("Unexpected CoJ version", coj.version(), is(GG10));
    assertThat("Unexpected CoJ synced at", coj.syncedAt(), nullValue());
  }

  @Test
  void shouldUseSignedVersionWhenCojSigned() {
    Instant now = Instant.now();
    programmeMembership.setConditionsOfJoining(new ConditionsOfJoining(now, GG9, null));

    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(this.traineeProfile);

    service.getTraineeProfileByTraineeTisId(DEFAULT_TIS_ID_1);

    ConditionsOfJoining coj = programmeMembership.getConditionsOfJoining();
    assertThat("Unexpected Conditions of Joining", coj, notNullValue());
    assertThat("Unexpected CoJ signed at timestamp", coj.signedAt(), is(now));
    assertThat("Unexpected CoJ version", coj.version(), is(GG9));
    assertThat("Unexpected CoJ synced at", coj.syncedAt(), nullValue());
  }

  @Test
  void shouldReturnMultipleTraineeIdsWhenMultipleValidProfileFoundByEmail() {
    when(repository.findAllByTraineeEmail(PERSON_EMAIL))
        .thenReturn(List.of(traineeProfile, traineeProfile2));

    List<String> traineeTisIds = service.getTraineeTisIdsByEmail(PERSON_EMAIL);

    assertThat("Unexpected number of trainee TIS IDs.", traineeTisIds.size(), is(2));
    assertThat("Unexpected trainee TIS IDs.", traineeTisIds,
        hasItems(DEFAULT_TIS_ID_1, DEFAULT_TIS_ID_2));
  }

  @Test
  void shouldReturnEmptyWhenProfileNotFoundByEmail() {
    when(repository.findAllByTraineeEmail(PERSON_EMAIL)).thenReturn(Collections.emptyList());

    List<String> traineeTisIds = service.getTraineeTisIdsByEmail(PERSON_EMAIL);

    assertThat("Unexpected number of trainee TIS IDs.", traineeTisIds.size(), is(0));
  }

  @Test
  void shouldFindProfilesByLowerCaseEmail() {
    ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
    when(repository.findAllByTraineeEmail(emailCaptor.capture()))
        .thenReturn(Collections.emptyList());

    service.getTraineeTisIdsByEmail("UPPER.lower@UpperCamel.lowerCamel");

    String email = emailCaptor.getValue();
    assertThat("Unexpected email.", email, is("upper.lower@uppercamel.lowercamel"));
  }

  @Test
  void shouldReturnSingleTraineeIdWhenSingleInvalidProfileFoundByEmail() {
    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(null);
    traineeProfile.setPersonalDetails(personalDetails);

    when(repository.findAllByTraineeEmail(PERSON_EMAIL))
        .thenReturn(List.of(traineeProfile));

    List<String> traineeTisIds = service.getTraineeTisIdsByEmail(PERSON_EMAIL);

    assertThat("Unexpected number of trainee TIS IDs.", traineeTisIds.size(), is(1));
    assertThat("Unexpected trainee TIS IDs.", traineeTisIds,
        hasItems(DEFAULT_TIS_ID_1));
  }

  @Test
  void shouldFilterOutNullGmcWhenMultipleProfileFoundByEmail() {
    PersonalDetails personalDetails2 = new PersonalDetails();
    personalDetails2.setGmcNumber(null);
    traineeProfile2.setPersonalDetails(personalDetails2);

    when(repository.findAllByTraineeEmail(PERSON_EMAIL))
        .thenReturn(List.of(traineeProfile, traineeProfile2));

    List<String> traineeTisIds = service.getTraineeTisIdsByEmail(PERSON_EMAIL);

    assertThat("Unexpected number of trainee TIS IDs.", traineeTisIds.size(), is(1));
    assertThat("Unexpected trainee TIS IDs.", traineeTisIds, hasItems(DEFAULT_TIS_ID_1));
  }

  @ParameterizedTest
  @ValueSource(strings = {"UNKNOWN", "Delete4", "N/A", "p123456", "L1234567", "123456"})
  void shouldFilterOutInvalidGmcWhenMultipleProfileFoundByEmail(String arg) {
    PersonalDetails personalDetails2 = new PersonalDetails();
    personalDetails2.setGmcNumber(arg);
    traineeProfile2.setPersonalDetails(personalDetails2);

    when(repository.findAllByTraineeEmail(PERSON_EMAIL))
        .thenReturn(List.of(traineeProfile, traineeProfile2));

    List<String> traineeTisIds = service.getTraineeTisIdsByEmail(PERSON_EMAIL);

    assertThat("Unexpected number of trainee TIS IDs.", traineeTisIds.size(), is(1));
    assertThat("Unexpected trainee TIS IDs.", traineeTisIds, hasItems(DEFAULT_TIS_ID_1));
  }

  @ParameterizedTest
  @ValueSource(strings = {"L123456", "1234567"})
  void shouldReturnValidGmcWhenMultipleProfileFoundByEmail(String arg) {
    PersonalDetails personalDetails2 = new PersonalDetails();
    personalDetails2.setGmcNumber(arg);
    traineeProfile2.setPersonalDetails(personalDetails2);

    when(repository.findAllByTraineeEmail(PERSON_EMAIL))
        .thenReturn(List.of(traineeProfile, traineeProfile2));

    List<String> traineeTisIds = service.getTraineeTisIdsByEmail(PERSON_EMAIL);

    assertThat("Unexpected number of trainee TIS IDs.", traineeTisIds.size(), is(2));
    assertThat("Unexpected trainee TIS IDs.", traineeTisIds,
        hasItems(DEFAULT_TIS_ID_1, DEFAULT_TIS_ID_2));
  }

  @Test
  void shouldReturnAllRecordWhenProfileFoundByEmailWithoutValidGmc() {
    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(null);
    personalDetails.setGdcNumber("UNKNOWN");
    traineeProfile.setPersonalDetails(personalDetails);

    PersonalDetails personalDetails2 = new PersonalDetails();
    personalDetails2.setGmcNumber(null);
    personalDetails2.setGdcNumber("111111");
    traineeProfile2.setPersonalDetails(personalDetails2);

    when(repository.findAllByTraineeEmail(PERSON_EMAIL))
        .thenReturn(List.of(traineeProfile, traineeProfile2));

    List<String> traineeTisIds = service.getTraineeTisIdsByEmail(PERSON_EMAIL);

    assertThat("Unexpected number of trainee TIS IDs.", traineeTisIds.size(), is(2));
    assertThat("Unexpected trainee TIS IDs.", traineeTisIds,
        hasItems(DEFAULT_TIS_ID_1, DEFAULT_TIS_ID_2));
  }

  @ParameterizedTest
  @ValueSource(strings = {"a GMC number", "a gmc number"})
  void shouldIgnoreGmcCaseWhenFindingProfilesByEmailGmcAndDob(String arg) {
    PersonalDetails personalDetails2 = new PersonalDetails();
    personalDetails2.setGmcNumber(arg);
    personalDetails2.setDateOfBirth(PERSON_DATEOFBIRTH);
    traineeProfile.setPersonalDetails(personalDetails2);

    PersonalDetails personalDetails3 = new PersonalDetails();
    personalDetails3.setGmcNumber(PERSON_GMC);
    personalDetails3.setDateOfBirth(PERSON_DATEOFBIRTH);
    traineeProfile2.setPersonalDetails(personalDetails3);

    when(repository.findAllByTraineeEmail(PERSON_EMAIL))
        .thenReturn(List.of(traineeProfile, traineeProfile2));

    List<String> traineeTisIds = service.getTraineeTisIdsByEmailGmcAndDob(PERSON_EMAIL,
        "a gmc Number", PERSON_DATEOFBIRTH);

    assertThat("Unexpected number of trainee TIS IDs.", traineeTisIds.size(), is(1));
    assertThat("Unexpected trainee TIS IDs.", traineeTisIds,
        hasItem(DEFAULT_TIS_ID_1));
  }

  @Test
  void shouldMatchDateWhenFindingProfilesByEmailGmcAndDob() {
    PersonalDetails personalDetails2 = new PersonalDetails();
    personalDetails2.setGmcNumber(PERSON_GMC);
    personalDetails2.setDateOfBirth(PERSON_DATEOFBIRTH);
    traineeProfile.setPersonalDetails(personalDetails2);

    PersonalDetails personalDetails3 = new PersonalDetails();
    personalDetails3.setGmcNumber(PERSON_GMC);
    personalDetails3.setDateOfBirth(LocalDate.MIN);
    traineeProfile2.setPersonalDetails(personalDetails3);

    when(repository.findAllByTraineeEmail(PERSON_EMAIL))
        .thenReturn(List.of(traineeProfile, traineeProfile2));

    List<String> traineeTisIds = service.getTraineeTisIdsByEmailGmcAndDob(PERSON_EMAIL,
        PERSON_GMC, PERSON_DATEOFBIRTH);

    assertThat("Unexpected number of trainee TIS IDs.", traineeTisIds.size(), is(1));
    assertThat("Unexpected trainee TIS IDs.", traineeTisIds,
        hasItem(DEFAULT_TIS_ID_1));
  }

  @Test
  void shouldDeleteProfileById() {
    service.deleteTraineeProfileByTraineeTisId("1");
    verify(repository).deleteByTraineeTisId("1");
  }

  @Test
  void shouldReturnEmptyWhenTraineeNotFoundByTisId() {
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(null);

    Optional<UserDetails> detail = service.getTraineeDetailsByTisId(DEFAULT_TIS_ID_1);

    assertThat("Unexpected trainee account details.", detail, is(Optional.empty()));
  }

  @Test
  void shouldReturnEmptyWhenPersonalDetailsNotFoundByTisId() {
    traineeProfile.setPersonalDetails(null);
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(traineeProfile);

    Optional<UserDetails> detail = service.getTraineeDetailsByTisId(DEFAULT_TIS_ID_1);

    assertThat("Unexpected trainee account details.", detail, is(Optional.empty()));
  }

  @Test
  void shouldFindAccountDetailsByTisId() {
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(traineeProfile);

    Optional<UserDetails> detail = service.getTraineeDetailsByTisId(DEFAULT_TIS_ID_1);

    assertThat("Unexpected missing account details.", detail.isPresent(), is(true));
    assertThat("Unexpected trainee email.", detail.get().email(), is(PERSON_EMAIL));
    assertThat("Unexpected trainee title.", detail.get().title(), is(PERSON_TITLE));
    assertThat("Unexpected trainee family name.", detail.get().familyName(),
        is(PERSON_SURNAME));
    assertThat("Unexpected trainee given name.", detail.get().givenName(),
        is(PERSON_FORENAME));
    assertThat("Unexpected trainee GMC number.", detail.get().gmcNumber(),
        is(PERSON_GMC));
  }

  @ParameterizedTest
  @EnumSource(LocalOfficeContactType.class)
  void shouldReturnEmptyLoContactsWhenTraineeNotFoundByTisId(LocalOfficeContactType contactType) {
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(null);

    Optional<Set<LocalOfficeContact>> loContacts
        = service.getTraineeLocalOfficeContacts(DEFAULT_TIS_ID_1, contactType);

    assertThat("Unexpected local office contact details.", loContacts, is(Optional.empty()));
  }

  @ParameterizedTest
  @EnumSource(LocalOfficeContactType.class)
  void shouldFindEmptySetOfLoContactsWhenNoPms(LocalOfficeContactType contactType) {
    traineeProfile.setProgrammeMemberships(new ArrayList<>());
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(traineeProfile);

    Optional<Set<LocalOfficeContact>> loContacts
        = service.getTraineeLocalOfficeContacts(DEFAULT_TIS_ID_1, contactType);

    assertThat("Unexpected missing LO contacts.", loContacts.isPresent(), is(true));
    Set<LocalOfficeContact> foundLoContacts = loContacts.get();
    assertThat("Unexpected local office contacts.", foundLoContacts.size(), is(0));
  }

  @ParameterizedTest
  @EnumSource(LocalOfficeContactType.class)
  void shouldFindLoContacts(LocalOfficeContactType contactType) {
    programmeMembership.setStartDate(LocalDate.MIN);
    programmeMembership.setEndDate(LocalDate.MAX);
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(traineeProfile);
    when(programmeMembershipService.getOwnerContact(
        MANAGING_DEANERY, contactType, null, null))
        .thenReturn(LO_CONTACT1);

    Optional<Set<LocalOfficeContact>> loContacts
        = service.getTraineeLocalOfficeContacts(DEFAULT_TIS_ID_1, contactType);

    assertThat("Unexpected missing LO contacts.", loContacts.isPresent(), is(true));
    Set<LocalOfficeContact> foundLoContacts = loContacts.get();
    assertThat("Unexpected local office contacts.", foundLoContacts.size(), is(1));
    LocalOfficeContact firstLo = foundLoContacts.stream().toList().get(0);
    assertThat("Unexpected local office contact.", firstLo.contact(), is(LO_CONTACT1));
    assertThat("Unexpected local office contact LO.", firstLo.localOffice(),
        is(MANAGING_DEANERY));
  }

  @ParameterizedTest
  @EnumSource(LocalOfficeContactType.class)
  void shouldNotFindLoContactsWithOtherContactType(LocalOfficeContactType contactType) {
    programmeMembership.setStartDate(LocalDate.MIN);
    programmeMembership.setEndDate(LocalDate.MAX);
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(traineeProfile);

    when(programmeMembershipService.getOwnerContact(
        MANAGING_DEANERY, contactType, null, null))
        .thenReturn(LO_CONTACT1);

    service.getTraineeLocalOfficeContacts(DEFAULT_TIS_ID_1, contactType);

    verify(programmeMembershipService)
        .getOwnerContact(MANAGING_DEANERY, contactType, null, null);
    verifyNoMoreInteractions(programmeMembershipService);
  }

  @ParameterizedTest
  @EnumSource(LocalOfficeContactType.class)
  void shouldFindDistinctMappedLoContacts(LocalOfficeContactType contactType) {
    programmeMembership.setStartDate(LocalDate.MIN);
    programmeMembership.setEndDate(LocalDate.MAX);
    ProgrammeMembership programmeMembership2 = new ProgrammeMembership();
    programmeMembership2.setStartDate(LocalDate.MIN);
    programmeMembership2.setEndDate(LocalDate.MAX);
    programmeMembership2.setManagingDeanery(MANAGING_DEANERY2);
    ProgrammeMembership programmeMembership3 = new ProgrammeMembership();
    programmeMembership3.setStartDate(LocalDate.MIN);
    programmeMembership3.setEndDate(LocalDate.MAX);
    programmeMembership3.setManagingDeanery(MANAGING_DEANERY);
    ProgrammeMembership programmeMembership4 = new ProgrammeMembership();
    programmeMembership4.setStartDate(LocalDate.MIN);
    programmeMembership4.setEndDate(LocalDate.MAX);
    //no deanery
    ProgrammeMembership programmeMembership5 = new ProgrammeMembership();
    programmeMembership5.setStartDate(LocalDate.MIN);
    programmeMembership5.setEndDate(LocalDate.MAX);
    programmeMembership5.setManagingDeanery("unknown deanery");

    traineeProfile.setProgrammeMemberships(new ArrayList<>(List.of(
        programmeMembership, programmeMembership2, programmeMembership3,
        programmeMembership4, programmeMembership5)));
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(traineeProfile);
    when(programmeMembershipService.getOwnerContact(
        MANAGING_DEANERY, contactType, null, null))
        .thenReturn(LO_CONTACT1);
    when(programmeMembershipService.getOwnerContact(
        MANAGING_DEANERY2, contactType, null, null))
        .thenReturn(LO_CONTACT2);

    Optional<Set<LocalOfficeContact>> loContacts
        = service.getTraineeLocalOfficeContacts(DEFAULT_TIS_ID_1, contactType);

    assertThat("Unexpected missing LO contacts.", loContacts.isPresent(), is(true));
    Set<LocalOfficeContact> foundLoContacts = loContacts.get();
    assertThat("Unexpected local office contacts.", foundLoContacts.size(), is(2));
    LocalOfficeContact firstLo = foundLoContacts.stream()
        .sorted(Comparator.comparing(LocalOfficeContact::localOffice))
        .toList().get(0);
    assertThat("Unexpected local office contact.", firstLo.contact(),
        is(LO_CONTACT2));
    assertThat("Unexpected local office localOffice.", firstLo.localOffice(),
        is(MANAGING_DEANERY2));
    LocalOfficeContact secondLo = foundLoContacts.stream()
        .sorted(Comparator.comparing(LocalOfficeContact::localOffice))
        .toList().get(1);
    assertThat("Unexpected local office contact.", secondLo.contact(),
        is(LO_CONTACT1));
    assertThat("Unexpected local office contact LO.", secondLo.localOffice(),
        is(MANAGING_DEANERY));
  }

  @ParameterizedTest
  @EnumSource(LocalOfficeContactType.class)
  void shouldFindLoContactsWithinDate(LocalOfficeContactType contactType) {
    programmeMembership.setStartDate(LocalDate.now());
    programmeMembership.setEndDate(LocalDate.now());
    ProgrammeMembership programmeMembership2 = new ProgrammeMembership();
    programmeMembership2.setStartDate(LocalDate.now().plusDays(1));
    programmeMembership2.setEndDate(LocalDate.MAX);
    programmeMembership2.setManagingDeanery(MANAGING_DEANERY2);
    ProgrammeMembership programmeMembership3 = new ProgrammeMembership();
    programmeMembership3.setStartDate(LocalDate.MIN);
    programmeMembership3.setEndDate(LocalDate.now().minusDays(1));
    programmeMembership3.setManagingDeanery(MANAGING_DEANERY2);

    traineeProfile.setProgrammeMemberships(new ArrayList<>(List.of(
        programmeMembership, programmeMembership2, programmeMembership3)));
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(traineeProfile);
    when(programmeMembershipService.getOwnerContact(
        MANAGING_DEANERY, contactType, null, null))
        .thenReturn(LO_CONTACT1);

    Optional<Set<LocalOfficeContact>> loContacts
        = service.getTraineeLocalOfficeContacts(DEFAULT_TIS_ID_1, contactType);

    verify(programmeMembershipService).getOwnerContact(eq(MANAGING_DEANERY), any(), any(), any());
    verifyNoMoreInteractions(programmeMembershipService); //only one filtered programme membership

    assertThat("Unexpected missing LO contacts.", loContacts.isPresent(), is(true));
    Set<LocalOfficeContact> foundLoContacts = loContacts.get();
    assertThat("Unexpected local office contacts.", foundLoContacts.size(), is(1));
    LocalOfficeContact firstLo = foundLoContacts.stream().toList().get(0);
    assertThat("Unexpected local office contact.", firstLo.contact(),
        is(LO_CONTACT1));
    assertThat("Unexpected local office contact LO.", firstLo.localOffice(),
        is(MANAGING_DEANERY));
  }

  @Test
  void moveShouldReturnEmptyMapWhenFromProfileNotFound() {
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(null);
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_2)).thenReturn(traineeProfile2);

    Map<String, Integer> result = service.moveData(DEFAULT_TIS_ID_1, DEFAULT_TIS_ID_2);

    assertThat("Unexpected number of moved items.", result.size(), is(0));
  }

  @Test
  void moveShouldReturnEmptyMapWhenToProfileNotFound() {
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(traineeProfile);
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_2)).thenReturn(null);

    Map<String, Integer> result = service.moveData(DEFAULT_TIS_ID_1, DEFAULT_TIS_ID_2);

    assertThat("Unexpected number of moved items.", result.size(), is(0));
  }

  @Test
  void moveShouldCombineResultsFromAllServices() {
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(traineeProfile);
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_2)).thenReturn(traineeProfile2);

    Map<String, String> expectedPathVars = Map.of(
        "fromTraineeId", DEFAULT_TIS_ID_1,
        "toTraineeId", DEFAULT_TIS_ID_2
    );

    when(restTemplate.exchange(eq(null + API_MOVE_LTFT),
        eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class),
        eq(expectedPathVars))).thenReturn(ResponseEntity.ok(Map.of("ltft", 1)));

    when(restTemplate.exchange(eq(null + API_MOVE_FORMR),
        eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class),
        eq(expectedPathVars))).thenReturn(ResponseEntity.ok(Map.of("formr-a", 2, "formr-b", 3)));

    when(restTemplate.exchange(eq(null + API_MOVE_NOTIFICATIONS),
        eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class),
        eq(expectedPathVars))).thenReturn(ResponseEntity.ok(Map.of("notification", 4)));

    when(restTemplate.exchange(eq(null + API_MOVE_ACTIONS),
        eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class),
        eq(expectedPathVars))).thenReturn(ResponseEntity.ok(Map.of("action", 5)));

    Map<String, Integer> result = service.moveData(DEFAULT_TIS_ID_1, DEFAULT_TIS_ID_2);

    assertThat("Unexpected number of moved items.", result.size(), is(5));
    assertThat("Unexpected LTFT items moved.", result.get("ltft"), is(1));
    assertThat("Unexpected FormR items moved.", result.get("formr-a"), is(2));
    assertThat("Unexpected FormR items moved.", result.get("formr-b"), is(3));
    assertThat("Unexpected notification items moved.", result.get("notification"), is(4));
    assertThat("Unexpected action items moved.", result.get("action"), is(5));
  }

  @Test
  void moveShouldHandleIndividualServiceErrors() {
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(traineeProfile);
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_2)).thenReturn(traineeProfile2);

    when(restTemplate.exchange(eq(null + API_MOVE_LTFT),
        eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class),
        anyMap())).thenThrow(new RestClientException("LTFT service error"));

    when(restTemplate.exchange(eq(null + API_MOVE_FORMR),
        eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class),
        anyMap())).thenReturn(ResponseEntity.ok(Map.of("formr-a", 1)));

    when(restTemplate.exchange(eq(null + API_MOVE_NOTIFICATIONS),
        eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class),
        anyMap())).thenReturn(ResponseEntity.ok(Map.of("notification", 2)));

    when(restTemplate.exchange(eq(null + API_MOVE_ACTIONS),
        eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class),
        anyMap())).thenReturn(ResponseEntity.ok(Map.of("action", 3)));

    Map<String, Integer> result = service.moveData(DEFAULT_TIS_ID_1, DEFAULT_TIS_ID_2);

    assertThat("Unexpected number of moved items.", result.size(), is(3));
    assertThat("Unexpected FormR items moved.", result.get("formr-a"), is(1));
    assertThat("Unexpected notification items moved.", result.get("notification"), is(2));
    assertThat("Unexpected action items moved.", result.get("action"), is(3));
  }

  @Test
  void moveShouldHandleAllServiceErrors() {
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(traineeProfile);
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_2)).thenReturn(traineeProfile2);

    when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(),
        any(ParameterizedTypeReference.class), anyMap()))
        .thenThrow(new RestClientException("Service error"));

    Map<String, Integer> result = service.moveData(DEFAULT_TIS_ID_1, DEFAULT_TIS_ID_2);

    assertThat("Unexpected number of moved items.", result.size(), is(0));
  }

  @Test
  void moveShouldKeepFirstValueIfDuplicateKeysFromDifferentServices() {
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(traineeProfile);
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_2)).thenReturn(traineeProfile2);

    Map<String, String> expectedPathVars = Map.of(
        "fromTraineeId", DEFAULT_TIS_ID_1,
        "toTraineeId", DEFAULT_TIS_ID_2
    );

    // Multiple services return results with the same key
    when(restTemplate.exchange(eq(null + API_MOVE_LTFT),
        eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class),
        eq(expectedPathVars))).thenReturn(ResponseEntity.ok(Map.of("records", 2)));

    when(restTemplate.exchange(eq(null + API_MOVE_FORMR),
        eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class),
        eq(expectedPathVars))).thenReturn(ResponseEntity.ok(Map.of("records", 99)));

    when(restTemplate.exchange(eq(null + API_MOVE_NOTIFICATIONS),
        eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class),
        eq(expectedPathVars))).thenReturn(ResponseEntity.ok(Map.of("records", -1)));

    when(restTemplate.exchange(eq(null + API_MOVE_ACTIONS),
        eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class),
        eq(expectedPathVars))).thenReturn(ResponseEntity.ok(Map.of("records", 111)));

    Map<String, Integer> result = service.moveData(DEFAULT_TIS_ID_1, DEFAULT_TIS_ID_2);

    assertThat("Unexpected number of result types.", result.size(), is(1));
    assertThat("Unexpected result map.", result.get("records"), is(2));
  }

  @Test
  void moveShouldTriggerFuturesInParallelAndWaitForAllToComplete() throws InterruptedException {
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(traineeProfile);
    when(repository.findByTraineeTisId(DEFAULT_TIS_ID_2)).thenReturn(traineeProfile2);

    CountDownLatch startLatch = new CountDownLatch(4); // Track when futures start
    CountDownLatch completeLatch = new CountDownLatch(1); // Signal futures to complete

    AtomicBoolean ltftStarted = new AtomicBoolean(false);
    AtomicBoolean formrStarted = new AtomicBoolean(false);
    AtomicBoolean notificationsStarted = new AtomicBoolean(false);
    AtomicBoolean actionsStarted = new AtomicBoolean(false);

    Map<String, String> expectedPathVars = Map.of(
        "fromTraineeId", DEFAULT_TIS_ID_1,
        "toTraineeId", DEFAULT_TIS_ID_2);

    when(restTemplate.exchange(eq(null + API_MOVE_LTFT), eq(HttpMethod.GET), isNull(),
        any(ParameterizedTypeReference.class), eq(expectedPathVars)))
        .thenAnswer(invocation -> {
          ltftStarted.set(true);
          startLatch.countDown();
          completeLatch.await(1, SECONDS); // Wait for signal to complete
          return ResponseEntity.ok(Map.of("ltft", 1));
        });

    when(restTemplate.exchange(eq(null + API_MOVE_FORMR), eq(HttpMethod.GET), isNull(),
        any(ParameterizedTypeReference.class), eq(expectedPathVars)))
        .thenAnswer(invocation -> {
          formrStarted.set(true);
          startLatch.countDown();
          completeLatch.await(1, SECONDS);
          return ResponseEntity.ok(Map.of("formr", 2));
        });

    when(restTemplate.exchange(eq(null + API_MOVE_NOTIFICATIONS), eq(HttpMethod.GET), isNull(),
        any(ParameterizedTypeReference.class), eq(expectedPathVars)))
        .thenAnswer(invocation -> {
          notificationsStarted.set(true);
          startLatch.countDown();
          completeLatch.await(1, SECONDS);
          return ResponseEntity.ok(Map.of("notification", 3));
        });

    when(restTemplate.exchange(eq(null + API_MOVE_ACTIONS), eq(HttpMethod.GET), isNull(),
        any(ParameterizedTypeReference.class), eq(expectedPathVars)))
        .thenAnswer(invocation -> {
          actionsStarted.set(true);
          startLatch.countDown();
          completeLatch.await(1, SECONDS);
          return ResponseEntity.ok(Map.of("action", 4));
        });

    // Start the move operation in a separate thread
    CompletableFuture<Map<String, Integer>> future = CompletableFuture.supplyAsync(
        () -> service.moveData(DEFAULT_TIS_ID_1, DEFAULT_TIS_ID_2));

    // Wait for all futures to start
    assertThat("Not all futures started", startLatch.await(2, SECONDS), is(true));

    // Verify all futures started before any completed
    assertThat("LTFT future did not start", ltftStarted.get(), is(true));
    assertThat("FormR future did not start", formrStarted.get(), is(true));
    assertThat("Notifications future did not start", notificationsStarted.get(), is(true));
    assertThat("Actions future did not start", actionsStarted.get(), is(true));

    // Allow futures to complete
    completeLatch.countDown();

    Map<String, Integer> result = future.join();

    assertThat("Unexpected number of moved items", result.size(), is(4));
    assertThat("Missing LTFT result", result.get("ltft"), is(1));
    assertThat("Missing FormR result", result.get("formr"), is(2));
    assertThat("Missing notification result", result.get("notification"), is(3));
    assertThat("Missing action result", result.get("action"), is(4));
  }
}
