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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
  private static final String NEW_PROGRAMME_MEMBERSHIP_ID = "1";
  private static final String EXISTING_PROGRAMME_MEMBERSHIP_ID = "2";
  private static final String MULTIPLE_PROGRAMME_MEMBERSHIP_ID = "123,456,789";
  private static final UUID PROGRAMME_MEMBERSHIP_UUID = UUID.randomUUID();
  private static final Instant COJ_SIGNED_AT = Instant.now();
  private static final GoldGuideVersion GOLD_GUIDE_VERSION = GoldGuideVersion.GG9;

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
    programmeMembership.setTisId(EXISTING_PROGRAMME_MEMBERSHIP_ID);
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
            createProgrammeMembership(NEW_PROGRAMME_MEMBERSHIP_ID, MODIFIED_SUFFIX, 100));

    assertThat("Unexpected optional isEmpty flag.", programmeMembership.isEmpty(), is(false));

    ProgrammeMembership expectedProgrammeMembership = new ProgrammeMembership();
    expectedProgrammeMembership.setTisId(NEW_PROGRAMME_MEMBERSHIP_ID);
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
        new ConditionsOfJoining(COJ_SIGNED_AT.plus(Duration.ofDays(100)), GOLD_GUIDE_VERSION));

    assertThat("Unexpected programme membership.", programmeMembership.get(),
        is(expectedProgrammeMembership));
  }

  @Test
  void shouldAddProgrammeMembershipWhenTraineeFoundAndProgrammeMembershipNotExists() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    Optional<ProgrammeMembership> programmeMembership = service
        .updateProgrammeMembershipForTrainee(TRAINEE_TIS_ID,
            createProgrammeMembership(NEW_PROGRAMME_MEMBERSHIP_ID, MODIFIED_SUFFIX, 100));

    assertThat("Unexpected optional isEmpty flag.", programmeMembership.isEmpty(), is(false));

    ProgrammeMembership expectedProgrammeMembership = new ProgrammeMembership();
    expectedProgrammeMembership.setTisId(NEW_PROGRAMME_MEMBERSHIP_ID);
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
        new ConditionsOfJoining(COJ_SIGNED_AT.plus(Duration.ofDays(100)), GOLD_GUIDE_VERSION));

    assertThat("Unexpected programme membership.", programmeMembership.get(),
        is(expectedProgrammeMembership));
  }

  @Test
  void shouldUpdateProgrammeMembershipWhenTraineeFoundAndProgrammeMembershipExists() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    Optional<ProgrammeMembership> programmeMembership = service
        .updateProgrammeMembershipForTrainee(TRAINEE_TIS_ID,
            createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_ID, MODIFIED_SUFFIX, 100));

    assertThat("Unexpected optional isEmpty flag.", programmeMembership.isEmpty(), is(false));

    ProgrammeMembership expectedProgrammeMembership = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_ID,
        ORIGINAL_SUFFIX, 0);
    expectedProgrammeMembership.setTisId(EXISTING_PROGRAMME_MEMBERSHIP_ID);
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
        new ConditionsOfJoining(COJ_SIGNED_AT.plus(Duration.ofDays(100)), GOLD_GUIDE_VERSION));

    assertThat("Unexpected programme membership.", programmeMembership.get(),
        is(expectedProgrammeMembership));
  }

  @Test
  void shouldNotUpdateExistingConditionsOfJoiningWhenNewCojNull() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    ProgrammeMembership newProgrammeMembership = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_ID, MODIFIED_SUFFIX, 100);
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
  }

  @Test
  void shouldUpdateProgrammeMembershipCojWhenPmCojNullAndCojCached() {
    ProgrammeMembership existingProgrammeMembership = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0);

    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships().add(existingProgrammeMembership);

    ConditionsOfJoining coj = existingProgrammeMembership.getConditionsOfJoining();
    existingProgrammeMembership.setConditionsOfJoining(null);

    when(cachingDelegate.getConditionsOfJoining(EXISTING_PROGRAMME_MEMBERSHIP_ID)).thenReturn(
        Optional.of(coj));
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    ProgrammeMembership newProgrammeMembership = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_ID, MODIFIED_SUFFIX, 100);
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
  }

  @Test
  void shouldUpdateProgrammeMembershipCojWhenPmCojNotSignedAndCojCached() {
    ProgrammeMembership existingProgrammeMembership = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0);

    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships().add(existingProgrammeMembership);

    ConditionsOfJoining coj = existingProgrammeMembership.getConditionsOfJoining();
    existingProgrammeMembership.setConditionsOfJoining(
        new ConditionsOfJoining(null, GoldGuideVersion.GG9));

    when(cachingDelegate.getConditionsOfJoining(EXISTING_PROGRAMME_MEMBERSHIP_ID)).thenReturn(
        Optional.of(coj));
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    ProgrammeMembership newProgrammeMembership = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_ID, MODIFIED_SUFFIX, 100);
    newProgrammeMembership.setConditionsOfJoining(
        new ConditionsOfJoining(null, GoldGuideVersion.GG9));

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
  }

  @Test
  void shouldUpdateProgrammeMembershipCojWhenPmHasMultipleIds() {
    ProgrammeMembership programmeMembership = createProgrammeMembership(
        MULTIPLE_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0);
    programmeMembership.setConditionsOfJoining(null);
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships().add(programmeMembership);

    when(cachingDelegate.getConditionsOfJoining("123")).thenReturn(
        Optional.of(new ConditionsOfJoining(Instant.MIN, GoldGuideVersion.GG9)));
    when(cachingDelegate.getConditionsOfJoining("456")).thenReturn(
        Optional.of(new ConditionsOfJoining(Instant.MAX, GoldGuideVersion.GG9)));
    when(cachingDelegate.getConditionsOfJoining("789")).thenReturn(
        Optional.of(new ConditionsOfJoining(COJ_SIGNED_AT, GoldGuideVersion.GG9)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    Optional<ProgrammeMembership> optionalProgrammeMembership = service
        .updateProgrammeMembershipForTrainee(TRAINEE_TIS_ID, programmeMembership);

    assertThat("Unexpected optional isEmpty flag.", optionalProgrammeMembership.isEmpty(),
        is(false));
    ProgrammeMembership updatedProgrammeMembership = optionalProgrammeMembership.get();
    assertThat("Unexpected conditions of joining.",
        updatedProgrammeMembership.getConditionsOfJoining(), notNullValue());

    ConditionsOfJoining conditionsOfJoining = updatedProgrammeMembership.getConditionsOfJoining();
    assertThat("Unexpected signed at.", conditionsOfJoining.signedAt(), is(COJ_SIGNED_AT));
    assertThat("Unexpected signed version.", conditionsOfJoining.version(),
        is(GoldGuideVersion.GG9));

    verify(cachingDelegate, times(1)).getConditionsOfJoining("123");
    verify(cachingDelegate, times(1)).getConditionsOfJoining("456");
    verify(cachingDelegate, times(1)).getConditionsOfJoining("789");
  }

  @Test
  void shouldUpdateProgrammeMembershipCojWhenPmHasUuidAndCachedCojHasUuid() {
    ProgrammeMembership programmeMembership = createProgrammeMembership(
        PROGRAMME_MEMBERSHIP_UUID.toString(), ORIGINAL_SUFFIX, 0);
    programmeMembership.setConditionsOfJoining(null);
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships().add(programmeMembership);

    when(cachingDelegate.getConditionsOfJoining(PROGRAMME_MEMBERSHIP_UUID.toString())).thenReturn(
        Optional.of(new ConditionsOfJoining(COJ_SIGNED_AT, GoldGuideVersion.GG9)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    Optional<ProgrammeMembership> optionalProgrammeMembership = service
        .updateProgrammeMembershipForTrainee(TRAINEE_TIS_ID, programmeMembership);

    assertThat("Unexpected optional isEmpty flag.", optionalProgrammeMembership.isEmpty(),
        is(false));
    ProgrammeMembership updatedProgrammeMembership = optionalProgrammeMembership.get();
    assertThat("Unexpected conditions of joining.",
        updatedProgrammeMembership.getConditionsOfJoining(), notNullValue());

    ConditionsOfJoining conditionsOfJoining = updatedProgrammeMembership.getConditionsOfJoining();
    assertThat("Unexpected signed at.", conditionsOfJoining.signedAt(), is(COJ_SIGNED_AT));
    assertThat("Unexpected signed version.", conditionsOfJoining.version(),
        is(GoldGuideVersion.GG9));

    verify(cachingDelegate, times(1))
        .getConditionsOfJoining(PROGRAMME_MEMBERSHIP_UUID.toString());
  }

  @Test
  void shouldUpdateProgrammeMembershipCojWhenPmHasUuidAndCachedCojHasIds() {
    Curriculum curriculum = new Curriculum();
    curriculum.setTisId("123");
    ProgrammeMembership programmeMembership = createProgrammeMembership(
        PROGRAMME_MEMBERSHIP_UUID.toString(), ORIGINAL_SUFFIX, 0);
    programmeMembership.setConditionsOfJoining(null);
    programmeMembership.setCurricula(new ArrayList<>(Arrays.asList(curriculum)));
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships().add(programmeMembership);

    when(cachingDelegate.getConditionsOfJoining(PROGRAMME_MEMBERSHIP_UUID.toString())).thenReturn(
        Optional.empty());
    when(cachingDelegate.getConditionsOfJoining("123")).thenReturn(
        Optional.of(new ConditionsOfJoining(COJ_SIGNED_AT, GoldGuideVersion.GG9)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    Optional<ProgrammeMembership> optionalProgrammeMembership = service
        .updateProgrammeMembershipForTrainee(TRAINEE_TIS_ID, programmeMembership);

    assertThat("Unexpected optional isEmpty flag.", optionalProgrammeMembership.isEmpty(),
        is(false));
    ProgrammeMembership updatedProgrammeMembership = optionalProgrammeMembership.get();
    assertThat("Unexpected conditions of joining.",
        updatedProgrammeMembership.getConditionsOfJoining(), notNullValue());

    ConditionsOfJoining conditionsOfJoining = updatedProgrammeMembership.getConditionsOfJoining();
    assertThat("Unexpected signed at.", conditionsOfJoining.signedAt(), is(COJ_SIGNED_AT));
    assertThat("Unexpected signed version.", conditionsOfJoining.version(),
        is(GoldGuideVersion.GG9));

    verify(cachingDelegate, times(1))
        .getConditionsOfJoining(PROGRAMME_MEMBERSHIP_UUID.toString());
    verify(cachingDelegate, times(1))
        .getConditionsOfJoining("123");
  }

  @Test
  void shouldNotUpdateProgrammeMembershipCojWhenCojAlreadySigned() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships().add(createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0));

    when(cachingDelegate.getConditionsOfJoining(EXISTING_PROGRAMME_MEMBERSHIP_ID)).thenReturn(
        Optional.of(new ConditionsOfJoining(Instant.MAX, GoldGuideVersion.GG9)));
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    ProgrammeMembership newProgrammeMembership = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_ID, MODIFIED_SUFFIX, 100);

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

    verifyNoInteractions(cachingDelegate);
  }

  @Test
  void shouldDeleteProgrammeMembershipsWhenTraineeFoundAndProgrammeMembershipsExist() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0));

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
        .add(createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    service.deleteProgrammeMembershipsForTrainee(TRAINEE_TIS_ID);

    verify(cachingDelegate).cacheConditionsOfJoining(any(), any());
  }

  @Test
  void shouldCacheCojFromDeleteProgrammeMembershipsWhenCojSignedForMultipleCurriculum() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(MULTIPLE_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    service.deleteProgrammeMembershipsForTrainee(TRAINEE_TIS_ID);

    verify(cachingDelegate, times(1)).cacheConditionsOfJoining(eq("123"), any());
    verify(cachingDelegate, times(1)).cacheConditionsOfJoining(eq("456"), any());
    verify(cachingDelegate, times(1)).cacheConditionsOfJoining(eq("789"), any());
  }

  @Test
  void shouldCacheCojFromDeleteProgrammeMembershipsWhenCojSignedUsingUuid() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(PROGRAMME_MEMBERSHIP_UUID.toString(), ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    service.deleteProgrammeMembershipsForTrainee(TRAINEE_TIS_ID);

    verify(cachingDelegate, times(1))
        .cacheConditionsOfJoining(eq(PROGRAMME_MEMBERSHIP_UUID.toString()), any());
  }

  @Test
  void shouldNotCacheCojFromDeleteProgrammeMembershipsWhenCojNotSigned() {
    ProgrammeMembership programmeMembership = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0);
    programmeMembership.setConditionsOfJoining(new ConditionsOfJoining(null, GoldGuideVersion.GG9));

    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships().add(programmeMembership);

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    service.deleteProgrammeMembershipsForTrainee(TRAINEE_TIS_ID);

    verifyNoInteractions(cachingDelegate);
  }

  @Test
  void shouldNotCacheCojFromDeleteProgrammeMembershipsWhenCojNull() {
    ProgrammeMembership programmeMembership = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0);
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
        NEW_PROGRAMME_MEMBERSHIP_ID);

    assertThat("Unexpected result.", deleted, is(false));
    verify(repository, never()).save(any());
  }

  @Test
  void shouldNotDeleteProgrammeMembershipWhenTraineesProgrammeMembershipNotFound() {
    ProgrammeMembership programmeMembership = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0);

    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships().add(programmeMembership);

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean deleted = service.deleteProgrammeMembershipForTrainee(TRAINEE_TIS_ID,
        NEW_PROGRAMME_MEMBERSHIP_ID);

    assertThat("Unexpected result.", deleted, is(false));
    verify(repository, never()).save(any());
  }

  @Test
  void shouldDeleteProgrammeMembershipWhenTraineeFoundAndProgrammeMembershipExists() {
    ProgrammeMembership programmeMembership1 = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0);
    ProgrammeMembership programmeMembership2 = createProgrammeMembership(
        "unrelatedPm", "unrelatedPm", 1);

    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .addAll(List.of(programmeMembership1, programmeMembership2));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean deleted = service.deleteProgrammeMembershipForTrainee(TRAINEE_TIS_ID,
        EXISTING_PROGRAMME_MEMBERSHIP_ID);

    assertThat("Unexpected result.", deleted, is(true));

    ArgumentCaptor<TraineeProfile> profileCaptor = ArgumentCaptor.forClass(TraineeProfile.class);
    verify(repository).save(profileCaptor.capture());

    List<ProgrammeMembership> programmeMemberships = profileCaptor.getValue()
        .getProgrammeMemberships();
    assertThat("Unexpected programme membership count.", programmeMemberships.size(), is(1));

    ProgrammeMembership remainingProgrammeMembership = programmeMemberships.get(0);
    assertThat("Unexpected programme membership id.", remainingProgrammeMembership.getTisId(),
        is("unrelatedPm"));
  }

  @Test
  void shouldSignCojWhenTraineeProgrammeMembershipFound() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0));
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership("3", ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<ProgrammeMembership> programmeMembership = service
        .signProgrammeMembershipCoj(TRAINEE_TIS_ID, EXISTING_PROGRAMME_MEMBERSHIP_ID);

    assertThat("Unexpected optional isEmpty flag.", programmeMembership.isEmpty(), is(false));
    assertThat("Unexpected COJ signedAt.",
        programmeMembership.get().getConditionsOfJoining().signedAt(),
        notNullValue());
    assertThat("Unexpected COJ version.",
        programmeMembership.get().getConditionsOfJoining().version(),
        is(GoldGuideVersion.getLatest()));
  }

  @Test
  void shouldNotSignCojWhenTraineeProfileNotFound() {
    Optional<ProgrammeMembership> programmeMembership = service
        .signProgrammeMembershipCoj("randomId", EXISTING_PROGRAMME_MEMBERSHIP_ID);

    assertThat("Unexpected optional isEmpty flag.", programmeMembership.isEmpty(), is(true));
    verify(repository).findByTraineeTisId("randomId");
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotSignCojWhenTraineeFoundButProgrammeMembershipsNotExists() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    Optional<ProgrammeMembership> programmeMembership = service
        .signProgrammeMembershipCoj(TRAINEE_TIS_ID, "randomPmId");

    assertThat("Unexpected optional isEmpty flag.", programmeMembership.isEmpty(), is(true));
    verify(repository, never()).save(traineeProfile);
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
            GOLD_GUIDE_VERSION));

    return programmeMembership;
  }
}
