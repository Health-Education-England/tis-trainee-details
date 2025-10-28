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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.nhs.hee.trainee.details.config.FeaturesProperties;
import uk.nhs.hee.trainee.details.config.FeaturesProperties.Tranche;
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
        .ltft(Map.of("pilot", Tranche.builder()
            .startDate(LocalDate.EPOCH)
            .deaneries(Set.of("test 1", "test 2", "test 3"))
            .build()))
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
      MEDICAL_CURRICULUM | FOUNDATION
      MEDICAL_CURRICULUM | PUBLIC HEALTH MEDICINE
      MEDICAL_SPR        | FOUNDATION
      MEDICAL_SPR        | PUBLIC HEALTH MEDICINE
      UNKNOWN            | General Practice
      medical_curriculum | foundation
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
  @ValueSource(strings = {"MEDICAL_CURRICULUM", "medical_curriculum", "MEDICAL_SPR", "medical_spr"})
  void shouldEnableActionsWhenSpecialtyProgrammeFound(String subType) {
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
      MEDICAL_CURRICULUM | FOUNDATION
      MEDICAL_CURRICULUM | PUBLIC HEALTH MEDICINE
      MEDICAL_SPR        | FOUNDATION
      MEDICAL_SPR        | PUBLIC HEALTH MEDICINE
      UNKNOWN            | General Practice
      medical_curriculum | foundation
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
  @ValueSource(strings = {"MEDICAL_CURRICULUM", "medical_curriculum", "MEDICAL_SPR", "medical_spr"})
  void shouldEnableCctWhenSpecialtyProgrammeFound(String subType) {
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
      MEDICAL_CURRICULUM | FOUNDATION
      MEDICAL_CURRICULUM | PUBLIC HEALTH MEDICINE
      MEDICAL_SPR        | FOUNDATION
      MEDICAL_SPR        | PUBLIC HEALTH MEDICINE
      UNKNOWN            | General Practice
      medical_curriculum | foundation
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
  @ValueSource(strings = {"MEDICAL_CURRICULUM", "medical_curriculum", "MEDICAL_SPR", "medical_spr"})
  void shouldEnableDetailsWhenSpecialtyProgrammeFound(String subType) {
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
      MEDICAL_CURRICULUM | FOUNDATION
      MEDICAL_SPR        | FOUNDATION
      UNKNOWN            | General Practice
      medical_curriculum | foundation
      ""                 | ""
      MEDICAL_SPR        |
                         |
      """)
  void shouldDisableFormsWhenNoPublicHealthOrSpecialtyProgrammes(String subType, String specialty) {
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
      MEDICAL_CURRICULUM | FOUNDATION
      MEDICAL_CURRICULUM | PUBLIC HEALTH MEDICINE
      MEDICAL_SPR        | FOUNDATION
      MEDICAL_SPR        | PUBLIC HEALTH MEDICINE
      UNKNOWN            | General Practice
      medical_curriculum | foundation
      ""                 | ""
      MEDICAL_SPR        |
                         |
      """)
  void shouldDisableLtftWhenNoSpecialtyProgrammes(String subType, String specialty) {
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
  @ValueSource(strings = {"test 1", "test 2", "test 3"})
  void shouldDisableLtftWhenNoTranchesStarted(String deanery) {
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

    FeaturesProperties featuresProperties = FeaturesProperties.builder()
        .ltft(Map.of("pilot", Tranche.builder()
            .startDate(LocalDate.MAX)
            .deaneries(Set.of("test 1", "test 2", "test 3"))
            .build()))
        .build();

    service = new FeatureService(traineeIdentity, profileService, featuresProperties,
        ZoneId.of("UTC"));

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(false));
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
  @ValueSource(strings = {"test 1", "test 2", "test 3"})
  void shouldCombineStartedLtftTranches(String deanery) {
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

    TraineeIdentity traineeIdentity = new TraineeIdentity();
    traineeIdentity.setTraineeId(TRAINEE_ID);

    FeaturesProperties featuresProperties = FeaturesProperties.builder()
        .ltft(Map.of(
            "tranche1", Tranche.builder()
                .startDate(LocalDate.EPOCH)
                .deaneries(Set.of("test 1", "test 2"))
                .build(),
            "tranche2", Tranche.builder()
                .startDate(LocalDate.now())
                .deaneries(Set.of("test 3"))
                .build()))
        .build();

    service = new FeatureService(traineeIdentity, profileService, featuresProperties,
        ZoneId.of("UTC"));

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"future 1", "future 2", "future 3"})
  void shouldNotCombineNotStartedLtftTranches(String deanery) {
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

    FeaturesProperties featuresProperties = FeaturesProperties.builder()
        .ltft(Map.of(
            "tranche1", Tranche.builder()
                .startDate(LocalDate.EPOCH)
                .deaneries(Set.of("test 1", "test 2", "test 3"))
                .build(),
            "tranche2", Tranche.builder()
                .startDate(LocalDate.MAX)
                .deaneries(Set.of("future 1", "future 2", "future 3"))
                .build()))
        .build();

    service = new FeatureService(traineeIdentity, profileService, featuresProperties,
        ZoneId.of("UTC"));

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"test 1", "test 2", "test 3"})
  void shouldEnableLtftWhenNoTranchesStartedAndBetaUser(String deanery) {
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
        .ltft(Map.of("pilot", Tranche.builder()
            .startDate(LocalDate.MAX)
            .deaneries(Set.of("test 1", "test 2", "test 3"))
            .build()))
        .build();

    service = new FeatureService(traineeIdentity, profileService, featuresProperties,
        ZoneId.of("UTC"));

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.forms().ltft().enabled(), is(true));
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
      MEDICAL_CURRICULUM | FOUNDATION
      MEDICAL_CURRICULUM | PUBLIC HEALTH MEDICINE
      MEDICAL_SPR        | FOUNDATION
      MEDICAL_SPR        | PUBLIC HEALTH MEDICINE
      UNKNOWN            | General Practice
      medical_curriculum | foundation
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
  @ValueSource(strings = {"MEDICAL_CURRICULUM", "medical_curriculum", "MEDICAL_SPR", "medical_spr"})
  void shouldEnableNotificationsWhenSpecialtyProgrammeFound(String subType) {
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

    assertThat("Unexpected feature flag.", features.notifications().enabled(), is(true));
  }

  @Test
  void shouldMatchReadOnlyWhenMultipleNonPublicHealthOrSpecialtyProgrammesAndCurricula() {
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
