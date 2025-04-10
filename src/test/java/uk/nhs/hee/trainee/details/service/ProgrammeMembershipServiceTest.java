/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.trainee.details.model.HrefType.ABSOLUTE_URL;
import static uk.nhs.hee.trainee.details.model.HrefType.NON_HREF;
import static uk.nhs.hee.trainee.details.model.HrefType.PROTOCOL_EMAIL;
import static uk.nhs.hee.trainee.details.service.ProgrammeMembershipService.CONTACT_FIELD;
import static uk.nhs.hee.trainee.details.service.ProgrammeMembershipService.CONTACT_TYPE_FIELD;
import static uk.nhs.hee.trainee.details.service.ProgrammeMembershipService.DEFAULT_NO_CONTACT_MESSAGE;
import static uk.nhs.hee.trainee.details.service.ProgrammeMembershipService.MEDICAL_CURRICULA;
import static uk.nhs.hee.trainee.details.service.ProgrammeMembershipService.NON_RELEVANT_PROGRAMME_MEMBERSHIP_TYPES;
import static uk.nhs.hee.trainee.details.service.ProgrammeMembershipService.NOT_TSS_SPECIALTIES;
import static uk.nhs.hee.trainee.details.service.ProgrammeMembershipService.PILOT_2024_LOCAL_OFFICES_ALL_PROGRAMMES;
import static uk.nhs.hee.trainee.details.service.ProgrammeMembershipService.PILOT_2024_NW_SPECIALTIES;
import static uk.nhs.hee.trainee.details.service.ProgrammeMembershipService.PILOT_2024_ROLLOUT_LOCAL_OFFICES;
import static uk.nhs.hee.trainee.details.service.ProgrammeMembershipService.PM_CONFIRM_WEEKS;
import static uk.nhs.hee.trainee.details.service.ProgrammeMembershipService.PROGRAMME_BREAK_DAYS;
import static uk.nhs.hee.trainee.details.service.ProgrammeMembershipService.TSS_CURRICULA;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.nhs.hee.trainee.details.dto.enumeration.GoldGuideVersion;
import uk.nhs.hee.trainee.details.mapper.ProgrammeMembershipMapperImpl;
import uk.nhs.hee.trainee.details.model.ConditionsOfJoining;
import uk.nhs.hee.trainee.details.model.Curriculum;
import uk.nhs.hee.trainee.details.model.HeeUser;
import uk.nhs.hee.trainee.details.model.LocalOfficeContactType;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;

class ProgrammeMembershipServiceTest {

  private static final String REFERENCE_URL = "http://localhost/8205/reference";
  private static final String TEMPLATE_VERSION = "v1.0.0";
  private static final LocalDate START_DATE = LocalDate.now();
  private static final LocalDate END_DATE = START_DATE.plusYears(1);
  private static final LocalDate COMPLETION_DATE = END_DATE.plusYears(1);
  private static final String PROGRAMME_TIS_ID = "programmeTisId-";
  private static final String PROGRAMME_NAME = "programmeName-";
  private static final String PROGRAMME_NUMBER = "programmeNumber-";
  private static final String MANAGING_DEANERY = "managingDeanery-";
  private static final String DESIGNATED_BODY = "designatedBody-";
  private static final String DESIGNATED_BODY_CODE = "designatedBodyCode-";
  private static final String PROGRAMME_MEMBERSHIP_TYPE = "programmeMembershipType-";
  private static final String TRAINEE_TIS_ID = "40";
  private static final String MODIFIED_SUFFIX = "post";
  private static final String ORIGINAL_SUFFIX = "pre";
  private static final String NEW_PROGRAMME_MEMBERSHIP_UUID = UUID.randomUUID().toString();
  private static final String EXISTING_PROGRAMME_MEMBERSHIP_UUID = UUID.randomUUID().toString();
  private static final String DIFFERENT_PROGRAMME_MEMBERSHIP_UUID = UUID.randomUUID().toString();
  private static final Instant COJ_SIGNED_AT = Instant.now();
  private static final GoldGuideVersion GOLD_GUIDE_VERSION = GoldGuideVersion.GG9;
  private static final Instant COJ_SYNCED_AT = Instant.now();
  private static final String CURRICULUM_SPECIALTY_CODE = "X75";
  private static final String CURRICULUM_SPECIALTY = "some valid specialty";
  private static final String RO_FIRSTNAME = "first name-";
  private static final String RO_LASTNAME = "last name-";
  private static final String RO_EMAIL = "email-";
  private static final String RO_PHONE = "phone-";
  private static final String RO_GMC = "gmc-";
  private static final String FORENAMES = "forenames-";
  private static final String SURNAME = "surname-";
  private static final String GMC_NUMBER = "gmcNumber-";
  private static final String OWNER_CONTACT = "ownerContact-";

  private ProgrammeMembershipService service;
  private TraineeProfileRepository repository;
  private CachingDelegate cachingDelegate;
  private PdfGeneratingService pdfService;
  private RestTemplate restTemplate;

  @BeforeEach
  void setUp() {
    repository = mock(TraineeProfileRepository.class);
    cachingDelegate = mock(CachingDelegate.class);
    pdfService = mock(PdfGeneratingService.class);
    restTemplate = mock(RestTemplate.class);
    service = new ProgrammeMembershipService(repository, new ProgrammeMembershipMapperImpl(),
        cachingDelegate, pdfService, restTemplate, REFERENCE_URL, TEMPLATE_VERSION);
  }

  @Test
  void shouldNotUpdateProgrammeMembershipWhenTraineeIdNotFound() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId(EXISTING_PROGRAMME_MEMBERSHIP_UUID);
    Optional<ProgrammeMembership> updatedProgrammeMembership = service
        .updateProgrammeMembershipForTrainee("notFound", programmeMembership);

    assertThat("Unexpected optional isEmpty flag.", updatedProgrammeMembership.isEmpty(), is(true));
    verify(repository).findByTraineeTisId("notFound");
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldAddProgrammeMembershipWhenTraineeFoundAndNoProgrammeMembershipsExists() {
    TraineeProfile traineeProfile = new TraineeProfile();

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    Optional<ProgrammeMembership> programmeMembership = service
        .updateProgrammeMembershipForTrainee(TRAINEE_TIS_ID,
            createProgrammeMembership(NEW_PROGRAMME_MEMBERSHIP_UUID, MODIFIED_SUFFIX, 100));

    assertThat("Unexpected optional isEmpty flag.", programmeMembership.isEmpty(), is(false));

    ProgrammeMembership expectedProgrammeMembership = new ProgrammeMembership();
    expectedProgrammeMembership.setTisId(NEW_PROGRAMME_MEMBERSHIP_UUID);
    expectedProgrammeMembership.setProgrammeTisId(PROGRAMME_TIS_ID + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setProgrammeName(PROGRAMME_NAME + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setProgrammeNumber(PROGRAMME_NUMBER + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setManagingDeanery(MANAGING_DEANERY + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setDesignatedBody(DESIGNATED_BODY + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setDesignatedBodyCode(DESIGNATED_BODY_CODE + MODIFIED_SUFFIX);
    expectedProgrammeMembership
        .setProgrammeMembershipType(PROGRAMME_MEMBERSHIP_TYPE + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setStartDate(START_DATE.plusDays(100));
    expectedProgrammeMembership.setEndDate(END_DATE.plusDays(100));
    expectedProgrammeMembership.setProgrammeCompletionDate(COMPLETION_DATE.plusDays(100));
    expectedProgrammeMembership.setConditionsOfJoining(
        new ConditionsOfJoining(COJ_SIGNED_AT.plus(Duration.ofDays(100)), GOLD_GUIDE_VERSION,
            COJ_SYNCED_AT.plus(Duration.ofDays(100))));
    expectedProgrammeMembership.setResponsibleOfficer(getResponsibleOfficerUser(MODIFIED_SUFFIX));

    assertThat("Unexpected programme membership.", programmeMembership.get(),
        is(expectedProgrammeMembership));
  }

  @Test
  void shouldAddProgrammeMembershipWhenTraineeFoundAndProgrammeMembershipNotExists() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_UUID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    Optional<ProgrammeMembership> programmeMembership = service
        .updateProgrammeMembershipForTrainee(TRAINEE_TIS_ID,
            createProgrammeMembership(NEW_PROGRAMME_MEMBERSHIP_UUID, MODIFIED_SUFFIX, 100));

    assertThat("Unexpected optional isEmpty flag.", programmeMembership.isEmpty(), is(false));

    ProgrammeMembership expectedProgrammeMembership = new ProgrammeMembership();
    expectedProgrammeMembership.setTisId(NEW_PROGRAMME_MEMBERSHIP_UUID);
    expectedProgrammeMembership.setProgrammeTisId(PROGRAMME_TIS_ID + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setProgrammeName(PROGRAMME_NAME + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setProgrammeNumber(PROGRAMME_NUMBER + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setManagingDeanery(MANAGING_DEANERY + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setDesignatedBody(DESIGNATED_BODY + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setDesignatedBodyCode(DESIGNATED_BODY_CODE + MODIFIED_SUFFIX);
    expectedProgrammeMembership
        .setProgrammeMembershipType(PROGRAMME_MEMBERSHIP_TYPE + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setStartDate(START_DATE.plusDays(100));
    expectedProgrammeMembership.setEndDate(END_DATE.plusDays(100));
    expectedProgrammeMembership.setProgrammeCompletionDate(COMPLETION_DATE.plusDays(100));
    expectedProgrammeMembership.setConditionsOfJoining(
        new ConditionsOfJoining(COJ_SIGNED_AT.plus(Duration.ofDays(100)), GOLD_GUIDE_VERSION,
            COJ_SYNCED_AT.plus(Duration.ofDays(100))));
    expectedProgrammeMembership.setResponsibleOfficer(getResponsibleOfficerUser(MODIFIED_SUFFIX));

    assertThat("Unexpected programme membership.", programmeMembership.get(),
        is(expectedProgrammeMembership));
  }

  @Test
  void shouldUpdateProgrammeMembershipWhenTraineeFoundAndProgrammeMembershipExists() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_UUID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    Optional<ProgrammeMembership> programmeMembership = service
        .updateProgrammeMembershipForTrainee(TRAINEE_TIS_ID,
            createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_UUID, MODIFIED_SUFFIX, 100));

    assertThat("Unexpected optional isEmpty flag.", programmeMembership.isEmpty(), is(false));

    ProgrammeMembership expectedProgrammeMembership = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_UUID,
        ORIGINAL_SUFFIX, 0);
    expectedProgrammeMembership.setTisId(EXISTING_PROGRAMME_MEMBERSHIP_UUID);
    expectedProgrammeMembership.setProgrammeTisId(PROGRAMME_TIS_ID + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setProgrammeName(PROGRAMME_NAME + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setProgrammeNumber(PROGRAMME_NUMBER + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setManagingDeanery(MANAGING_DEANERY + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setDesignatedBody(DESIGNATED_BODY + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setDesignatedBodyCode(DESIGNATED_BODY_CODE + MODIFIED_SUFFIX);
    expectedProgrammeMembership
        .setProgrammeMembershipType(PROGRAMME_MEMBERSHIP_TYPE + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setStartDate(START_DATE.plusDays(100));
    expectedProgrammeMembership.setEndDate(END_DATE.plusDays(100));
    expectedProgrammeMembership.setProgrammeCompletionDate(COMPLETION_DATE.plusDays(100));
    expectedProgrammeMembership.setConditionsOfJoining(
        new ConditionsOfJoining(COJ_SIGNED_AT.plus(Duration.ofDays(100)), GOLD_GUIDE_VERSION,
            COJ_SYNCED_AT.plus(Duration.ofDays(100))));
    expectedProgrammeMembership.setResponsibleOfficer(getResponsibleOfficerUser(MODIFIED_SUFFIX));

    assertThat("Unexpected programme membership.", programmeMembership.get(),
        is(expectedProgrammeMembership));
  }

  @Test
  void shouldNotUpdateProgrammeMembershipCojWhenNewCojNull() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_UUID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    ProgrammeMembership newProgrammeMembership = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_UUID, MODIFIED_SUFFIX, 100);
    newProgrammeMembership.setConditionsOfJoining(null);

    Optional<ProgrammeMembership> programmeMembership = service
        .updateProgrammeMembershipForTrainee(TRAINEE_TIS_ID, newProgrammeMembership);

    assertThat("Unexpected optional isEmpty flag.", programmeMembership.isEmpty(), is(false));
    ProgrammeMembership updatedProgrammeMembership = programmeMembership.get();
    assertThat("Unexpected conditions of joining.",
        updatedProgrammeMembership.getConditionsOfJoining(), notNullValue());

    ConditionsOfJoining conditionsOfJoining = updatedProgrammeMembership.getConditionsOfJoining();
    assertThat("Unexpected signed at.", conditionsOfJoining.signedAt(), is(COJ_SIGNED_AT));
    assertThat("Unexpected signed version.", conditionsOfJoining.version(),
        is(GoldGuideVersion.GG9));
    assertThat("Unexpected synced at.", conditionsOfJoining.syncedAt(), is(COJ_SYNCED_AT));
  }

  @Test
  void shouldNotUpdateProgrammeMembershipCojWhenSavedPmNotFound() {
    ProgrammeMembership programmeMembership = createProgrammeMembership(
        DIFFERENT_PROGRAMME_MEMBERSHIP_UUID, ORIGINAL_SUFFIX, 100);
    programmeMembership.setConditionsOfJoining(null);

    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships().add(createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_UUID, MODIFIED_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    Optional<ProgrammeMembership> optionalProgrammeMembership = service
        .updateProgrammeMembershipForTrainee(TRAINEE_TIS_ID, programmeMembership);

    assertThat("Unexpected optional isEmpty flag.", optionalProgrammeMembership.isEmpty(),
        is(false));
    ProgrammeMembership updatedProgrammeMembership = optionalProgrammeMembership.get();
    assertThat("Unexpected conditions of joining.",
        updatedProgrammeMembership.getConditionsOfJoining(), nullValue());
  }

  @Test
  void shouldNotUpdateProgrammeMembershipCojWhenNoSavedCoj() {
    ProgrammeMembership programmeMembership = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_UUID, ORIGINAL_SUFFIX, 100);
    programmeMembership.setConditionsOfJoining(new ConditionsOfJoining(
        null, GOLD_GUIDE_VERSION, null));

    ProgrammeMembership savedProgrammeMembership = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_UUID, MODIFIED_SUFFIX, 0);
    savedProgrammeMembership.setConditionsOfJoining(null);
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships().add(savedProgrammeMembership);

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    Optional<ProgrammeMembership> optionalProgrammeMembership = service
        .updateProgrammeMembershipForTrainee(TRAINEE_TIS_ID, programmeMembership);

    assertThat("Unexpected optional isEmpty flag.", optionalProgrammeMembership.isEmpty(),
        is(false));
    ProgrammeMembership updatedProgrammeMembership = optionalProgrammeMembership.get();
    assertThat("Unexpected conditions of joining.",
        updatedProgrammeMembership.getConditionsOfJoining(), notNullValue());
  }

  @Test
  void shouldNotUpdateProgrammeMembershipCojWhenNewCojNotSigned() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships().add(createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_UUID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    ProgrammeMembership newProgrammeMembership = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_UUID, MODIFIED_SUFFIX, 100);
    ConditionsOfJoining newConditionsOfJoining
        = new ConditionsOfJoining(null, GoldGuideVersion.GG10, null);
    newProgrammeMembership.setConditionsOfJoining(newConditionsOfJoining);

    Optional<ProgrammeMembership> programmeMembership = service
        .updateProgrammeMembershipForTrainee(TRAINEE_TIS_ID, newProgrammeMembership);

    assertThat("Unexpected optional isEmpty flag.", programmeMembership.isEmpty(), is(false));
    ProgrammeMembership updatedProgrammeMembership = programmeMembership.get();
    assertThat("Unexpected conditions of joining.",
        updatedProgrammeMembership.getConditionsOfJoining(), notNullValue());

    ConditionsOfJoining conditionsOfJoining = updatedProgrammeMembership.getConditionsOfJoining();
    assertThat("Unexpected signed at.", conditionsOfJoining.signedAt(),
        is(COJ_SIGNED_AT));
    assertThat("Unexpected synced at.", conditionsOfJoining.syncedAt(), is(COJ_SYNCED_AT));
  }

  @Test
  void shouldUpdateProgrammeMembershipCojWhenNewCojSigned() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships().add(createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_UUID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    ProgrammeMembership newProgrammeMembership = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_UUID, MODIFIED_SUFFIX, 100);

    Optional<ProgrammeMembership> programmeMembership = service
        .updateProgrammeMembershipForTrainee(TRAINEE_TIS_ID, newProgrammeMembership);

    assertThat("Unexpected optional isEmpty flag.", programmeMembership.isEmpty(), is(false));
    ProgrammeMembership updatedProgrammeMembership = programmeMembership.get();
    assertThat("Unexpected conditions of joining.",
        updatedProgrammeMembership.getConditionsOfJoining(), notNullValue());

    ConditionsOfJoining conditionsOfJoining = updatedProgrammeMembership.getConditionsOfJoining();
    assertThat("Unexpected signed at.", conditionsOfJoining.signedAt(),
        is(COJ_SIGNED_AT.plus(Duration.ofDays(100))));
    assertThat("Unexpected signed version.", conditionsOfJoining.version(),
        is(GoldGuideVersion.GG9));
  }

  @Test
  void shouldDeleteProgrammeMembershipsWhenTraineeFoundAndProgrammeMembershipsExist() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_UUID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean result = service.deleteProgrammeMembershipsForTrainee(TRAINEE_TIS_ID);

    assertThat("Unexpected result.", result, is(true));
  }

  @Test
  void shouldNotDeleteProgrammeMembershipsWhenTraineeNotFound() {
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(null);

    boolean result = service.deleteProgrammeMembershipsForTrainee(TRAINEE_TIS_ID);

    assertThat("Unexpected result.", result, is(false));
  }

  @Test
  void shouldCacheCojFromDeleteProgrammeMembershipsWhenCojSigned() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_UUID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    service.deleteProgrammeMembershipsForTrainee(TRAINEE_TIS_ID);

    verify(cachingDelegate).cacheConditionsOfJoining(eq(EXISTING_PROGRAMME_MEMBERSHIP_UUID),
        any());
  }

  @Test
  void shouldNotCacheCojFromDeleteProgrammeMembershipsWhenCojNotSigned() {
    ProgrammeMembership programmeMembership = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_UUID, ORIGINAL_SUFFIX, 0);
    programmeMembership.setConditionsOfJoining(
        new ConditionsOfJoining(null, GoldGuideVersion.GG10, null));

    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships().add(programmeMembership);

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    service.deleteProgrammeMembershipsForTrainee(TRAINEE_TIS_ID);

    verifyNoInteractions(cachingDelegate);
  }

  @Test
  void shouldNotCacheCojFromDeleteProgrammeMembershipsWhenCojNull() {
    ProgrammeMembership programmeMembership = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_UUID, ORIGINAL_SUFFIX, 0);
    programmeMembership.setConditionsOfJoining(null);

    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships().add(programmeMembership);

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    service.deleteProgrammeMembershipsForTrainee(TRAINEE_TIS_ID);

    verifyNoInteractions(cachingDelegate);
  }

  @Test
  void shouldNotDeleteProgrammeMembershipWhenTraineeNotFound() {
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(null);

    boolean deleted = service.deleteProgrammeMembershipForTrainee(TRAINEE_TIS_ID,
        NEW_PROGRAMME_MEMBERSHIP_UUID);

    assertThat("Unexpected result.", deleted, is(false));
    verify(repository, never()).save(any());
  }

  @Test
  void shouldNotDeleteProgrammeMembershipWhenTraineesProgrammeMembershipNotFound() {
    ProgrammeMembership programmeMembership = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_UUID, ORIGINAL_SUFFIX, 0);

    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships().add(programmeMembership);

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean deleted = service.deleteProgrammeMembershipForTrainee(TRAINEE_TIS_ID,
        DIFFERENT_PROGRAMME_MEMBERSHIP_UUID);

    assertThat("Unexpected result.", deleted, is(false));
    verify(repository, never()).save(any());
  }

  @Test
  void shouldDeleteProgrammeMembershipWhenTraineeFoundAndProgrammeMembershipExists() {
    ProgrammeMembership programmeMembership1 = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_UUID, ORIGINAL_SUFFIX, 0);
    ProgrammeMembership programmeMembership2 = createProgrammeMembership(
        DIFFERENT_PROGRAMME_MEMBERSHIP_UUID, "unrelatedPm", 1);

    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .addAll(List.of(programmeMembership1, programmeMembership2));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean deleted = service.deleteProgrammeMembershipForTrainee(TRAINEE_TIS_ID,
        EXISTING_PROGRAMME_MEMBERSHIP_UUID);

    assertThat("Unexpected result.", deleted, is(true));

    ArgumentCaptor<TraineeProfile> profileCaptor = ArgumentCaptor.forClass(TraineeProfile.class);
    verify(repository).save(profileCaptor.capture());

    List<ProgrammeMembership> programmeMemberships = profileCaptor.getValue()
        .getProgrammeMemberships();
    assertThat("Unexpected programme membership count.", programmeMemberships.size(), is(1));

    ProgrammeMembership remainingProgrammeMembership = programmeMemberships.get(0);
    assertThat("Unexpected programme membership id.", remainingProgrammeMembership.getTisId(),
        is(DIFFERENT_PROGRAMME_MEMBERSHIP_UUID));
  }

  @Test
  void shouldSignCojWhenTraineeProgrammeMembershipFound() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_UUID, ORIGINAL_SUFFIX, 0));
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(DIFFERENT_PROGRAMME_MEMBERSHIP_UUID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<ProgrammeMembership> programmeMembership = service
        .signProgrammeMembershipCoj(TRAINEE_TIS_ID, EXISTING_PROGRAMME_MEMBERSHIP_UUID);

    assertThat("Unexpected optional isEmpty flag.", programmeMembership.isEmpty(), is(false));
    assertThat("Unexpected COJ signedAt.",
        programmeMembership.get().getConditionsOfJoining().signedAt(),
        notNullValue());
    assertThat("Unexpected COJ version.",
        programmeMembership.get().getConditionsOfJoining().version(),
        is(GoldGuideVersion.getLatest()));
    assertThat("Unexpected COJ syncedAt.",
        programmeMembership.get().getConditionsOfJoining().syncedAt(),
        nullValue());
  }

  @Test
  void shouldNotSignCojWhenTraineeProfileNotFound() {
    Optional<ProgrammeMembership> programmeMembership = service
        .signProgrammeMembershipCoj("randomId", EXISTING_PROGRAMME_MEMBERSHIP_UUID);

    assertThat("Unexpected optional isEmpty flag.", programmeMembership.isEmpty(), is(true));
    verify(repository).findByTraineeTisId("randomId");
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotSignCojWhenTraineeFoundButProgrammeMembershipsNotExists() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_UUID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    Optional<ProgrammeMembership> programmeMembership = service
        .signProgrammeMembershipCoj(TRAINEE_TIS_ID, DIFFERENT_PROGRAMME_MEMBERSHIP_UUID);

    assertThat("Unexpected optional isEmpty flag.", programmeMembership.isEmpty(), is(true));
    verify(repository, never()).save(traineeProfile);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"LAT", "VISITOR"})
  void shouldNotBeOnboardableWhenTypeNotOnboardable(String pmType) {
    ProgrammeMembership pm = createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_UUID,
        ORIGINAL_SUFFIX, 0);
    pm.setProgrammeMembershipType(pmType);

    boolean canBeOnboarded = service.canBeOnboarded(pm);

    assertThat("Unexpected canBeOnboarded result.", canBeOnboarded, is(false));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = "NOT_ONBOARDABLE")
  void shouldNotBeOnboardableWhenNoOnboardableCurriculaSubType(String currSubType) {
    ProgrammeMembership pm = createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_UUID,
        ORIGINAL_SUFFIX, 0);

    Curriculum curr1 = createCurriculum(currSubType, null, "specialty1");
    Curriculum curr2 = createCurriculum("not onboardable", null, "specialty2");
    pm.setCurricula(List.of(curr1, curr2));

    boolean canBeOnboarded = service.canBeOnboarded(pm);

    assertThat("Unexpected canBeOnboarded result.", canBeOnboarded, is(false));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"Public Health Medicine", "Foundation"})
  void shouldNotBeOnboardableWhenNoOnboardableSpecialty(String specialty) {
    ProgrammeMembership pm = createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_UUID,
        ORIGINAL_SUFFIX, 0);

    Curriculum curr1 = createCurriculum("MEDICAL_CURRICULUM", null, specialty);
    Curriculum curr2 = createCurriculum("MEDICAL_SPR", null, specialty);
    pm.setCurricula(List.of(curr1, curr2));

    boolean canBeOnboarded = service.canBeOnboarded(pm);

    assertThat("Unexpected canBeOnboarded result.", canBeOnboarded, is(false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"MEDICAL_CURRICULUM", "MEDICAL_SPR"})
  void shouldBeOnboardableWhenProgrammeMembershipAndCurriculaOnboardable(String subType) {
    ProgrammeMembership pm = createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_UUID,
        ORIGINAL_SUFFIX, 0);

    Curriculum curr1 = createCurriculum(subType, null, "Foundation");
    Curriculum curr2 = createCurriculum(subType, null, "specialty2");
    pm.setCurricula(List.of(curr1, curr2));

    boolean canBeOnboarded = service.canBeOnboarded(pm);

    assertThat("Unexpected canBeOnboarded result.", canBeOnboarded, is(true));
  }

  @Test
  void newStarterShouldBeFalseIfTraineeNotFound() {
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(null);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(false));
  }

  @Test
  void newStarterShouldBeFalseIfPmNotFound() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getNewStarterProgrammeMembershipDefault("unknown id",
            PROGRAMME_MEMBERSHIP_TYPE, START_DATE, END_DATE))); //PROGRAMME_TIS_ID != "unknown id"
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(false));
  }

  @ParameterizedTest
  @NullSource
  @MethodSource("listNonRelevantPmTypes")
  void newStarterShouldBeFalseIfPmHasWrongType(String pmType) {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getNewStarterProgrammeMembershipDefault(PROGRAMME_TIS_ID, pmType, START_DATE,
            END_DATE)));
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(false));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = "1970-01-01")
  void newStarterShouldBeFalseIfPmHasEnded(LocalDate endDate) {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getNewStarterProgrammeMembershipDefault(PROGRAMME_TIS_ID, PROGRAMME_MEMBERSHIP_TYPE,
            START_DATE, endDate)));
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(false));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"another subtype"})
  void newStarterShouldBeFalseIfPmHasNoMedicalCurricula(String curriculumSubtype) {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, START_DATE, END_DATE, MANAGING_DEANERY, curriculumSubtype,
            CURRICULUM_SPECIALTY_CODE)));
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(false));
  }

  @ParameterizedTest
  @MethodSource("listMedicalCurriculaSubTypes")
  void newStarterShouldBeTrueIfPmHasMedicalCurricula(String curriculumSubtype) {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, START_DATE, END_DATE, MANAGING_DEANERY, curriculumSubtype,
            CURRICULUM_SPECIALTY_CODE)));
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(true));
  }

  @Test
  void newStarterShouldBeTrueIfItIsTheOnlyPm() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(List.of(getNewStarterProgrammeMembershipDefault()));
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(true));
  }

  @Test
  void newStarterShouldBeTrueIfNoRecentPrecedingPm() {
    List<ProgrammeMembership> pms
        = new java.util.ArrayList<>(List.of(getNewStarterProgrammeMembershipDefault()));
    pms.add(getNewStarterProgrammeMembershipDefault("another id",
        PROGRAMME_MEMBERSHIP_TYPE, START_DATE.minusDays(PROGRAMME_BREAK_DAYS + 100),
        START_DATE.minusDays(PROGRAMME_BREAK_DAYS + 1)));
    //ended more than PROGRAMME_BREAK_DAYS ago
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(pms);
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(true));
  }

  @Test
  void newStarterShouldBeTrueIfPrecedingPmMissingDateInfo() {
    List<ProgrammeMembership> pms
        = new java.util.ArrayList<>(List.of(getNewStarterProgrammeMembershipDefault()));
    //null startDate
    pms.add(getNewStarterProgrammeMembershipDefault("another id",
        PROGRAMME_MEMBERSHIP_TYPE, null, START_DATE.minusDays(1)));
    //null endDate
    pms.add(getNewStarterProgrammeMembershipDefault("another id2",
        PROGRAMME_MEMBERSHIP_TYPE, START_DATE.minusDays(500), null));
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(pms);
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(true));
  }

  @Test
  void newStarterShouldBeFalseIfOneOfPrecedingPmsIsIntraOrRota() {
    List<ProgrammeMembership> pms
        = new java.util.ArrayList<>(List.of(getNewStarterProgrammeMembershipDefault()));
    //preceding PM, but not an intra-deanery / rota PM because no matching curriculum specialty
    pms.add(getProgrammeMembershipWithOneCurriculum("another id",
        PROGRAMME_MEMBERSHIP_TYPE, START_DATE.minusDays(PROGRAMME_BREAK_DAYS + 100),
        START_DATE.minusDays(PROGRAMME_BREAK_DAYS - 1), MANAGING_DEANERY,
        MEDICAL_CURRICULA.get(0), "a different curriculum specialty"));
    //preceding PM, and an intra-deanery / rota PM
    pms.add(getNewStarterProgrammeMembershipDefault("another id2",
        PROGRAMME_MEMBERSHIP_TYPE, START_DATE.minusDays(PROGRAMME_BREAK_DAYS + 100),
        START_DATE.minusDays(PROGRAMME_BREAK_DAYS - 1)));
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(pms);
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(false));
  }

  @Test
  void newStarterShouldBeFalseIfPrecedingPmWithMultipleCurriculaIsIntraOrRota() {
    //preceding PM with non-matching curriculum specialty code
    ProgrammeMembership rotaPm = getProgrammeMembershipWithOneCurriculum("another id",
        PROGRAMME_MEMBERSHIP_TYPE, START_DATE.minusDays(PROGRAMME_BREAK_DAYS + 100),
        START_DATE.minusDays(PROGRAMME_BREAK_DAYS - 1), MANAGING_DEANERY,
        MEDICAL_CURRICULA.get(0), "some other specialty code");
    //curriculum which matches
    Curriculum c2 = new Curriculum();
    c2.setCurriculumSubType(MEDICAL_CURRICULA.get(0));
    c2.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    rotaPm.setCurricula(List.of(rotaPm.getCurricula().get(0), c2));

    List<ProgrammeMembership> pms
        = new java.util.ArrayList<>(List.of(getNewStarterProgrammeMembershipDefault()));
    pms.add(rotaPm);

    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(pms);
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(false));
  }

  @Test
  void newStarterShouldBeTrueIfPrecedingPmNotIntraOrRota() {
    List<ProgrammeMembership> pms
        = new java.util.ArrayList<>(List.of(getNewStarterProgrammeMembershipDefault()));
    //not an intra-deanery / rota PM because different deanery
    pms.add(getProgrammeMembershipWithOneCurriculum("another id",
        PROGRAMME_MEMBERSHIP_TYPE, START_DATE.minusDays(PROGRAMME_BREAK_DAYS + 100),
        START_DATE.minusDays(PROGRAMME_BREAK_DAYS - 1), "some other deanery",
        MEDICAL_CURRICULA.get(0), CURRICULUM_SPECIALTY_CODE));

    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(pms);
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(true));
  }

  @Test
  void newStarterShouldBeTrueIfIntraOrRotaMissingDeanery() {
    List<ProgrammeMembership> pms
        = new java.util.ArrayList<>(List.of(getNewStarterProgrammeMembershipDefault()));
    //preceding PM with missing deanery
    pms.add(getProgrammeMembershipWithOneCurriculum("another id",
        PROGRAMME_MEMBERSHIP_TYPE, START_DATE.minusDays(PROGRAMME_BREAK_DAYS + 100),
        START_DATE.minusDays(PROGRAMME_BREAK_DAYS - 1), null,
        MEDICAL_CURRICULA.get(0), CURRICULUM_SPECIALTY_CODE));

    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(pms);
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(true));
  }

  @Test
  void newStarterShouldBeTrueIfPmMissingDeanery() {
    //anchor PM has missing deanery
    List<ProgrammeMembership> pms = new java.util.ArrayList<>(List.of(
        getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, START_DATE, END_DATE, null, MEDICAL_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE)));
    pms.add(getNewStarterProgrammeMembershipDefault("another id",
        PROGRAMME_MEMBERSHIP_TYPE, START_DATE.minusDays(PROGRAMME_BREAK_DAYS + 100),
        START_DATE.minusDays(PROGRAMME_BREAK_DAYS - 1)));

    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(pms);
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(true));
  }

  @Test
  void newStarterShouldBeTrueIfIntraOrRotaMissingProgrammeMembershipType() {
    List<ProgrammeMembership> pms
        = new java.util.ArrayList<>(List.of(getNewStarterProgrammeMembershipDefault()));
    //preceding PM with missing programme membership type
    pms.add(getProgrammeMembershipWithOneCurriculum("another id",
        null, START_DATE.minusDays(PROGRAMME_BREAK_DAYS + 100),
        START_DATE.minusDays(PROGRAMME_BREAK_DAYS - 1), MANAGING_DEANERY,
        MEDICAL_CURRICULA.get(0), CURRICULUM_SPECIALTY_CODE));

    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(pms);
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(true));
  }

  @Test
  void newStarterShouldBeTrueIfNoPrecedingPmIsIntraOrRota() {
    List<ProgrammeMembership> pms
        = new java.util.ArrayList<>(List.of(getNewStarterProgrammeMembershipDefault()));
    //preceding PM with different deanery
    pms.add(getProgrammeMembershipWithOneCurriculum("another id",
        PROGRAMME_MEMBERSHIP_TYPE, START_DATE.minusDays(PROGRAMME_BREAK_DAYS + 100),
        START_DATE.minusDays(PROGRAMME_BREAK_DAYS - 1), "different deanery",
        MEDICAL_CURRICULA.get(0), CURRICULUM_SPECIALTY_CODE));
    //not preceding PM, though it would be intra / rota PM
    pms.add(getNewStarterProgrammeMembershipDefault("another id2",
        PROGRAMME_MEMBERSHIP_TYPE, START_DATE.minusDays(PROGRAMME_BREAK_DAYS + 100),
        START_DATE.minusDays(PROGRAMME_BREAK_DAYS + 1)));
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(pms);
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(true));
  }

  @Test
  void newStarterShouldBeTrueIfPmIsMissingStartDateInfo() {
    //anchor PM has missing startDate
    List<ProgrammeMembership> pms = new java.util.ArrayList<>(List.of(
        getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, null, END_DATE, MANAGING_DEANERY,
            MEDICAL_CURRICULA.get(0), CURRICULUM_SPECIALTY_CODE)));
    //intra-deanery / rota PM, but cannot assess whether preceding
    pms.add(getProgrammeMembershipWithOneCurriculum("another id",
        PROGRAMME_MEMBERSHIP_TYPE, START_DATE.minusDays(100),
        START_DATE.minusDays(1), MANAGING_DEANERY, MEDICAL_CURRICULA.get(0),
        CURRICULUM_SPECIALTY_CODE));
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(pms);
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(true));
  }

  @Test
  void pilot2024ShouldBeFalseIfTraineeNotFound() {
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(null);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected pilot2024 value.", isPilot2024, is(false));
  }

  @Test
  void pilot2024ShouldBeFalseIfPmNotFound() {
    TraineeProfile traineeProfile = new TraineeProfile();
    ProgrammeMembership pm = getProgrammeMembershipWithOneCurriculum("unknown id",
        PROGRAMME_MEMBERSHIP_TYPE, START_DATE, END_DATE, MANAGING_DEANERY, TSS_CURRICULA.get(0),
        CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY);

    traineeProfile.setProgrammeMemberships(List.of(pm)); //PROGRAMME_TIS_ID != "unknown id"

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"another subtype"})
  void pilot2024ShouldBeFalseIfPmHasNoMedicalCurricula(String curriculumSubtype) {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, START_DATE, END_DATE, MANAGING_DEANERY, curriculumSubtype,
            CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @ParameterizedTest
  @NullSource
  @MethodSource("listNonRelevantPmTypes")
  void pilot2024ShouldBeFalseIfPmHasWrongType(String pmType) {
    TraineeProfile traineeProfile = new TraineeProfile();
    ProgrammeMembership pm = getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID, pmType,
        START_DATE, END_DATE, MANAGING_DEANERY, TSS_CURRICULA.get(0), CURRICULUM_SPECIALTY_CODE,
        CURRICULUM_SPECIALTY);
    traineeProfile.setProgrammeMemberships(List.of(pm));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isPilot2024, is(false));
  }

  @ParameterizedTest
  @MethodSource("listLoPilot2024AllProgrammes")
  void pilot2024ShouldBeTrueIfLoWithAllProgrammesAndCorrectStartDate(String lo) {
    LocalDate dateInRange = LocalDate.of(2024, 8, 1);
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateInRange, END_DATE, lo, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(true));
  }

  @ParameterizedTest
  @MethodSource("listLoPilot2024AllProgrammes")
  void pilot2024ShouldBeFalseIfLoWithAllProgrammesAndWrongStartDate(String lo) {
    LocalDate dateOutOfRange = LocalDate.of(2024, 7, 1);
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateOutOfRange, END_DATE, lo, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @ParameterizedTest
  @MethodSource("listLoPilot2024AllProgrammes")
  void pilot2024ShouldBeFalseIfLoWithAllProgrammesAndTooLateStartDate(String lo) {
    LocalDate dateOutOfRange = LocalDate.of(2024, 12, 1);
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateOutOfRange, END_DATE, lo, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Internal Medicine Stage One", "Core surgical training"})
  void pilot2024ShouldBeTrueForYhWithCorrectDateAndCurriculumSpecialty(String specialty) {
    LocalDate date = LocalDate.of(2024, 8, 15);
    String deanery = "Yorkshire and the Humber";
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, date, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, specialty)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(true));
  }

  @Test
  void pilot2024ShouldBeFalseForYhGeneralPractice() {
    LocalDate date = LocalDate.of(2024, 8, 7);
    String deanery = "Yorkshire and the Humber";
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, date, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, "General Practice")));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Internal Medicine Stage One", "Core surgical training"})
  void pilot2024ShouldBeFalseForYhWithTooLateDateAndCorrectCurriculumSpecialty(String specialty) {
    LocalDate wrongDate = LocalDate.of(2024, 11, 1);
    String deanery = "Yorkshire and the Humber";
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, wrongDate, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, specialty)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Internal Medicine Stage One", "Core surgical training"})
  void pilot2024ShouldBeFalseForYhWithTooEarlyDateAndCorrectCurriculumSpecialty(String specialty) {
    LocalDate wrongDate = LocalDate.of(2024, 7, 1);
    String deanery = "Yorkshire and the Humber";
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, wrongDate, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, specialty)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Internal Medicine Stage One", "Core surgical training"})
  void pilot2024ShouldBeFalseIfYhLoWithGpSpecialtyInMultipleCurricula(String specialty) {
    LocalDate dateInRange = LocalDate.of(2024, 8, 15);
    String deanery = "Yorkshire and the Humber";
    TraineeProfile traineeProfile = new TraineeProfile();
    Curriculum curriculum = createCurriculum(MEDICAL_CURRICULA.get(0),
        CURRICULUM_SPECIALTY_CODE, specialty);
    Curriculum curriculumGp = createCurriculum(MEDICAL_CURRICULA.get(1),
        CURRICULUM_SPECIALTY_CODE, "General Practice");
    ProgrammeMembership programmeMembership =
        getProgrammeMembershipWithMultipleCurriculum(PROGRAMME_TIS_ID, PROGRAMME_MEMBERSHIP_TYPE,
            dateInRange, END_DATE, deanery, List.of(curriculum, curriculumGp));

    traineeProfile.setProgrammeMemberships(List.of(programmeMembership));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Internal Medicine Stage One", "Core surgical training"})
  void pilot2024ShouldBeTrueIfSwLoWithCorrectStartDateAndSpecialty(String specialty) {
    LocalDate dateInRange = LocalDate.of(2024, 10, 31);
    String deanery = "South West";
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateInRange, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, specialty)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Internal Medicine Stage One", "Core surgical training"})
  void pilot2024ShouldBeFalseIfSwLoWithTooEarlyStartDate(String specialty) {
    LocalDate dateInRange = LocalDate.of(2024, 6, 2);
    String deanery = "South West";
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateInRange, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, specialty)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Internal Medicine Stage One", "Core surgical training"})
  void pilot2024ShouldBeFalseIfSwLoWithTooLateStartDate(String specialty) {
    LocalDate dateInRange = LocalDate.of(2024, 11, 1);
    String deanery = "South West";
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateInRange, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, specialty)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @Test
  void pilot2024ShouldBeFalseIfSwLoWithGpSpecialty() {
    LocalDate dateInRange = LocalDate.of(2024, 8, 5);
    String deanery = "South West";
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateInRange, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, "General Practice")));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Internal Medicine Stage One", "Core surgical training"})
  void pilot2024ShouldBeFalseIfSwLoWithGpSpecialtyInMultipleCurricula(String specialty) {
    LocalDate dateInRange = LocalDate.of(2024, 10, 31);
    String deanery = "South West";
    TraineeProfile traineeProfile = new TraineeProfile();
    Curriculum curriculum = createCurriculum(MEDICAL_CURRICULA.get(0),
        CURRICULUM_SPECIALTY_CODE, specialty);
    Curriculum curriculumGp = createCurriculum(MEDICAL_CURRICULA.get(1),
        CURRICULUM_SPECIALTY_CODE, "General Practice");
    ProgrammeMembership programmeMembership =
        getProgrammeMembershipWithMultipleCurriculum(PROGRAMME_TIS_ID, PROGRAMME_MEMBERSHIP_TYPE,
            dateInRange, END_DATE, deanery, List.of(curriculum, curriculumGp));

    traineeProfile.setProgrammeMemberships(List.of(programmeMembership));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @ParameterizedTest
  @MethodSource("listNwPilot2024AllSpecialties")
  void pilot2024ShouldBeTrueIfNwLoWithCorrectStartDateAndSpecialty(String specialty) {
    LocalDate dateInRange = LocalDate.of(2024, 8, 1);
    String deanery = "North West";
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateInRange, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, specialty)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(true));
  }

  @ParameterizedTest
  @MethodSource("listNwPilot2024AllSpecialties")
  void pilot2024ShouldBeFalseIfNwLoWithIncorrectStartDateAndOkSpecialty(String specialty) {
    LocalDate dateOutOfRange = LocalDate.of(2024, 7, 1);
    String deanery = "North West";
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateOutOfRange, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, specialty)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @ParameterizedTest
  @MethodSource("listNwPilot2024AllSpecialties")
  void pilot2024ShouldBeFalseIfNwLoWithTooLateStartDateAndOkSpecialty(String specialty) {
    LocalDate dateOutOfRange = LocalDate.of(2024, 12, 1);
    String deanery = "North West";
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateOutOfRange, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, specialty)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Cardio-thoracic surgery (run through)",
      "Oral and maxillo-facial surgery (run through)"})
  void pilot2024ShouldBeTrueIfNwLoWithCorrectStartDateAndProgramme(String programme) {
    LocalDate dateInRange = LocalDate.of(2024, 8, 1);
    String deanery = "North West";
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateInRange, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY)));
    traineeProfile.getProgrammeMemberships().get(0).setProgrammeName(programme);

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Cardio-thoracic surgery (run through)",
      "Oral and maxillo-facial surgery (run through)"})
  void pilot2024ShouldBeFalseIfNwLoWithIncorrectStartDateAndOkProgramme(String programme) {
    LocalDate dateOutOfRange = LocalDate.of(2024, 7, 1);
    String deanery = "North West";
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateOutOfRange, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY)));
    traineeProfile.getProgrammeMemberships().get(0).setProgrammeName(programme);

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @Test
  void pilot2024ShouldBeFalseIfConditionsNotMet() {
    //obviously there are a number of scenarios that could (should) be tested here
    LocalDate dateInRange = LocalDate.of(2024, 8, 1);
    String invalidSpecialty = "some specialty";
    String deanery = "North West";
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateInRange, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, invalidSpecialty)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @ParameterizedTest
  @MethodSource("listTssCurriculaSubTypes")
  void pilot2024ShouldBeTrueIfTssCurriculumSubtype(String validCurriculumSubtype) {
    LocalDate dateInRange = LocalDate.of(2024, 8, 1);
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateInRange, END_DATE,
            PILOT_2024_LOCAL_OFFICES_ALL_PROGRAMMES.get(0), validCurriculumSubtype,
            CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(true));
  }

  @Test
  void pilot2024ShouldBeFalseIfNotTssCurriculumSubtype() {
    LocalDate dateInRange = LocalDate.of(2024, 8, 1);
    String invalidCurriculumSubtype = "xxx";
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateInRange, END_DATE,
            PILOT_2024_LOCAL_OFFICES_ALL_PROGRAMMES.get(0), invalidCurriculumSubtype,
            CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @ParameterizedTest
  @NullSource
  @MethodSource("listNotTssCurriculaSpecialties")
  void pilot2024ShouldBeFalseIfInvalidTssCurriculumSpecialty(String invalidCurriculumSpecialty) {
    LocalDate dateInRange = LocalDate.of(2024, 8, 1);
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateInRange, END_DATE,
            PILOT_2024_LOCAL_OFFICES_ALL_PROGRAMMES.get(0), TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, invalidCurriculumSpecialty)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @Test
  void pilot2024ShouldBeTrueIfNotInvalidTssCurriculumSpecialty() {
    LocalDate dateInRange = LocalDate.of(2024, 8, 1);
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateInRange, END_DATE,
            PILOT_2024_LOCAL_OFFICES_ALL_PROGRAMMES.get(0), TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(true));
  }

  @Test
  void rollout2024ShouldBeFalseIfTraineeNotFound() {
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(null);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected pilotRollout2024 value.", isPilotRollout2024, is(false));
  }

  @Test
  void rollout2024ShouldBeFalseIfPmNotFound() {
    TraineeProfile traineeProfile = new TraineeProfile();
    ProgrammeMembership pm = getProgrammeMembershipWithOneCurriculum("unknown id",
        PROGRAMME_MEMBERSHIP_TYPE, START_DATE, END_DATE, MANAGING_DEANERY, TSS_CURRICULA.get(0),
        CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY);

    traineeProfile.setProgrammeMemberships(List.of(pm)); //PROGRAMME_TIS_ID != "unknown id"

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilotRollout2024 value.", isPilotRollout2024, is(false));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"another subtype"})
  void rollout2024ShouldBeFalseIfPmHasNoMedicalCurricula(String curriculumSubtype) {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, START_DATE, END_DATE, MANAGING_DEANERY, curriculumSubtype,
            CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilotRollout2024 value.", isPilotRollout2024, is(false));
  }

  @ParameterizedTest
  @NullSource
  @MethodSource("listNonRelevantPmTypes")
  void rollout2024ShouldBeFalseIfPmHasWrongType(String pmType) {
    TraineeProfile traineeProfile = new TraineeProfile();
    ProgrammeMembership pm = getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID, pmType,
        START_DATE, END_DATE, MANAGING_DEANERY, TSS_CURRICULA.get(0), CURRICULUM_SPECIALTY_CODE,
        CURRICULUM_SPECIALTY);
    traineeProfile.setProgrammeMemberships(List.of(pm));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilotRollout2024 value.", isPilotRollout2024, is(false));
  }

  @ParameterizedTest
  @MethodSource("listLoRollout2024")
  void rollout2024ShouldBeTrueIfLoWithAllProgrammesAndOkStartDate(String deanery) {
    LocalDate notificationEpoch = LocalDate.of(2024, 10, 31);
    if (deanery.equalsIgnoreCase("Thames Valley")) {
      notificationEpoch = LocalDate.of(2025, 1, 31);
    }
    if (deanery.equalsIgnoreCase("North East")) {
      notificationEpoch = LocalDate.of(2025, 4, 13);
    }
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, notificationEpoch.plusDays(1), END_DATE, deanery,
            TSS_CURRICULA.get(0), CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilotRollout2024 value.", isPilotRollout2024, is(true));
  }

  @ParameterizedTest
  @MethodSource("listLoRollout2024")
  void rollout2024ShouldBeFalseIfLoWithAllProgrammesAndTooEarlyStartDate(String deanery) {
    LocalDate dateTooEarly = LocalDate.of(2024, 10, 31);
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateTooEarly, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilotRollout2024 value.", isPilotRollout2024, is(false));
  }

  @Test
  void rollout2024ShouldBeFalseIfTvLoWithTooEarlyStartDate() {
    LocalDate dateOutOfRange = LocalDate.of(2025, 1, 31);
    String deanery = "Thames Valley";
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateOutOfRange, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilotRollout2024 value.", isPilotRollout2024, is(false));
  }

  @Test
  void rollout2024ShouldBeTrueIfTvLoWithCorrectStartDate() {
    LocalDate dateInRange = LocalDate.of(2025, 2, 1);
    String deanery = "Thames Valley";
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateInRange, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilotRollout2024 value.", isPilotRollout2024, is(true));
  }

  @Test
  void rollout2024ShouldBeFalseIfNeLoWithTooEarlyStartDate() {
    LocalDate dateOutOfRange = LocalDate.of(2025, 4, 13);
    String deanery = "North East";
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateOutOfRange, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilotRollout2024 value.", isPilotRollout2024, is(false));
  }

  @Test
  void rollout2024ShouldBeTrueIfNeLoWithCorrectStartDate() {
    LocalDate dateInRange = LocalDate.of(2025, 4, 14);
    String deanery = "North East";
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateInRange, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilotRollout2024 value.", isPilotRollout2024, is(true));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"Defence Postgraduate Medical Deanery"})
  void rollout2024ShouldBeFalseIfNonRolloutLo(String deanery) {
    LocalDate dateInRange = LocalDate.of(2024, 12, 1);
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, dateInRange, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilotRollout2024 value.", isPilotRollout2024, is(false));
  }

  @ParameterizedTest
  @MethodSource("listLoRollout2024")
  void rollout2024ShouldBeFalseIfNoStartDate(String deanery) {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, null, END_DATE, deanery, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isPilotRollout2024 value.", isPilotRollout2024, is(false));
  }

  @Test
  void shouldGenerateProgrammeConfirmationPdf() throws IOException {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPersonalDetails(createPersonalDetails(""));
    traineeProfile.setProgrammeMemberships(
        List.of(
            getProgrammeMembershipWithOneCurriculum("OtherId",
            PROGRAMME_MEMBERSHIP_TYPE, START_DATE, END_DATE, MANAGING_DEANERY, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY),
            getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, START_DATE, END_DATE, MANAGING_DEANERY, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY)
            ));

    List<Map<String, String>> contacts = new ArrayList<>();
    Map<String, String> contact1 = new HashMap<>();
    contact1.put(CONTACT_TYPE_FIELD,
        LocalOfficeContactType.ONBOARDING_SUPPORT.getContactTypeName());
    contact1.put(CONTACT_FIELD, OWNER_CONTACT);
    contacts.add(contact1);

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(restTemplate
        .getForObject("http://localhost/8205/reference/api/local-office-contact-by-lo-name"
                + "/{localOfficeName}",
            List.class, Map.of("localOfficeName", MANAGING_DEANERY))).thenReturn(contacts);

    service.generateProgrammeMembershipPdf(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
    verify(pdfService).generatePdf(any(), variablesCaptor.capture());
    Map<String, Object> variables = variablesCaptor.getValue();
    assertThat("Unexpected programme membership.", variables.get("pm"),
        is(traineeProfile.getProgrammeMemberships().get(1)));
    assertThat("Unexpected trainee.", variables.get("trainee"),
        is(traineeProfile.getPersonalDetails()));
    assertThat("Unexpected local Office Contact.", variables.get("localOfficeContact"),
        is(OWNER_CONTACT));
    assertThat("Unexpected contact Href.", variables.get("contactHref"),
        is(NON_HREF.toString()));
  }

  @Test
  void shouldNotGenerateProgrammeConfirmationPdfWhenTraineeNotFound() {
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(null);

    assertThrows(IllegalArgumentException.class,
        () -> service.generateProgrammeMembershipPdf(TRAINEE_TIS_ID, PROGRAMME_TIS_ID));
    verifyNoInteractions(restTemplate);
  }

  @Test
  void shouldNotGenerateProgrammeConfirmationPdfWhenPmNotFound() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPersonalDetails(createPersonalDetails(""));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    assertThrows(IllegalArgumentException.class,
        () -> service.generateProgrammeMembershipPdf(TRAINEE_TIS_ID, PROGRAMME_TIS_ID));
    verifyNoInteractions(restTemplate);
  }

  @Test
  void shouldNotGenerateProgrammeConfirmationPdfWhenPmNotStartIn12Weeks() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPersonalDetails(createPersonalDetails(""));
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, START_DATE.plusWeeks(PM_CONFIRM_WEEKS).plusDays(1),
            END_DATE, MANAGING_DEANERY, TSS_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE, CURRICULUM_SPECIALTY)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    assertThrows(IllegalArgumentException.class,
        () -> service.generateProgrammeMembershipPdf(TRAINEE_TIS_ID, PROGRAMME_TIS_ID));
    verifyNoInteractions(restTemplate);
  }

  @Test
  void shouldGetContactWhenContactTypeExists() {
    List<Map<String, String>> contacts = new ArrayList<>();
    Map<String, String> contact1 = new HashMap<>();
    contact1.put(CONTACT_TYPE_FIELD, LocalOfficeContactType.TSS_SUPPORT.getContactTypeName());
    contact1.put(CONTACT_FIELD, "one@email.com, another@email.com");
    contacts.add(contact1);
    Map<String, String> contact2 = new HashMap<>();
    contact2.put(CONTACT_TYPE_FIELD, LocalOfficeContactType.DEFERRAL.getContactTypeName());
    contact2.put(CONTACT_FIELD, "onboarding");
    contacts.add(contact2);

    String ownerContact = service.getOwnerContact(contacts,
        LocalOfficeContactType.TSS_SUPPORT, LocalOfficeContactType.DEFERRAL,
        DEFAULT_NO_CONTACT_MESSAGE);

    assertThat("Unexpected owner contact.", ownerContact, is(contact1.get(CONTACT_FIELD)));
  }

  @Test
  void shouldGetFallbackContactWhenContactMissing() {
    List<Map<String, String>> contacts = new ArrayList<>();
    Map<String, String> contact1 = new HashMap<>();
    contact1.put(CONTACT_TYPE_FIELD, LocalOfficeContactType.TSS_SUPPORT.getContactTypeName());
    contact1.put(CONTACT_FIELD, "one@email.com, another@email.com");
    contacts.add(contact1);

    String ownerContact = service.getOwnerContact(contacts,
        LocalOfficeContactType.ONBOARDING_SUPPORT, LocalOfficeContactType.TSS_SUPPORT,
        DEFAULT_NO_CONTACT_MESSAGE);

    assertThat("Unexpected owner contact.", ownerContact, is(contact1.get(CONTACT_FIELD)));
  }

  @Test
  void shouldGetDefaultNoContactWhenContactMissingAndFallbackNull() {
    List<Map<String, String>> contacts = new ArrayList<>();
    Map<String, String> contact1 = new HashMap<>();
    contact1.put(CONTACT_TYPE_FIELD, LocalOfficeContactType.TSS_SUPPORT.getContactTypeName());
    contact1.put(CONTACT_FIELD, "one@email.com, another@email.com");
    contacts.add(contact1);

    String ownerContact = service.getOwnerContact(contacts,
        LocalOfficeContactType.ONBOARDING_SUPPORT, null, DEFAULT_NO_CONTACT_MESSAGE);

    assertThat("Unexpected owner contact.", ownerContact, is(DEFAULT_NO_CONTACT_MESSAGE));
  }

  @Test
  void shouldUseCustomDefaultNoContactWhenContactAndFallbackMissing() {
    List<Map<String, String>> contacts = new ArrayList<>();
    Map<String, String> contact1 = new HashMap<>();
    contact1.put(CONTACT_TYPE_FIELD, LocalOfficeContactType.TSS_SUPPORT.getContactTypeName());
    contact1.put(CONTACT_FIELD, "one@email.com, another@email.com");
    contacts.add(contact1);

    String ownerContact = service.getOwnerContact(contacts,
        LocalOfficeContactType.ONBOARDING_SUPPORT, LocalOfficeContactType.DEFERRAL, "testDefault");

    assertThat("Unexpected owner contact.", ownerContact, is("testDefault"));
  }

  @Test
  void shouldUseCustomDefaultNoContactWhenContactMissingAndFallbackNull() {
    List<Map<String, String>> contacts = new ArrayList<>();
    Map<String, String> contact1 = new HashMap<>();
    contact1.put(CONTACT_TYPE_FIELD, LocalOfficeContactType.TSS_SUPPORT.getContactTypeName());
    contact1.put(CONTACT_FIELD, "one@email.com, another@email.com");
    contacts.add(contact1);

    String ownerContact = service.getOwnerContact(contacts,
        LocalOfficeContactType.ONBOARDING_SUPPORT, null, "testDefault");

    assertThat("Unexpected owner contact.", ownerContact, is("testDefault"));
  }

  @Test
  void shouldGetEmptyContactListIfReferenceServiceFailure() {
    doThrow(new RestClientException("error"))
        .when(restTemplate).getForObject(any(), any(), anyMap());

    List<Map<String, String>> contactList = service.getOwnerContactList("a local office");

    assertThat("Unexpected owner contact list.", contactList.size(), is(0));
  }

  @Test
  void shouldReturnNullContactListIfOwnerContactListIsNull() {
    when(restTemplate.getForObject(any(), any(), anyMap())).thenReturn(null);

    List<Map<String, String>> contactList = service.getOwnerContactList("a local office");

    assertThat("Unexpected owner contact list.", contactList.size(), is(0));
  }

  @Test
  void shouldGetEmptyContactListIfLocalOfficeNull() {
    List<Map<String, String>> contactList = service.getOwnerContactList(null);

    assertThat("Unexpected owner contact.", contactList.size(), is(0));
  }

  @Test
  void shouldGetUrlHrefTypeForUrlContact() {
    assertThat("Unexpected contact href type.",
        service.getHrefTypeForContact("https://a.validwebsite.com"),
        is(ABSOLUTE_URL.getHrefTypeName()));
  }

  @Test
  void shouldGetEmailHrefTypeForEmailContact() {
    assertThat("Unexpected contact href type.",
        service.getHrefTypeForContact("some@email.com"),
        is(PROTOCOL_EMAIL.getHrefTypeName()));
  }

  @Test
  void shouldGetNonValidHrefTypeForOtherTypeContact() {
    assertThat("Unexpected contact href type.",
        service.getHrefTypeForContact("some@email.com, also@another.com"),
        is(NON_HREF.getHrefTypeName()));
  }

  /**
   * Create an instance of ProgrammeMembership with default dummy values.
   *
   * @param tisId              The TIS ID to set on the programmeMembership.
   * @param stringSuffix       The suffix to use for string values.
   * @param dateAdjustmentDays The number of days to add to dates.
   * @return The dummy entity.
   */
  private ProgrammeMembership createProgrammeMembership(String tisId, String stringSuffix,
      int dateAdjustmentDays) {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId(tisId);
    programmeMembership.setProgrammeTisId(PROGRAMME_TIS_ID + stringSuffix);
    programmeMembership.setProgrammeName(PROGRAMME_NAME + stringSuffix);
    programmeMembership.setProgrammeNumber(PROGRAMME_NUMBER + stringSuffix);
    programmeMembership.setManagingDeanery(MANAGING_DEANERY + stringSuffix);
    programmeMembership.setDesignatedBody(DESIGNATED_BODY + stringSuffix);
    programmeMembership.setDesignatedBodyCode(DESIGNATED_BODY_CODE + stringSuffix);
    programmeMembership.setProgrammeMembershipType(PROGRAMME_MEMBERSHIP_TYPE + stringSuffix);
    programmeMembership.setStartDate(START_DATE.plusDays(dateAdjustmentDays));
    programmeMembership.setEndDate(END_DATE.plusDays(dateAdjustmentDays));
    programmeMembership.setProgrammeCompletionDate(COMPLETION_DATE.plusDays(dateAdjustmentDays));
    programmeMembership.setConditionsOfJoining(
        new ConditionsOfJoining(COJ_SIGNED_AT.plus(Duration.ofDays(dateAdjustmentDays)),
            GOLD_GUIDE_VERSION, COJ_SYNCED_AT.plus(Duration.ofDays(dateAdjustmentDays))));

    programmeMembership.setResponsibleOfficer(getResponsibleOfficerUser(stringSuffix));

    return programmeMembership;
  }

  static Stream<String> listNonRelevantPmTypes() {
    return NON_RELEVANT_PROGRAMME_MEMBERSHIP_TYPES.stream();
  }

  static Stream<String> listLoRollout2024() {
    return PILOT_2024_ROLLOUT_LOCAL_OFFICES.stream();
  }

  static Stream<String> listLoPilot2024AllProgrammes() {
    return PILOT_2024_LOCAL_OFFICES_ALL_PROGRAMMES.stream();
  }

  static Stream<String> listNwPilot2024AllSpecialties() {
    return PILOT_2024_NW_SPECIALTIES.stream();
  }

  static Stream<String> listMedicalCurriculaSubTypes() {
    return MEDICAL_CURRICULA.stream();
  }

  static Stream<String> listTssCurriculaSubTypes() {
    return TSS_CURRICULA.stream();
  }

  static Stream<String> listNotTssCurriculaSpecialties() {
    return NOT_TSS_SPECIALTIES.stream();
  }

  /**
   * Create a programme membership with a single curriculum for testing isNewStarter conditions.
   *
   * @param programmeMembershipTisId The TIS ID to set on the programmeMembership.
   * @param programmeMembershipType  The programme membership type.
   * @param startDate                The start date.
   * @param endDate                  The end date.
   * @param managingDeanery          The managing deanery.
   * @param curriculumSubType        The curriculum subtype.
   * @param curriculumSpecialtyCode  The curriculum specialty code.
   * @return The programme membership.
   */
  private ProgrammeMembership getProgrammeMembershipWithOneCurriculum(
      String programmeMembershipTisId, String programmeMembershipType, LocalDate startDate,
      LocalDate endDate, String managingDeanery, String curriculumSubType,
      String curriculumSpecialtyCode) {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId(programmeMembershipTisId);
    programmeMembership.setProgrammeTisId(PROGRAMME_TIS_ID);
    programmeMembership.setProgrammeName(PROGRAMME_NAME);
    programmeMembership.setProgrammeNumber(PROGRAMME_NUMBER);
    programmeMembership.setManagingDeanery(managingDeanery);
    programmeMembership.setDesignatedBody(DESIGNATED_BODY);
    programmeMembership.setProgrammeMembershipType(programmeMembershipType);
    programmeMembership.setStartDate(startDate);
    programmeMembership.setEndDate(endDate);
    programmeMembership.setProgrammeCompletionDate(COMPLETION_DATE);

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType(curriculumSubType);
    curriculum.setCurriculumSpecialtyCode(curriculumSpecialtyCode);
    programmeMembership.setCurricula(List.of(curriculum));

    programmeMembership.setResponsibleOfficer(getResponsibleOfficerUser(""));

    return programmeMembership;
  }

  /**
   * Create a programme membership with a single curriculum for testing pilot2024 conditions.
   *
   * @param programmeMembershipTisId The TIS ID to set on the programmeMembership.
   * @param programmeMembershipType  The programme membership type.
   * @param startDate                The start date.
   * @param endDate                  The end date.
   * @param managingDeanery          The managing deanery.
   * @param curriculumSubType        The curriculum subtype.
   * @param curriculumSpecialtyCode  The curriculum specialty code.
   * @param curriculumSpecialty      The curriculum specialty name.
   * @return The programme membership.
   */
  private ProgrammeMembership getProgrammeMembershipWithOneCurriculum(
      String programmeMembershipTisId, String programmeMembershipType, LocalDate startDate,
      LocalDate endDate, String managingDeanery, String curriculumSubType,
      String curriculumSpecialtyCode, String curriculumSpecialty) {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId(programmeMembershipTisId);
    programmeMembership.setProgrammeTisId(PROGRAMME_TIS_ID);
    programmeMembership.setProgrammeName(PROGRAMME_NAME);
    programmeMembership.setProgrammeNumber(PROGRAMME_NUMBER);
    programmeMembership.setManagingDeanery(managingDeanery);
    programmeMembership.setDesignatedBody(DESIGNATED_BODY);
    programmeMembership.setProgrammeMembershipType(programmeMembershipType);
    programmeMembership.setStartDate(startDate);
    programmeMembership.setEndDate(endDate);
    programmeMembership.setProgrammeCompletionDate(COMPLETION_DATE);

    Curriculum curriculum =
        createCurriculum(curriculumSubType, curriculumSpecialtyCode, curriculumSpecialty);
    programmeMembership.setCurricula(List.of(curriculum));

    programmeMembership.setResponsibleOfficer(getResponsibleOfficerUser(""));

    return programmeMembership;
  }

  /**
   * Create a programme membership with a multiple curricula for testing pilot2024 conditions.
   *
   * @param programmeMembershipTisId The TIS ID to set on the programmeMembership.
   * @param programmeMembershipType  The programme membership type.
   * @param startDate                The start date.
   * @param endDate                  The end date.
   * @param managingDeanery          The managing deanery.
   * @param curricula                The curricula to set on the programmeMembership.
   * @return The programme membership.
   */
  private ProgrammeMembership getProgrammeMembershipWithMultipleCurriculum(
      String programmeMembershipTisId, String programmeMembershipType, LocalDate startDate,
      LocalDate endDate, String managingDeanery, List<Curriculum> curricula) {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId(programmeMembershipTisId);
    programmeMembership.setProgrammeTisId(PROGRAMME_TIS_ID);
    programmeMembership.setProgrammeName(PROGRAMME_NAME);
    programmeMembership.setProgrammeNumber(PROGRAMME_NUMBER);
    programmeMembership.setManagingDeanery(managingDeanery);
    programmeMembership.setDesignatedBody(DESIGNATED_BODY);
    programmeMembership.setProgrammeMembershipType(programmeMembershipType);
    programmeMembership.setStartDate(startDate);
    programmeMembership.setEndDate(endDate);
    programmeMembership.setProgrammeCompletionDate(COMPLETION_DATE);
    programmeMembership.setCurricula(curricula);

    programmeMembership.setResponsibleOfficer(getResponsibleOfficerUser(""));

    return programmeMembership;
  }

  private Curriculum createCurriculum(String curriculumSubType,
      String curriculumSpecialtyCode, String curriculumSpecialty) {
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType(curriculumSubType);
    curriculum.setCurriculumSpecialtyCode(curriculumSpecialtyCode);
    curriculum.setCurriculumSpecialty(curriculumSpecialty);

    return curriculum;
  }

  /**
   * Create a default new starter programme membership with a single curriculum for testing
   * isNewStarter conditions.
   *
   * @param programmeMembershipTisId The TIS ID to set on the programmeMembership.
   * @param programmeMembershipType  The programme membership type.
   * @param startDate                The start date.
   * @param endDate                  The end date.
   * @return The default programme membership.
   */
  private ProgrammeMembership getNewStarterProgrammeMembershipDefault(
      String programmeMembershipTisId, String programmeMembershipType, LocalDate startDate,
      LocalDate endDate) {
    return getProgrammeMembershipWithOneCurriculum(
        programmeMembershipTisId, programmeMembershipType, startDate,
        endDate, MANAGING_DEANERY, MEDICAL_CURRICULA.get(0), CURRICULUM_SPECIALTY_CODE);
  }

  /**
   * Create a default new starter programme membership with a single curriculum for testing
   * isNewStarter conditions.
   *
   * @return The default programme membership.
   */
  private ProgrammeMembership getNewStarterProgrammeMembershipDefault() {
    return getProgrammeMembershipWithOneCurriculum(
        PROGRAMME_TIS_ID, PROGRAMME_MEMBERSHIP_TYPE, START_DATE,
        END_DATE, MANAGING_DEANERY, MEDICAL_CURRICULA.get(0), CURRICULUM_SPECIALTY_CODE);
  }

  /**
   * Create a default responsible officer HEE user.
   *
   * @param stringSuffix The suffix to apply to field values.
   * @return The HEE user.
   */
  private HeeUser getResponsibleOfficerUser(String stringSuffix) {
    HeeUser heeUser = new HeeUser();
    heeUser.setFirstName(RO_FIRSTNAME + stringSuffix);
    heeUser.setLastName(RO_LASTNAME + stringSuffix);
    heeUser.setPhoneNumber(RO_PHONE + stringSuffix);
    heeUser.setGmcId(RO_GMC + stringSuffix);
    heeUser.setEmailAddress(RO_EMAIL + stringSuffix);

    return heeUser;
  }

  /**
   * Create an instance of PersonalDetails with default dummy values.
   *
   * @return The dummy entity.
   */
  private PersonalDetails createPersonalDetails(String stringSuffix) {
    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setForenames(FORENAMES + stringSuffix);
    personalDetails.setSurname(SURNAME + stringSuffix);
    personalDetails.setGmcNumber(GMC_NUMBER + stringSuffix);

    return personalDetails;
  }
}
