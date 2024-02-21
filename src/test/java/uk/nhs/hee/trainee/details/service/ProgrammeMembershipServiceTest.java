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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.trainee.details.service.ProgrammeMembershipService.MEDICAL_CURRICULA;
import static uk.nhs.hee.trainee.details.service.ProgrammeMembershipService.NON_NEW_START_PROGRAMME_MEMBERSHIP_TYPES;
import static uk.nhs.hee.trainee.details.service.ProgrammeMembershipService.PROGRAMME_BREAK_DAYS;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import uk.nhs.hee.trainee.details.dto.enumeration.GoldGuideVersion;
import uk.nhs.hee.trainee.details.mapper.ProgrammeMembershipMapperImpl;
import uk.nhs.hee.trainee.details.model.ConditionsOfJoining;
import uk.nhs.hee.trainee.details.model.Curriculum;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;

class ProgrammeMembershipServiceTest {

  private static final LocalDate START_DATE = LocalDate.now();
  private static final LocalDate END_DATE = START_DATE.plusYears(1);
  private static final LocalDate COMPLETION_DATE = END_DATE.plusYears(1);
  private static final String PROGRAMME_TIS_ID = "programmeTisId-";
  private static final String PROGRAMME_NAME = "programmeName-";
  private static final String PROGRAMME_NUMBER = "programmeNumber-";
  private static final String MANAGING_DEANERY = "managingDeanery-";
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

  private ProgrammeMembershipService service;
  private TraineeProfileRepository repository;
  private CachingDelegate cachingDelegate;

  @BeforeEach
  void setUp() {
    repository = mock(TraineeProfileRepository.class);
    cachingDelegate = mock(CachingDelegate.class);
    service = new ProgrammeMembershipService(repository, new ProgrammeMembershipMapperImpl(),
        cachingDelegate);
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
    expectedProgrammeMembership
        .setProgrammeMembershipType(PROGRAMME_MEMBERSHIP_TYPE + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setStartDate(START_DATE.plusDays(100));
    expectedProgrammeMembership.setEndDate(END_DATE.plusDays(100));
    expectedProgrammeMembership.setProgrammeCompletionDate(COMPLETION_DATE.plusDays(100));
    expectedProgrammeMembership.setConditionsOfJoining(
        new ConditionsOfJoining(COJ_SIGNED_AT.plus(Duration.ofDays(100)), GOLD_GUIDE_VERSION,
            COJ_SYNCED_AT.plus(Duration.ofDays(100))));

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
    expectedProgrammeMembership
        .setProgrammeMembershipType(PROGRAMME_MEMBERSHIP_TYPE + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setStartDate(START_DATE.plusDays(100));
    expectedProgrammeMembership.setEndDate(END_DATE.plusDays(100));
    expectedProgrammeMembership.setProgrammeCompletionDate(COMPLETION_DATE.plusDays(100));
    expectedProgrammeMembership.setConditionsOfJoining(
        new ConditionsOfJoining(COJ_SIGNED_AT.plus(Duration.ofDays(100)), GOLD_GUIDE_VERSION,
            COJ_SYNCED_AT.plus(Duration.ofDays(100))));

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
    expectedProgrammeMembership
        .setProgrammeMembershipType(PROGRAMME_MEMBERSHIP_TYPE + MODIFIED_SUFFIX);
    expectedProgrammeMembership.setStartDate(START_DATE.plusDays(100));
    expectedProgrammeMembership.setEndDate(END_DATE.plusDays(100));
    expectedProgrammeMembership.setProgrammeCompletionDate(COMPLETION_DATE.plusDays(100));
    expectedProgrammeMembership.setConditionsOfJoining(
        new ConditionsOfJoining(COJ_SIGNED_AT.plus(Duration.ofDays(100)), GOLD_GUIDE_VERSION,
            COJ_SYNCED_AT.plus(Duration.ofDays(100))));

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
        = new ConditionsOfJoining(null, GoldGuideVersion.GG9, null);
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
        new ConditionsOfJoining(null, GoldGuideVersion.GG9, null));

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
        List.of(getProgrammeMembershipDefault("unknown id",
            PROGRAMME_MEMBERSHIP_TYPE, START_DATE, END_DATE)));
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(false));
  }

  @ParameterizedTest
  @NullSource
  @MethodSource("listNonNewStartPmTypes")
  void newStarterShouldBeFalseIfPmHasWrongType(String pmType) {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipDefault(PROGRAMME_TIS_ID, pmType, START_DATE, END_DATE)));
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(false));
  }

  @ParameterizedTest
  @NullSource
  @CsvSource({"1970-01-01"})
  void newStarterShouldBeFalseIfPmHasEnded(LocalDate endDate) {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembershipDefault(PROGRAMME_TIS_ID, PROGRAMME_MEMBERSHIP_TYPE, START_DATE,
            endDate)));
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

  @Test
  void newStarterShouldBeTrueIfItIsTheOnlyPm() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(List.of(getProgrammeMembershipDefault()));
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(true));
  }

  @Test
  void newStarterShouldBeTrueIfPrecedingPmEndedTooLongAgo() {
    List<ProgrammeMembership> pms
        = new java.util.ArrayList<>(List.of(getProgrammeMembershipDefault()));
    pms.add(getProgrammeMembershipDefault("another id",
        PROGRAMME_MEMBERSHIP_TYPE, START_DATE.minusDays(PROGRAMME_BREAK_DAYS + 100),
        START_DATE.minusDays(PROGRAMME_BREAK_DAYS + 1)));
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(pms);
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(true));
  }

  @Test
  void newStarterShouldBeTrueIfPrecedingPmIsMissingDateInfo() {
    List<ProgrammeMembership> pms
        = new java.util.ArrayList<>(List.of(getProgrammeMembershipDefault()));
    pms.add(getProgrammeMembershipDefault("another id",
        PROGRAMME_MEMBERSHIP_TYPE, null, START_DATE.minusDays(1)));
    pms.add(getProgrammeMembershipDefault("another id2",
        PROGRAMME_MEMBERSHIP_TYPE, START_DATE.minusDays(500), null));
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(pms);
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(true));
  }

  @Test
  void newStarterShouldBeFalseIfOnePrecedingPmEndedNotLongAgoAndIsIntraOrRota() {
    List<ProgrammeMembership> pms
        = new java.util.ArrayList<>(List.of(getProgrammeMembershipDefault()));
    pms.add(getProgrammeMembershipDefault("another id",
        PROGRAMME_MEMBERSHIP_TYPE, START_DATE.minusDays(PROGRAMME_BREAK_DAYS + 100),
        START_DATE.minusDays(PROGRAMME_BREAK_DAYS + 1)));
    pms.add(getProgrammeMembershipDefault("another id2",
        PROGRAMME_MEMBERSHIP_TYPE, START_DATE.minusDays(PROGRAMME_BREAK_DAYS + 100),
        START_DATE.minusDays(PROGRAMME_BREAK_DAYS - 1)));
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(pms);
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(false));
  }

  @Test
  void newStarterShouldBeTrueIfOnePrecedingPmEndedNotLongAgoButIsNotIntraOrRota() {
    List<ProgrammeMembership> pms
        = new java.util.ArrayList<>(List.of(getProgrammeMembershipDefault()));
    pms.add(getProgrammeMembershipWithOneCurriculum("another id",
        PROGRAMME_MEMBERSHIP_TYPE, START_DATE.minusDays(PROGRAMME_BREAK_DAYS + 100),
        START_DATE.minusDays(PROGRAMME_BREAK_DAYS - 1), MANAGING_DEANERY,
        MEDICAL_CURRICULA.get(0), "a different curriculum specialty"));

    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setProgrammeMemberships(pms);
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isNewStarter = service.isNewStarter(TRAINEE_TIS_ID, PROGRAMME_TIS_ID);

    assertThat("Unexpected isNewStarter value.", isNewStarter, is(true));
  }

  @Test
  void newStarterShouldBeTrueIfIntraOrRotaMissingDeanery() {
    List<ProgrammeMembership> pms
        = new java.util.ArrayList<>(List.of(getProgrammeMembershipDefault()));
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
    List<ProgrammeMembership> pms = new java.util.ArrayList<>(List.of(
        getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, START_DATE, END_DATE, null, MEDICAL_CURRICULA.get(0),
            CURRICULUM_SPECIALTY_CODE)));
    pms.add(getProgrammeMembershipDefault("another id",
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
        = new java.util.ArrayList<>(List.of(getProgrammeMembershipDefault()));
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
  void newStarterShouldBeTrueIfPmIsMissingStartDateInfo() {
    List<ProgrammeMembership> pms = new java.util.ArrayList<>(List.of(
        getProgrammeMembershipWithOneCurriculum(PROGRAMME_TIS_ID,
            PROGRAMME_MEMBERSHIP_TYPE, null, END_DATE, MANAGING_DEANERY,
            MEDICAL_CURRICULA.get(0), CURRICULUM_SPECIALTY_CODE)));
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
    programmeMembership.setProgrammeMembershipType(PROGRAMME_MEMBERSHIP_TYPE + stringSuffix);
    programmeMembership.setStartDate(START_DATE.plusDays(dateAdjustmentDays));
    programmeMembership.setEndDate(END_DATE.plusDays(dateAdjustmentDays));
    programmeMembership.setProgrammeCompletionDate(COMPLETION_DATE.plusDays(dateAdjustmentDays));
    programmeMembership.setConditionsOfJoining(
        new ConditionsOfJoining(COJ_SIGNED_AT.plus(Duration.ofDays(dateAdjustmentDays)),
            GOLD_GUIDE_VERSION, COJ_SYNCED_AT.plus(Duration.ofDays(dateAdjustmentDays))));

    return programmeMembership;
  }

  static Stream<String> listNonNewStartPmTypes() {
    return NON_NEW_START_PROGRAMME_MEMBERSHIP_TYPES.stream();
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
    programmeMembership.setProgrammeMembershipType(programmeMembershipType);
    programmeMembership.setStartDate(startDate);
    programmeMembership.setEndDate(endDate);
    programmeMembership.setProgrammeCompletionDate(COMPLETION_DATE);

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType(curriculumSubType);
    curriculum.setCurriculumSpecialtyCode(curriculumSpecialtyCode);
    programmeMembership.setCurricula(List.of(curriculum));

    return programmeMembership;
  }

  /**
   * Create a default programme membership with a single curriculum for testing
   * isNewStarter conditions.
   *
   * @param programmeMembershipTisId The TIS ID to set on the programmeMembership.
   * @param programmeMembershipType  The programme membership type.
   * @param startDate                The start date.
   * @param endDate                  The end date.
   * @return The default programme membership.
   */
  private ProgrammeMembership getProgrammeMembershipDefault(
      String programmeMembershipTisId, String programmeMembershipType, LocalDate startDate,
      LocalDate endDate) {
    return getProgrammeMembershipWithOneCurriculum(
        programmeMembershipTisId, programmeMembershipType, startDate,
        endDate, MANAGING_DEANERY, MEDICAL_CURRICULA.get(0), CURRICULUM_SPECIALTY_CODE);
  }

  /**
   * Create a default programme membership with a single curriculum for testing
   * isNewStarter conditions.
   *
   * @return The default programme membership.
   */
  private ProgrammeMembership getProgrammeMembershipDefault() {
    return getProgrammeMembershipWithOneCurriculum(
        PROGRAMME_TIS_ID, PROGRAMME_MEMBERSHIP_TYPE, START_DATE,
        END_DATE, MANAGING_DEANERY, MEDICAL_CURRICULA.get(0), CURRICULUM_SPECIALTY_CODE);
  }
}
