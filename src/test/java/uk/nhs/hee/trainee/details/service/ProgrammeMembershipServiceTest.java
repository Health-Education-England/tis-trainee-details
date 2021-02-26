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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.nhs.hee.trainee.details.mapper.ProgrammeMembershipMapper;
import uk.nhs.hee.trainee.details.model.Curriculum;
import uk.nhs.hee.trainee.details.model.CurriculumMembership;
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
  private static final String CURRICULUM_NAME = "curriculum";
  private static final String CURRICULUM_SUBTYPE = "curriculumSubtype";
  private static final String CURRICULUM_TIS_ID = "curriculumTisId";
  private static final String CURRICULUM_MEMBERSHIP_TIS_ID = "curriculumMembershipTisId";

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
            createProgrammeMembership(NEW_PROGRAMME_MEMBERSHIP_ID, MODIFIED_SUFFIX, 100, 0));

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

    assertThat("Unexpected programme membership.", programmeMembership.get(),
        is(expectedProgrammeMembership));
  }

  @Test
  void shouldAddProgrammeMembershipWhenTraineeFoundAndProgrammeMembershipNotExists() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<ProgrammeMembership> programmeMembership = service
        .updateProgrammeMembershipForTrainee(TRAINEE_TIS_ID,
            createProgrammeMembership(NEW_PROGRAMME_MEMBERSHIP_ID, MODIFIED_SUFFIX, 100, 0));

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

    assertThat("Unexpected programme membership.", programmeMembership.get(),
        is(expectedProgrammeMembership));
  }

  @Test
  void shouldUpdateProgrammeMembershipWhenTraineeFoundAndProgrammeMembershipExists() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<ProgrammeMembership> programmeMembership = service
        .updateProgrammeMembershipForTrainee(TRAINEE_TIS_ID,
            createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_ID, MODIFIED_SUFFIX, 100, 0));

    assertThat("Unexpected optional isEmpty flag.", programmeMembership.isEmpty(), is(false));

    ProgrammeMembership expectedProgrammeMembership = createProgrammeMembership(
        EXISTING_PROGRAMME_MEMBERSHIP_ID,
        ORIGINAL_SUFFIX, 0, 0);
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

    assertThat("Unexpected programme membership.", programmeMembership.get(),
        is(expectedProgrammeMembership));
  }


  @Test
  void shouldNotUpdateCurriculumMembershipWhenTraineeIdNotFound() {
    Optional<ProgrammeMembership> programmeMembership = service
        .updateProgrammeMembershipForTrainee("notFound", new ProgrammeMembership());

    assertThat("Unexpected optional isEmpty flag.", programmeMembership.isEmpty(), is(true));
    verify(repository).findByTraineeTisId("notFound");
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldUpdateCurriculumMembershipWhenExisting() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0, 2));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    ProgrammeMembership input =
        createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_ID, MODIFIED_SUFFIX, 100, 0);
    Curriculum modifiedCurriculum = new Curriculum();
    CurriculumMembership modifiedCurriculumMembersip = createCurriculumMembership(
        ORIGINAL_SUFFIX + 2, 100);
    modifiedCurriculumMembersip.setCurriculum(modifiedCurriculum);
    input.getCurriculumMemberships().add(modifiedCurriculumMembersip);
    Optional<ProgrammeMembership> programmeMembership = service
        .updateCurriculumMembershipForTrainee(TRAINEE_TIS_ID, input);

    assertThat("Unexpected optional isEmpty flag.", programmeMembership.isEmpty(), is(false));

    ProgrammeMembership actual = programmeMembership.get();
    assertEquals(EXISTING_PROGRAMME_MEMBERSHIP_ID, actual.getTisId());
    assertEquals(PROGRAMME_TIS_ID + MODIFIED_SUFFIX, actual.getProgrammeTisId());
    assertEquals(PROGRAMME_NAME + MODIFIED_SUFFIX, actual.getProgrammeName());
    assertEquals(PROGRAMME_NUMBER + MODIFIED_SUFFIX, actual.getProgrammeNumber());
    assertEquals(MANAGING_DEANERY + MODIFIED_SUFFIX, actual.getManagingDeanery());
    assertEquals(PROGRAMME_MEMBERSHIP_TYPE + MODIFIED_SUFFIX, actual.getProgrammeMembershipType());
    assertEquals(START_DATE.plusDays(100), actual.getStartDate());
    assertEquals(END_DATE.plusDays(100), actual.getEndDate());
    assertEquals(COMPLETION_DATE.plusDays(100), actual.getProgrammeCompletionDate());
    assertEquals(2, actual.getCurriculumMemberships().size());
    Optional<CurriculumMembership> curriculumMembershipOptional = actual.getCurriculumMemberships()
        .stream().filter(c ->
            c.getCurriculumMembershipTisId()
                .equals(CURRICULUM_MEMBERSHIP_TIS_ID + ORIGINAL_SUFFIX + 2))
        .findAny();
    assertThat(curriculumMembershipOptional.isPresent(), is(true));
    assertEquals(modifiedCurriculumMembersip, curriculumMembershipOptional.get());
  }

  @Test
  void shouldAppendCurriculumMembershipWhenProgrammeMembershipExists() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getProgrammeMemberships()
        .add(createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_ID, ORIGINAL_SUFFIX, 0, 1));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    ProgrammeMembership input =
        createProgrammeMembership(EXISTING_PROGRAMME_MEMBERSHIP_ID, MODIFIED_SUFFIX, 100, 0);
    input.getCurriculumMemberships().add(createCurriculumMembership(MODIFIED_SUFFIX + 2, 100));
    Optional<ProgrammeMembership> programmeMembership = service
        .updateCurriculumMembershipForTrainee(TRAINEE_TIS_ID, input);

    assertThat("Unexpected optional isEmpty flag.", programmeMembership.isEmpty(), is(false));

    ProgrammeMembership actual = programmeMembership.get();
    assertEquals(EXISTING_PROGRAMME_MEMBERSHIP_ID, actual.getTisId());
    assertEquals(PROGRAMME_TIS_ID + MODIFIED_SUFFIX, actual.getProgrammeTisId());
    assertEquals(PROGRAMME_NAME + MODIFIED_SUFFIX, actual.getProgrammeName());
    assertEquals(PROGRAMME_NUMBER + MODIFIED_SUFFIX, actual.getProgrammeNumber());
    assertEquals(MANAGING_DEANERY + MODIFIED_SUFFIX, actual.getManagingDeanery());
    assertEquals(PROGRAMME_MEMBERSHIP_TYPE + MODIFIED_SUFFIX, actual.getProgrammeMembershipType());
    assertEquals(START_DATE.plusDays(100), actual.getStartDate());
    assertEquals(END_DATE.plusDays(100), actual.getEndDate());
    assertEquals(COMPLETION_DATE.plusDays(100), actual.getProgrammeCompletionDate());
    assertEquals(2, actual.getCurriculumMemberships().size());
  }

  @Test
  void shouldAddProgrammeMembershipAndCurriculumWhenTraineeFoundWithoutProgrammeMemberships() {
    TraineeProfile traineeProfile = new TraineeProfile();

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<ProgrammeMembership> programmeMembership = service
        .updateCurriculumMembershipForTrainee(TRAINEE_TIS_ID,
            createProgrammeMembership(NEW_PROGRAMME_MEMBERSHIP_ID, MODIFIED_SUFFIX, 100, 1));

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
    expectedProgrammeMembership.setCurriculumMemberships(
        Collections.singletonList(createCurriculumMembership(MODIFIED_SUFFIX + 1, 100)));
    assertThat("Unexpected programme membership.", programmeMembership.get(),
        is(expectedProgrammeMembership));
  }

  /**
   * Create an instance of ProgrammeMembership with default dummy values.
   *
   * @param tisId              The TIS ID to set on the programmeMembership.
   * @param stringSuffix       The suffix to use for string values.
   * @param dateAdjustmentDays The number of days to add to dates.
   * @param curricula
   * @return The dummy entity.
   */
  private ProgrammeMembership createProgrammeMembership(String tisId, String stringSuffix,
      int dateAdjustmentDays, int curricula) {
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
    List<CurriculumMembership> curriculumMemberships = new ArrayList<>();
    for (int i = 1; i <= curricula; i++) {
      CurriculumMembership curriculumMembership = createCurriculumMembership(stringSuffix + i,
          dateAdjustmentDays);
      curriculumMemberships.add(curriculumMembership);
    }
    programmeMembership.setCurriculumMemberships(curriculumMemberships);
    return programmeMembership;
  }

  private CurriculumMembership createCurriculumMembership(String stringSuffix, int dateAdjustment) {
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumTisId(CURRICULUM_TIS_ID + stringSuffix);
    curriculum.setCurriculumName(CURRICULUM_NAME + stringSuffix);
    curriculum.setCurriculumSubType(CURRICULUM_SUBTYPE + stringSuffix);
    CurriculumMembership curriculumMembership = new CurriculumMembership();
    curriculumMembership.setCurriculumMembershipTisId(CURRICULUM_MEMBERSHIP_TIS_ID + stringSuffix);
    curriculumMembership.setCurriculumStartDate(START_DATE.plusDays(dateAdjustment));
    curriculumMembership.setCurriculumEndDate(END_DATE.plusDays(dateAdjustment));
    curriculumMembership.setCurriculum(curriculum);
    return curriculumMembership;
  }
}
