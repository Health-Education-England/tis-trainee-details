/*
 * The MIT License (MIT)
 *
 * Copyright 2025 Crown Copyright (Health Education England)
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.nhs.hee.trainee.details.config.FeaturesProperties;
import uk.nhs.hee.trainee.details.dto.FeaturesDto;
import uk.nhs.hee.trainee.details.dto.TraineeIdentity;
import uk.nhs.hee.trainee.details.model.Curriculum;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;

class FeatureServiceTest {

  private static final String TRAINEE_ID = UUID.randomUUID().toString();

  private FeatureService service;
  private TraineeProfileService profileService;

  @BeforeEach
  void setUp() {
    TraineeIdentity traineeIdentity = new TraineeIdentity();
    traineeIdentity.setTraineeId(TRAINEE_ID);

    profileService = mock(TraineeProfileService.class);
    FeaturesProperties featuresProperties = FeaturesProperties.builder()
        .foundationDeaneries(Set.of("South West"))
        .publicHealthDeaneries(Set.of("test 1", "test 2", "test 3"))
        .specialtyDeaneries(Set.of("test 1", "test 2", "test 3", "specialty only"))
        .build();

    service = new FeatureService(traineeIdentity, profileService, featuresProperties,
        ZoneId.of("UTC"));
  }

  @Test
  void shouldDisableActionsWhenProfileNotFound() {
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(null);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.actions().enabled(), is(false));
  }

  @Test
  void shouldDisableActionsWhenNoProgrammes() {
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(
        new TraineeProfile());

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.actions().enabled(), is(false));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      UNKNOWN            | General Practice
      ""                 | ""
      MEDICAL_SPR        |
                         |
      """)
  void shouldDisableActionsWhenNoSpecialtyProgrammes(String subType, String specialty) {
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType(subType);
    curriculum.setCurriculumSpecialty(specialty);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setEndDate(LocalDate.MAX);

    TraineeProfile profile = new TraineeProfile();
    profile.setProgrammeMemberships(List.of(pm));

    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.actions().enabled(), is(false));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      MEDICAL_CURRICULUM | GENERAL PRACTICE       | DUMMY
      medical_curriculum | general practice       | DUMMY
      MEDICAL_SPR        | GENERAL PRACTICE       | DUMMY
      medical_spr        | general practice       | DUMMY
      MEDICAL_CURRICULUM | PUBLIC HEALTH MEDICINE | DUMMY
      medical_curriculum | public health medicine | DUMMY
      MEDICAL_SPR        | PUBLIC HEALTH MEDICINE | DUMMY
      medical_spr        | public health medicine | DUMMY
      AFT                | ACADEMIC               | ACADEMIC FOUNDATION TRAINING
      aft                |                        | DUMMY
      MEDICAL_CURRICULUM | FOUNDATION             | DUMMY
      medical_curriculum | foundation             |
      """)
  void shouldEnableActionsWhenPublicHealthOrSpecialtyOrFoundationProgrammeFound(String subType,
      String specialty, String curriculumName) {
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumName(curriculumName);
    curriculum.setCurriculumSubType(subType);
    curriculum.setCurriculumSpecialty(specialty);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setEndDate(LocalDate.MAX);

    TraineeProfile profile = new TraineeProfile();
    profile.setProgrammeMemberships(List.of(pm));

    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.actions().enabled(), is(true));
  }

  @Test
  void shouldDisableCctWhenProfileNotFound() {
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(null);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.cct().enabled(), is(false));
  }

  @Test
  void shouldDisableCctWhenNoProgrammes() {
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(
        new TraineeProfile());

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.cct().enabled(), is(false));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      UNKNOWN            | General Practice
      ""                 | ""
      MEDICAL_SPR        |
                         |
      """)
  void shouldDisableCctWhenNoSpecialtyProgrammes(String subType, String specialty) {
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType(subType);
    curriculum.setCurriculumSpecialty(specialty);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setEndDate(LocalDate.MAX);

    TraineeProfile profile = new TraineeProfile();
    profile.setProgrammeMemberships(List.of(pm));

    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.cct().enabled(), is(false));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      MEDICAL_CURRICULUM | GENERAL PRACTICE       | DUMMY
      medical_curriculum | general practice       | DUMMY
      MEDICAL_SPR        | GENERAL PRACTICE       | DUMMY
      medical_spr        | general practice       | DUMMY
      MEDICAL_CURRICULUM | PUBLIC HEALTH MEDICINE | DUMMY
      medical_curriculum | public health medicine | DUMMY
      MEDICAL_SPR        | PUBLIC HEALTH MEDICINE | DUMMY
      medical_spr        | public health medicine | DUMMY
      AFT                | ACADEMIC               | ACADEMIC FOUNDATION TRAINING
      aft                |                        | DUMMY
      MEDICAL_CURRICULUM | FOUNDATION             | DUMMY
      medical_curriculum | foundation             |
      """)
  void shouldEnableCctWhenPublicHealthOrSpecialtyOrFoundationProgrammeFound(
      String subType, String specialty, String curriculumName) {
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumName(curriculumName);
    curriculum.setCurriculumSubType(subType);
    curriculum.setCurriculumSpecialty(specialty);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setEndDate(LocalDate.MAX);

    TraineeProfile profile = new TraineeProfile();
    profile.setProgrammeMemberships(List.of(pm));

    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.cct().enabled(), is(true));
  }

  @Test
  void shouldDisableDetailsWhenProfileNotFound() {
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(null);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.details().enabled(), is(false));
    assertThat("Unexpected feature flag.", features.details().placements().enabled(), is(false));
    assertThat("Unexpected feature flag.", features.details().profile().enabled(), is(false));
    assertThat("Unexpected feature flag.", features.details().profile().gmcUpdate().enabled(),
        is(false));
    assertThat("Unexpected feature flag.", features.details().programmes().enabled(), is(false));
    assertThat("Unexpected feature flag.",
        features.details().programmes().conditionsOfJoining().enabled(), is(false));
    assertThat("Unexpected feature flag.", features.details().programmes().confirmation().enabled(),
        is(false));
  }

  @Test
  void shouldEnableReadOnlyDetailsWhenNoProgrammes() {
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(
        new TraineeProfile());

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.details().enabled(), is(true));
    assertThat("Unexpected feature flag.", features.details().placements().enabled(), is(true));
    assertThat("Unexpected feature flag.", features.details().profile().enabled(), is(true));
    assertThat("Unexpected feature flag.", features.details().profile().gmcUpdate().enabled(),
        is(false));
    assertThat("Unexpected feature flag.", features.details().programmes().enabled(), is(true));
    assertThat("Unexpected feature flag.",
        features.details().programmes().conditionsOfJoining().enabled(), is(false));
    assertThat("Unexpected feature flag.", features.details().programmes().confirmation().enabled(),
        is(false));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      UNKNOWN            | General Practice
      ""                 | ""
      MEDICAL_SPR        |
                         |
      """)
  void shouldEnableReadOnlyDetailsWhenNoSpecialtyProgrammes(String subType, String specialty) {
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType(subType);
    curriculum.setCurriculumSpecialty(specialty);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setEndDate(LocalDate.MAX);

    TraineeProfile profile = new TraineeProfile();
    profile.setProgrammeMemberships(List.of(pm));

    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.details().enabled(), is(true));
    assertThat("Unexpected feature flag.", features.details().placements().enabled(), is(true));
    assertThat("Unexpected feature flag.", features.details().profile().enabled(), is(true));
    assertThat("Unexpected feature flag.", features.details().profile().gmcUpdate().enabled(),
        is(false));
    assertThat("Unexpected feature flag.", features.details().programmes().enabled(), is(true));
    assertThat("Unexpected feature flag.",
        features.details().programmes().conditionsOfJoining().enabled(), is(false));
    assertThat("Unexpected feature flag.", features.details().programmes().confirmation().enabled(),
        is(false));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      MEDICAL_CURRICULUM | GENERAL PRACTICE
      medical_curriculum | general practice
      MEDICAL_SPR        | GENERAL PRACTICE
      medical_spr        | general practice
      MEDICAL_CURRICULUM | PUBLIC HEALTH MEDICINE
      medical_curriculum | public health medicine
      MEDICAL_SPR        | PUBLIC HEALTH MEDICINE
      medical_spr        | public health medicine
      """)
  void shouldEnableDetailsWhenPublicHealthOrSpecialtyProgrammeFound(String subType,
      String specialty) {
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType(subType);
    curriculum.setCurriculumSpecialty(specialty);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setEndDate(LocalDate.MAX);

    TraineeProfile profile = new TraineeProfile();
    profile.setProgrammeMemberships(List.of(pm));

    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.details().enabled(), is(true));
    assertThat("Unexpected feature flag.", features.details().placements().enabled(), is(true));
    assertThat("Unexpected feature flag.", features.details().profile().enabled(), is(true));
    assertThat("Unexpected feature flag.", features.details().profile().gmcUpdate().enabled(),
        is(true));
    assertThat("Unexpected feature flag.", features.details().programmes().enabled(), is(true));
    assertThat("Unexpected feature flag.",
        features.details().programmes().conditionsOfJoining().enabled(), is(true));
    assertThat("Unexpected feature flag.", features.details().programmes().confirmation().enabled(),
        is(true));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      AFT                | ACADEMIC   | ACADEMIC FOUNDATION TRAINING
      aft                |            | DUMMY
      MEDICAL_CURRICULUM | FOUNDATION | DUMMY
      medical_curriculum | foundation |
      """)
  void shouldEnableDetailsButDisableCojWhenFoundationProgrammeFound(String subType,
      String specialty, String curriculumName) {
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumName(curriculumName);
    curriculum.setCurriculumSubType(subType);
    curriculum.setCurriculumSpecialty(specialty);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setEndDate(LocalDate.MAX);

    TraineeProfile profile = new TraineeProfile();
    profile.setProgrammeMemberships(List.of(pm));

    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.details().enabled(), is(true));
    assertThat("Unexpected feature flag.", features.details().placements().enabled(), is(true));
    assertThat("Unexpected feature flag.", features.details().profile().enabled(), is(true));
    assertThat("Unexpected feature flag.", features.details().profile().gmcUpdate().enabled(),
        is(true));
    assertThat("Unexpected feature flag.", features.details().programmes().enabled(), is(true));
    assertThat("Unexpected feature flag.",
        features.details().programmes().conditionsOfJoining().enabled(), is(false));
    assertThat("Unexpected feature flag.", features.details().programmes().confirmation().enabled(),
        is(true));
  }

  @Test
  void shouldDisableFormsWhenProfileNotFound() {
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(null);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.forms().enabled(), is(false));
    assertThat("Unexpected feature flag.", features.forms().formr().enabled(), is(false));
    assertThat("Unexpected feature flag.", features.forms().ltft().enabled(), is(false));
  }

  @Test
  void shouldDisableFormsWhenNoProgrammes() {
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(
        new TraineeProfile());

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.forms().enabled(), is(false));
    assertThat("Unexpected feature flag.", features.forms().formr().enabled(), is(false));
    assertThat("Unexpected feature flag.", features.forms().ltft().enabled(), is(false));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      AFT                | ACADEMIC         | ACADEMIC FOUNDATION TRAINING
      aft                |                  | DUMMY
      MEDICAL_CURRICULUM | FOUNDATION       | DUMMY
      medical_curriculum | foundation       |
      UNKNOWN            | General Practice | DUMMY
      ""                 | ""               | ""
      MEDICAL_SPR        |                  |
                         | FOUNDATION       |
                         |                  | DUMMY
                         |                  |
      """)
  void shouldDisableFormsWhenFoundationOrNoPublicHealthOrSpecialtyProgrammes(
      String subType, String specialty, String curriculumName)  {
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumName(curriculumName);
    curriculum.setCurriculumSubType(subType);
    curriculum.setCurriculumSpecialty(specialty);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setEndDate(LocalDate.MAX);

    TraineeProfile profile = new TraineeProfile();
    profile.setProgrammeMemberships(List.of(pm));

    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.forms().enabled(), is(false));
    assertThat("Unexpected feature flag.", features.forms().formr().enabled(), is(false));
    assertThat("Unexpected feature flag.", features.forms().ltft().enabled(), is(false));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      MEDICAL_CURRICULUM | GENERAL PRACTICE
      medical_curriculum | general practice
      MEDICAL_SPR        | GENERAL PRACTICE
      medical_spr        | general practice
      MEDICAL_CURRICULUM | PUBLIC HEALTH MEDICINE
      medical_curriculum | public health medicine
      MEDICAL_SPR        | PUBLIC HEALTH MEDICINE
      medical_spr        | public health medicine
      """)
  void shouldEnableFormRsWhenPublicHealthOrSpecialtyProgrammeFound(String subType,
      String specialty) {
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType(subType);
    curriculum.setCurriculumSpecialty(specialty);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setEndDate(LocalDate.MAX);

    TraineeProfile profile = new TraineeProfile();
    profile.setProgrammeMemberships(List.of(pm));

    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.forms().enabled(), is(true));
    assertThat("Unexpected feature flag.", features.forms().formr().enabled(), is(true));
    assertThat("Unexpected feature flag.", features.forms().ltft().enabled(), is(false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"MEDICAL_CURRICULUM", "medical_curriculum", "MEDICAL_SPR", "medical_spr"})
  void shouldEnableFormsWhenSpecialtyProgrammeFound(String subType) {
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType(subType);
    curriculum.setCurriculumSpecialty("General Practice");

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setEndDate(LocalDate.MAX);

    TraineeProfile profile = new TraineeProfile();
    profile.setProgrammeMemberships(List.of(pm));

    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.forms().enabled(), is(true));
    assertThat("Unexpected feature flag.", features.forms().formr().enabled(), is(true));
    assertThat("Unexpected feature flag.", features.forms().ltft().enabled(), is(false));
  }

  @Test
  void shouldDisableLtftAndEmptyLtftProgrammesWhenNoProfileFound() {
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(null);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(false));
    assertThat("Unexpected enabled programme count.",
        features.forms().ltft().qualifyingProgrammes().size(), is(0));
  }

  @Test
  void shouldDisableLtftWhenNoProgrammes() {
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    profile.setProgrammeMemberships(List.of());
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(false));
  }


  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      AFT                | ACADEMIC               | ACADEMIC FOUNDATION TRAINING
      aft                |                        | DUMMY
      MEDICAL_CURRICULUM | FOUNDATION             | DUMMY
      medical_curriculum | foundation             |
      UNKNOWN            | General Practice       | DUMMY
      ""                 | ""                     | ""
      MEDICAL_SPR        |                        |
                         | FOUNDATION             |
                         |                        | DUMMY
                         |                        |
      """)
  void shouldDisableLtftWhenFoundationOrNoSpecialtyProgrammes(
      String subType, String specialty, String curriculumName) {
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumName(curriculumName);
    curriculum.setCurriculumSubType(subType);
    curriculum.setCurriculumSpecialty(specialty);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setEndDate(LocalDate.MAX);

    TraineeProfile profile = new TraineeProfile();
    profile.setProgrammeMemberships(List.of(pm));

    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(false));
  }

  @Test
  void shouldDisableLtftWhenNoProgrammesInQualifyingDeanery() {
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType("MEDICAL_SPR");
    curriculum.setCurriculumSpecialty("General Practice");

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setManagingDeanery("None Qualifying");
    pm.setEndDate(LocalDate.now().plusDays(1));
    profile.setProgrammeMemberships(List.of(pm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"test 1", "test 2", "test 3"})
  void shouldDisableLtftWhenProgrammeInQualifyingDeaneryEnded(String deanery) {
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType("MEDICAL_SPR");
    curriculum.setCurriculumSpecialty("General Practice");

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setManagingDeanery(deanery);
    pm.setEndDate(LocalDate.now());
    profile.setProgrammeMemberships(List.of(pm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(false));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void shouldDisableLtftWhenNoSpecialtyDeaneriesConfigured(Set<String> specialtyDeaneries) {
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType("MEDICAL_SPR");
    curriculum.setCurriculumSpecialty("General Practice");

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setManagingDeanery("test 1");
    pm.setEndDate(LocalDate.now().plusDays(1));
    profile.setProgrammeMemberships(List.of(pm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    TraineeIdentity traineeIdentity = new TraineeIdentity();
    traineeIdentity.setTraineeId(TRAINEE_ID);

    FeaturesProperties featuresProperties = FeaturesProperties.builder()
        .publicHealthDeaneries(Set.of("test 1"))
        .foundationDeaneries(Set.of("test 1"))
        .specialtyDeaneries(specialtyDeaneries)
        .build();

    service = new FeatureService(traineeIdentity, profileService, featuresProperties,
        ZoneId.of("UTC"));

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(false));
    assertThat("Unexpected qualifying programmes.",
        features.forms().ltft().qualifyingProgrammes(), empty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"South West"})
  void shouldEnableLtftForCurrentProgrammesWhenFoundationTraineeInEnabledDeanery(String deanery) {
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType("MEDICAL_CURRICULUM");
    curriculum.setCurriculumSpecialty("Foundation");

    String pmId = UUID.randomUUID().toString();
    ProgrammeMembership currentPm = new ProgrammeMembership();
    currentPm.setCurricula(List.of(curriculum));
    currentPm.setTisId(pmId);
    currentPm.setManagingDeanery(deanery);
    currentPm.setEndDate(LocalDate.now().plusDays(1));

    ProgrammeMembership pastPm = new ProgrammeMembership();
    pastPm.setCurricula(List.of(curriculum));
    pastPm.setTisId(UUID.randomUUID().toString());
    pastPm.setManagingDeanery(deanery);
    pastPm.setEndDate(LocalDate.now().minusDays(1));

    profile.setProgrammeMemberships(List.of(currentPm, pastPm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(true));
    assertThat("Unexpected qualifying programmes.",
        features.forms().ltft().qualifyingProgrammes(), hasItem(pmId));
    assertThat("Unexpected FormR flag.",
        features.forms().formr().enabled(), is(false));
    assertThat("Unexpected COJ flag.",
        features.details().programmes().conditionsOfJoining().enabled(), is(false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"North West London", "Wessex", "Yorkshire and the Humber"})
  void shouldDisableLtftWhenFoundationTraineeNotInEnabledDeanery(String deanery) {
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType("MEDICAL_CURRICULUM");
    curriculum.setCurriculumSpecialty("Foundation");

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setTisId(UUID.randomUUID().toString());
    pm.setManagingDeanery(deanery);
    pm.setEndDate(LocalDate.now().plusDays(1));

    profile.setProgrammeMemberships(List.of(pm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(false));
    assertThat("Unexpected FormR flag.",
        features.forms().formr().enabled(), is(false));
    assertThat("Unexpected COJ flag.",
        features.details().programmes().conditionsOfJoining().enabled(), is(false));
  }

  @Test
  void shouldNotReturnPastProgrammesFromCurrentProgrammes() {
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType("MEDICAL_CURRICULUM");
    curriculum.setCurriculumSpecialty("Foundation");

    ProgrammeMembership endedToday = new ProgrammeMembership();
    endedToday.setCurricula(List.of(curriculum));
    endedToday.setTisId(UUID.randomUUID().toString());
    endedToday.setManagingDeanery("South West");
    endedToday.setEndDate(LocalDate.now());

    ProgrammeMembership endedYesterday = new ProgrammeMembership();
    endedYesterday.setCurricula(List.of(curriculum));
    endedYesterday.setTisId(UUID.randomUUID().toString());
    endedYesterday.setManagingDeanery("South West");
    endedYesterday.setEndDate(LocalDate.now().minusDays(1));

    ProgrammeMembership emptyDate = new ProgrammeMembership();
    emptyDate.setCurricula(List.of(curriculum));
    emptyDate.setTisId(UUID.randomUUID().toString());
    emptyDate.setManagingDeanery("South West");

    profile.setProgrammeMemberships(List.of(endedToday, endedYesterday, emptyDate));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT programme count.",
        features.forms().ltft().qualifyingProgrammes().size(), is(0));
  }

  @ParameterizedTest
  @ValueSource(strings = {"North West London", "Wessex", "Yorkshire and the Humber"})
  void shouldNotIncludeFoundationProgrammesNotInEnabledDeaneryWhenOtherInEnabledDeanery(
      String otherDeanery) {
    ProgrammeMembership enabledPm = createProgrammeMembership("MEDICAL_CURRICULUM", "Foundation",
        "South West", LocalDate.now().plusDays(1));
    ProgrammeMembership otherPm = createProgrammeMembership("MEDICAL_CURRICULUM", "Foundation",
        otherDeanery, LocalDate.now().plusDays(1));

    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    profile.setProgrammeMemberships(List.of(enabledPm, otherPm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(true));
    assertThat("Unexpected qualifying programmes.",
        features.forms().ltft().qualifyingProgrammes(), contains(enabledPm.getTisId()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"North West London", "Wessex", "Yorkshire and the Humber"})
  void shouldDisableLtftWhenOnlyPastFoundationProgrammeInEnabledDeanery(String otherDeanery) {
    ProgrammeMembership pastEnabledPm = createProgrammeMembership("MEDICAL_CURRICULUM",
        "Foundation", "South West", LocalDate.now().minusDays(1));
    ProgrammeMembership currentOtherPm = createProgrammeMembership("MEDICAL_CURRICULUM",
        "Foundation", otherDeanery, LocalDate.now().plusDays(1));

    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    profile.setProgrammeMemberships(List.of(pastEnabledPm, currentOtherPm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(false));
    assertThat("Unexpected qualifying programmes.",
        features.forms().ltft().qualifyingProgrammes(), empty());
  }

  @Test
  void shouldNotIncludeNonSpecialtyNonFoundationProgrammesForFoundationTrainee() {
    ProgrammeMembership foundationPm = createProgrammeMembership("MEDICAL_CURRICULUM",
        "Foundation", "South West", LocalDate.now().plusDays(1));
    ProgrammeMembership nonFoundationPm = createProgrammeMembership("UNKNOWN",
        "Some specialty", "South West", LocalDate.now().plusDays(1));

    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    profile.setProgrammeMemberships(List.of(foundationPm, nonFoundationPm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(true));
    assertThat("Unexpected qualifying programmes.",
        features.forms().ltft().qualifyingProgrammes(), contains(foundationPm.getTisId()));
  }

  @Test
  void shouldNotIncludeFoundationProgrammesWithNoDeaneryWhenOtherInEnabledDeanery() {
    ProgrammeMembership enabledPm = createProgrammeMembership("MEDICAL_CURRICULUM", "Foundation",
        "South West", LocalDate.now().plusDays(1));
    ProgrammeMembership noDeaneryPm = createProgrammeMembership("MEDICAL_CURRICULUM",
        "Foundation", null, LocalDate.now().plusDays(1));

    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    profile.setProgrammeMemberships(List.of(enabledPm, noDeaneryPm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected qualifying programmes.",
        features.forms().ltft().qualifyingProgrammes(), contains(enabledPm.getTisId()));
  }

  @Test
  void shouldIncludeAllCurrentFoundationProgrammesInEnabledDeanery() {
    ProgrammeMembership currentPm = createProgrammeMembership("MEDICAL_CURRICULUM", "Foundation",
        "South West", LocalDate.now().plusDays(1));
    ProgrammeMembership futurePm = createProgrammeMembership("AFT", null, "South West",
        LocalDate.now().plusYears(2));
    ProgrammeMembership pastPm = createProgrammeMembership("MEDICAL_CURRICULUM", "Foundation",
        "South West", LocalDate.now().minusDays(1));

    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    profile.setProgrammeMemberships(List.of(currentPm, futurePm, pastPm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(true));
    assertThat("Unexpected qualifying programmes.",
        features.forms().ltft().qualifyingProgrammes(),
        containsInAnyOrder(currentPm.getTisId(), futurePm.getTisId()));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void shouldDisableLtftForFoundationTraineeWhenNoFoundationDeaneriesConfigured(
      Set<String> foundationDeaneries) {
    ProgrammeMembership pm = createProgrammeMembership("MEDICAL_CURRICULUM", "Foundation",
        "South West", LocalDate.now().plusDays(1));

    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    profile.setProgrammeMemberships(List.of(pm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    TraineeIdentity traineeIdentity = new TraineeIdentity();
    traineeIdentity.setTraineeId(TRAINEE_ID);

    FeaturesProperties featuresProperties = FeaturesProperties.builder()
        .foundationDeaneries(foundationDeaneries)
        .publicHealthDeaneries(Set.of("South West"))
        .specialtyDeaneries(Set.of("South West"))
        .build();

    service = new FeatureService(traineeIdentity, profileService, featuresProperties,
        ZoneId.of("UTC"));

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(false));
    assertThat("Unexpected qualifying programmes.",
        features.forms().ltft().qualifyingProgrammes(), empty());
    assertThat("Unexpected features.", features,
        is(FeaturesDto.enableForFoundation(List.of())));
  }

  @ParameterizedTest
  @ValueSource(strings = {"test 1", "test 2", "test 3"})
  void shouldEnableLtftWhenProgrammeInQualifyingDeaneryNotEnded(String deanery) {
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType("MEDICAL_SPR");
    curriculum.setCurriculumSpecialty("General Practice");

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setTisId(UUID.randomUUID().toString());
    pm.setManagingDeanery(deanery);
    pm.setEndDate(LocalDate.now().plusDays(1));
    profile.setProgrammeMemberships(List.of(pm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"None Qualifying", "North West", "Yorkshire and the Humber"})
  void shouldEnableLtftWhenSpecialtyNotInEnabledDeaneryAndBetaUser(String deanery) {
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType("MEDICAL_SPR");
    curriculum.setCurriculumSpecialty("General Practice");

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setManagingDeanery(deanery);
    pm.setEndDate(LocalDate.now().plusDays(1));
    profile.setProgrammeMemberships(List.of(pm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    TraineeIdentity traineeIdentity = new TraineeIdentity();
    traineeIdentity.setTraineeId(TRAINEE_ID);
    traineeIdentity.setGroups(Set.of("beta-participant"));

    FeaturesProperties featuresProperties = FeaturesProperties.builder()
        .specialtyDeaneries(Set.of("test 1", "test 2", "test 3"))
        .build();

    service = new FeatureService(traineeIdentity, profileService, featuresProperties,
        ZoneId.of("UTC"));

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(true));
    assertThat("Unexpected qualifying programmes.",
        features.forms().ltft().qualifyingProgrammes(), empty());
  }

  @Test
  void shouldIncludeListOfLtftEnabledProgrammes() {
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType("MEDICAL_SPR");
    curriculum.setCurriculumSpecialty("General Practice");

    //ltft compliant
    String pm1Id = UUID.randomUUID().toString();
    ProgrammeMembership pm1 = new ProgrammeMembership();
    pm1.setCurricula(List.of(curriculum));
    pm1.setEndDate(LocalDate.now().plusDays(1));
    pm1.setManagingDeanery("test 1");
    pm1.setTisId(pm1Id);

    //finished
    String pm2Id = UUID.randomUUID().toString();
    ProgrammeMembership pm2 = new ProgrammeMembership();
    pm2.setCurricula(List.of(curriculum));
    pm2.setEndDate(LocalDate.now().minusDays(1));
    pm2.setManagingDeanery("test 1");
    pm2.setTisId(pm2Id);

    //wrong deanery
    String pm3Id = UUID.randomUUID().toString();
    ProgrammeMembership pm3 = new ProgrammeMembership();
    pm3.setCurricula(List.of(curriculum));
    pm3.setEndDate(LocalDate.now().plusDays(1));
    pm3.setManagingDeanery("not in list");
    pm3.setTisId(pm3Id);

    profile.setProgrammeMemberships(List.of(pm1, pm2, pm3));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    Set<String> qualifyingProgrammes = features.forms().ltft().qualifyingProgrammes();
    assertThat("Unexpected enabled programme count.", qualifyingProgrammes.size(), is(1));
    assertThat("Unexpected enabled programme ID.", qualifyingProgrammes, hasItem(pm1Id));
  }

  @Test
  void shouldNotIncludeLtftProgrammesWithNoEndDate() {
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType("MEDICAL_SPR");
    curriculum.setCurriculumSpecialty("General Practice");

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setTisId(UUID.randomUUID().toString());
    pm.setManagingDeanery("test 1");
    pm.setEndDate(null);
    profile.setProgrammeMemberships(List.of(pm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(false));
    assertThat("Unexpected LTFT programme count.",
        features.forms().ltft().qualifyingProgrammes().size(), is(0));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      MEDICAL_CURRICULUM | Public Health Medicine | test 1
      medical_curriculum | public health medicine | test 2
      MEDICAL_SPR        | PUBLIC HEALTH MEDICINE | test 3
      AFT                | Public Health Medicine | test 1
      """)
  void shouldEnableLtftWhenPublicHealthProgrammeInQualifyingDeaneryNotEnded(String subType,
      String specialty, String deanery) {
    ProgrammeMembership pm = createProgrammeMembership(subType, specialty, deanery,
        LocalDate.now().plusDays(1));

    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    profile.setProgrammeMemberships(List.of(pm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(true));
    Set<String> qualifyingProgrammes = features.forms().ltft().qualifyingProgrammes();
    assertThat("Unexpected LTFT programme count.", qualifyingProgrammes.size(), is(1));
    assertThat("Unexpected LTFT programme ID.", qualifyingProgrammes, hasItem(pm.getTisId()));
    assertThat("Unexpected forms flag.", features.forms().enabled(), is(true));
    assertThat("Unexpected FormR flag.", features.forms().formr().enabled(), is(true));
    assertThat("Unexpected COJ flag.",
        features.details().programmes().conditionsOfJoining().enabled(), is(true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"None Qualifying", "Wessex", "Yorkshire and the Humber"})
  void shouldDisableLtftWhenPublicHealthProgrammeNotInQualifyingDeanery(String deanery) {
    ProgrammeMembership pm = createProgrammeMembership("MEDICAL_CURRICULUM",
        "Public Health Medicine", deanery, LocalDate.now().plusDays(1));

    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    profile.setProgrammeMemberships(List.of(pm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(false));
    assertThat("Unexpected LTFT programme count.",
        features.forms().ltft().qualifyingProgrammes().size(), is(0));
    assertThat("Unexpected FormR flag.", features.forms().formr().enabled(), is(true));
    assertThat("Unexpected COJ flag.",
        features.details().programmes().conditionsOfJoining().enabled(), is(true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"test 1", "test 2", "test 3"})
  void shouldDisableLtftWhenPublicHealthProgrammeInQualifyingDeaneryEnded(String deanery) {
    ProgrammeMembership pm = createProgrammeMembership("MEDICAL_CURRICULUM",
        "Public Health Medicine", deanery, LocalDate.now());

    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    profile.setProgrammeMemberships(List.of(pm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(false));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void shouldDisableLtftWhenPublicHealthAndNoPublicHealthDeaneriesConfigured(
      Set<String> publicHealthDeaneries) {
    ProgrammeMembership pm = createProgrammeMembership("MEDICAL_CURRICULUM",
        "Public Health Medicine", "test 1", LocalDate.now().plusDays(1));

    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    profile.setProgrammeMemberships(List.of(pm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    TraineeIdentity traineeIdentity = new TraineeIdentity();
    traineeIdentity.setTraineeId(TRAINEE_ID);

    FeaturesProperties featuresProperties = FeaturesProperties.builder()
        .publicHealthDeaneries(publicHealthDeaneries)
        .specialtyDeaneries(Set.of("test 1"))
        .build();

    service = new FeatureService(traineeIdentity, profileService, featuresProperties,
        ZoneId.of("UTC"));

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(false));
    assertThat("Unexpected qualifying programmes.",
        features.forms().ltft().qualifyingProgrammes(), empty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"specialty only", "None Qualifying"})
  void shouldEnableLtftWhenPublicHealthNotInEnabledDeaneryAndBetaUser(String deanery) {
    ProgrammeMembership pm = createProgrammeMembership("MEDICAL_CURRICULUM",
        "Public Health Medicine", deanery, LocalDate.now().plusDays(1));

    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    profile.setProgrammeMemberships(List.of(pm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    TraineeIdentity traineeIdentity = new TraineeIdentity();
    traineeIdentity.setTraineeId(TRAINEE_ID);
    traineeIdentity.setGroups(Set.of("beta-participant"));

    FeaturesProperties featuresProperties = FeaturesProperties.builder()
        .publicHealthDeaneries(Set.of("test 1"))
        .specialtyDeaneries(Set.of("specialty only"))
        .build();

    service = new FeatureService(traineeIdentity, profileService, featuresProperties,
        ZoneId.of("UTC"));

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(true));
  }

  @Test
  void shouldDisableLtftWhenPublicHealthProgrammeInSpecialtyOnlyDeanery() {
    ProgrammeMembership pm = createProgrammeMembership("MEDICAL_CURRICULUM",
        "Public Health Medicine", "specialty only", LocalDate.now().plusDays(1));

    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    profile.setProgrammeMemberships(List.of(pm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(false));
    assertThat("Unexpected qualifying programmes.",
        features.forms().ltft().qualifyingProgrammes(), empty());
    assertThat("Unexpected FormR flag.", features.forms().formr().enabled(), is(true));
  }

  @Test
  void shouldEnableLtftWhenSpecialtyProgrammeInSpecialtyOnlyDeanery() {
    ProgrammeMembership pm = createProgrammeMembership("MEDICAL_SPR", "General Practice",
        "specialty only", LocalDate.now().plusDays(1));

    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    profile.setProgrammeMemberships(List.of(pm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(true));
    assertThat("Unexpected qualifying programmes.",
        features.forms().ltft().qualifyingProgrammes(), contains(pm.getTisId()));
  }

  @Test
  void shouldDisableLtftWhenFoundationProgrammeInSpecialtyOnlyDeanery() {
    ProgrammeMembership pm = createProgrammeMembership("MEDICAL_CURRICULUM", "Foundation",
        "specialty only", LocalDate.now().plusDays(1));

    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    profile.setProgrammeMemberships(List.of(pm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(false));
    assertThat("Unexpected qualifying programmes.",
        features.forms().ltft().qualifyingProgrammes(), empty());
  }

  /**
   * Pins current behaviour: a trainee with any specialty programme (past, current or future) is
   * classified as a Specialty trainee, and every not-ended programme in a Specialty-enabled deanery
   * qualifies, regardless of programme type. So Public Health and Foundation programmes in a
   * Specialty-only deanery (e.g. North East) qualify for these trainees.
   */
  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      Public Health Medicine | -365
      Public Health Medicine | 365
      Foundation             | -365
      Foundation             | 365
      """)
  void shouldIncludeNonSpecialtyProgrammeInSpecialtyOnlyDeaneryWhenSpecialtyTrainee(
      String otherSpecialty, int specialtyEndOffsetDays) {
    // A Public Health / Foundation programme in a Specialty-only deanery.
    ProgrammeMembership otherPm = createProgrammeMembership("MEDICAL_CURRICULUM", otherSpecialty,
        "specialty only", LocalDate.now().plusDays(1));

    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    // Without the Specialty programme: a Public Health / Foundation trainee, other PM excluded.
    profile.setProgrammeMemberships(List.of(otherPm));
    FeaturesDto withoutSpecialtyPm = service.getFeatures();

    assertThat("Unexpected LTFT flag without specialty programme.",
        withoutSpecialtyPm.forms().ltft().enabled(), is(false));
    assertThat("Unexpected qualifying programmes without specialty programme.",
        withoutSpecialtyPm.forms().ltft().qualifyingProgrammes(), empty());

    // A Specialty programme in a deanery with no LTFT at all, so it can never qualify itself. Its
    // only effect is to make the trainee a Specialty trainee.
    ProgrammeMembership specialtyPm = createProgrammeMembership("MEDICAL_SPR", "General Practice",
        "no LTFT", LocalDate.now().plusDays(specialtyEndOffsetDays));

    // With the Specialty programme: a Specialty trainee, other PM now included.
    profile.setProgrammeMemberships(List.of(specialtyPm, otherPm));
    FeaturesDto withSpecialtyPm = service.getFeatures();

    assertThat("Unexpected LTFT flag with specialty programme.",
        withSpecialtyPm.forms().ltft().enabled(), is(true));
    assertThat("Unexpected qualifying programmes with specialty programme.",
        withSpecialtyPm.forms().ltft().qualifyingProgrammes(), contains(otherPm.getTisId()));
    assertThat("Unexpected FormR flag with specialty programme.",
        withSpecialtyPm.forms().formr().enabled(), is(true));
    assertThat("Unexpected COJ flag with specialty programme.",
        withSpecialtyPm.details().programmes().conditionsOfJoining().enabled(), is(true));
  }

  @Test
  void shouldNotIncludePublicHealthProgrammesWithNoDeanery() {
    ProgrammeMembership enabledPm = createProgrammeMembership("MEDICAL_CURRICULUM",
        "Public Health Medicine", "test 1", LocalDate.now().plusDays(1));
    ProgrammeMembership noDeaneryPm = createProgrammeMembership("MEDICAL_CURRICULUM",
        "Public Health Medicine", null, LocalDate.now().plusDays(1));

    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    profile.setProgrammeMemberships(List.of(enabledPm, noDeaneryPm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected qualifying programmes.",
        features.forms().ltft().qualifyingProgrammes(), contains(enabledPm.getTisId()));
  }

  @Test
  void shouldOnlyIncludeQualifyingPublicHealthProgrammesInLtftProgrammes() {
    // LTFT compliant.
    ProgrammeMembership current = createProgrammeMembership("MEDICAL_CURRICULUM",
        "Public Health Medicine", "test 1", LocalDate.now().plusDays(1));
    // Finished.
    ProgrammeMembership ended = createProgrammeMembership("MEDICAL_CURRICULUM",
        "Public Health Medicine", "test 1", LocalDate.now().minusDays(1));
    // Wrong deanery.
    ProgrammeMembership otherDeanery = createProgrammeMembership("MEDICAL_CURRICULUM",
        "Public Health Medicine", "not in list", LocalDate.now().plusDays(1));
    // No end date.
    ProgrammeMembership noEndDate = createProgrammeMembership("MEDICAL_CURRICULUM",
        "Public Health Medicine", "test 1", null);

    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    profile.setProgrammeMemberships(List.of(current, ended, otherDeanery, noEndDate));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(true));
    Set<String> qualifyingProgrammes = features.forms().ltft().qualifyingProgrammes();
    assertThat("Unexpected enabled programme count.", qualifyingProgrammes.size(), is(1));
    assertThat("Unexpected enabled programme ID.", qualifyingProgrammes,
        hasItem(current.getTisId()));
  }

  /**
   * Create a programme membership with a single curriculum.
   *
   * @param subType         The curriculum sub-type.
   * @param specialty       The curriculum specialty.
   * @param managingDeanery The managing deanery.
   * @param endDate         The programme end date.
   * @return The created programme membership.
   */
  private static ProgrammeMembership createProgrammeMembership(String subType, String specialty,
      String managingDeanery, LocalDate endDate) {
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType(subType);
    curriculum.setCurriculumSpecialty(specialty);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setTisId(UUID.randomUUID().toString());
    pm.setManagingDeanery(managingDeanery);
    pm.setEndDate(endDate);
    return pm;
  }

  @Test
  void shouldDisableNotificationsWhenProfileNotFound() {
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(null);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.notifications().enabled(), is(false));
  }

  @Test
  void shouldDisableNotificationsWhenNoProgrammes() {
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(
        new TraineeProfile());

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.notifications().enabled(), is(false));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      UNKNOWN            | General Practice
      ""                 | ""
      MEDICAL_SPR        |
                         |
      """)
  void shouldDisableNotificationsWhenNoSpecialtyProgrammes(String subType, String specialty) {
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType(subType);
    curriculum.setCurriculumSpecialty(specialty);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setEndDate(LocalDate.MAX);

    TraineeProfile profile = new TraineeProfile();
    profile.setProgrammeMemberships(List.of(pm));

    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.notifications().enabled(), is(false));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      MEDICAL_CURRICULUM | GENERAL PRACTICE       | DUMMY
      medical_curriculum | general practice       | DUMMY
      MEDICAL_SPR        | GENERAL PRACTICE       | DUMMY
      medical_spr        | general practice       | DUMMY
      MEDICAL_CURRICULUM | PUBLIC HEALTH MEDICINE | DUMMY
      medical_curriculum | public health medicine | DUMMY
      MEDICAL_SPR        | PUBLIC HEALTH MEDICINE | DUMMY
      medical_spr        | public health medicine | DUMMY
      AFT                | ACADEMIC               | ACADEMIC FOUNDATION TRAINING
      aft                |                        | DUMMY
      MEDICAL_CURRICULUM | FOUNDATION             | DUMMY
      medical_curriculum | foundation             |
      """)
  void shouldEnableNotificationsWhenPublicHealthOrSpecialtyOrFoundationProgrammeFound(
      String subType, String specialty, String curriculumName) {
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumName(curriculumName);
    curriculum.setCurriculumSubType(subType);
    curriculum.setCurriculumSpecialty(specialty);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setEndDate(LocalDate.MAX);

    TraineeProfile profile = new TraineeProfile();
    profile.setProgrammeMemberships(List.of(pm));

    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.notifications().enabled(), is(true));
  }

  @Test
  void shouldDisableNewsWhenProfileNotFound() {
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(null);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.news().enabled(), is(false));
  }

  @Test
  void shouldDisableNewsWhenNoProgrammes() {
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(
        new TraineeProfile());

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.news().enabled(), is(false));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      UNKNOWN            | General Practice
      ""                 | ""
      MEDICAL_SPR        |
                         |
      """)
  void shouldDisableNewsWhenNoSpecialtyProgrammes(String subType, String specialty) {
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType(subType);
    curriculum.setCurriculumSpecialty(specialty);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setEndDate(LocalDate.MAX);

    TraineeProfile profile = new TraineeProfile();
    profile.setProgrammeMemberships(List.of(pm));

    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.news().enabled(), is(false));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      MEDICAL_CURRICULUM | GENERAL PRACTICE       | DUMMY
      medical_curriculum | general practice       | DUMMY
      MEDICAL_SPR        | GENERAL PRACTICE       | DUMMY
      medical_spr        | general practice       | DUMMY
      MEDICAL_CURRICULUM | PUBLIC HEALTH MEDICINE | DUMMY
      medical_curriculum | public health medicine | DUMMY
      MEDICAL_SPR        | PUBLIC HEALTH MEDICINE | DUMMY
      medical_spr        | public health medicine | DUMMY
      AFT                | ACADEMIC               | ACADEMIC FOUNDATION TRAINING
      aft                |                        | DUMMY
      MEDICAL_CURRICULUM | FOUNDATION             | DUMMY
      medical_curriculum | foundation             |
      """)
  void shouldEnableNewsWhenPublicHealthOrSpecialtyOrFoundationProgrammeFound(
      String subType, String specialty, String curriculumName) {
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumName(curriculumName);
    curriculum.setCurriculumSubType(subType);
    curriculum.setCurriculumSpecialty(specialty);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(List.of(curriculum));
    pm.setEndDate(LocalDate.MAX);

    TraineeProfile profile = new TraineeProfile();
    profile.setProgrammeMemberships(List.of(pm));

    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected feature flag.", features.news().enabled(), is(true));
  }

  @Test
  void shouldMatchReadOnlyWhenMultipleNonPublicHealthOrSpecialtyProgrammesAndCurricula() {
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSubType("UNKNOWN");
    curriculum.setCurriculumSpecialty("General Practice");

    ProgrammeMembership pm1 = new ProgrammeMembership();
    pm1.setCurricula(List.of(curriculum));
    pm1.setEndDate(LocalDate.MAX);

    Curriculum curriculum3 = new Curriculum();
    curriculum3.setCurriculumSubType("UNKNOWN");
    curriculum3.setCurriculumSpecialty("Public Health Medicine");

    Curriculum curriculum4 = new Curriculum();
    curriculum4.setCurriculumSubType("ANOTHER_SUB_TYPE");
    curriculum4.setCurriculumSpecialty("Another Specialty");

    ProgrammeMembership pm2 = new ProgrammeMembership();
    pm2.setCurricula(List.of(curriculum3, curriculum4));
    pm2.setEndDate(LocalDate.MAX);

    TraineeProfile profile = new TraineeProfile();
    profile.setProgrammeMemberships(List.of(pm1, pm2));

    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected features.", features, is(FeaturesDto.readOnly()));
  }

  @Test
  void shouldMatchEnabledWhenSingleSpecialtyProgrammeCurricula() {
    Curriculum curriculum1 = new Curriculum();
    curriculum1.setCurriculumSubType("MEDICAL_CURRICULUM");
    curriculum1.setCurriculumSpecialty("Foundation");

    Curriculum curriculum2 = new Curriculum();
    curriculum2.setCurriculumSubType("UNKNOWN");
    curriculum2.setCurriculumSpecialty("General Practice");

    ProgrammeMembership pm1 = new ProgrammeMembership();
    pm1.setCurricula(List.of(curriculum1, curriculum2));
    pm1.setEndDate(LocalDate.MAX);

    Curriculum curriculum3 = new Curriculum();
    curriculum3.setCurriculumSubType("MEDICAL_SPR");
    curriculum3.setCurriculumSpecialty("Public Health Medicine");

    Curriculum curriculum4 = new Curriculum();
    curriculum4.setCurriculumSubType("MEDICAL_SPR");
    curriculum4.setCurriculumSpecialty("Another Specialty");

    ProgrammeMembership pm2 = new ProgrammeMembership();
    pm2.setCurricula(List.of(curriculum3, curriculum4));
    pm2.setEndDate(LocalDate.MAX);

    TraineeProfile profile = new TraineeProfile();
    profile.setProgrammeMemberships(List.of(pm1, pm2));

    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected features.", features, is(FeaturesDto.enable()));
  }
}
