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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.nhs.hee.trainee.details.dto.enumeration.GoldGuideVersion;
import uk.nhs.hee.trainee.details.mapper.ProgrammeMembershipMapper;
import uk.nhs.hee.trainee.details.model.ConditionsOfJoining;
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
  private static final String NOT_EXISTING_PROGRAMME_MEMBERSHIP_ID = "3";
  private static final Instant COJ_SIGNED_AT = Instant.now();
  private static final GoldGuideVersion GOLD_GUIDE_VERSION = GoldGuideVersion.GG9;

  private ProgrammeMembershipService service;
  private TraineeProfileRepository repository;

  @BeforeEach
  void setUp() {
    repository = mock(TraineeProfileRepository.class);
    service = new ProgrammeMembershipService(repository,
        Mappers.getMapper(ProgrammeMembershipMapper.class));
  }

  @Test
  void shouldNotUpdateProgrammeMembershipWhenTraineeIdNotFound() {
    Optional<ProgrammeMembership> programmeMembership = service
        .updateProgrammeMembershipForTrainee("notFound", new ProgrammeMembership());

    assertThat("Unexpected optional isEmpty flag.", programmeMembership.isEmpty(), is(true));
    verify(repository).findByTraineeTisId("notFound");
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldAddProgrammeMembershipWhenTraineeFoundAndNoProgrammeMembershipsExists() {
    TraineeProfile traineeProfile = new TraineeProfile();

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

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
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

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
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

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
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

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
  void shouldDeleteProgrammeMembershipWhenTraineeFoundAndProgrammeMembershipExists() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean result = service.deleteProgrammeMembershipsForTrainee(TRAINEE_TIS_ID);

    assertThat("Unexpected result.", result, is(true));
  }

  @Test
  void shouldNotDeleteProgrammeMembershipWhenTraineeNotFound() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(null);

    boolean result = service.deleteProgrammeMembershipsForTrainee(TRAINEE_TIS_ID);

    assertThat("Unexpected result.", result, is(false));
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
