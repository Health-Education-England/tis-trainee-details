/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.nhs.hee.trainee.details.model.Curriculum;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;

@ExtendWith(OutputCaptureExtension.class)
class NtnGeneratorTest {

  private static final String CURRICULUM_SPECIALTY_CODE = "ABC";
  private static final String CURRICULUM_SUB_TYPE_MC = "MEDICAL_CURRICULUM";
  private static final String CURRICULUM_SUB_TYPE_SS = "SUB_SPECIALTY";
  private static final String GDC_NUMBER = "12345";
  private static final String GMC_NUMBER = "1234567";
  private static final String OWNER_NAME = "London LETBs";
  private static final String PROGRAMME_NAME = "Programme Name";
  private static final String PROGRAMME_NUMBER = "PROG123";
  private static final String TRAINING_PATHWAY = "N/A";

  private static final LocalDate NOW = LocalDate.now();
  private static final LocalDate PAST = NOW.minusYears(1);
  private static final LocalDate FUTURE = NOW.plusYears(1);

  private NtnGenerator service;

  @BeforeEach
  void setUp() {
    service = new NtnGenerator();
  }

  @Test
  void shouldNotPopulateNtnWhenNoPersonalDetails(CapturedOutput output) {
    TraineeProfile profile = new TraineeProfile();

    profile.setPersonalDetails(null);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    assertThat("Unexpected ntn.", ntn, nullValue());

    assertThat("Expected log not found.", output.getOut(),
        containsString("Skipping NTN population as personal details not available."));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "abcd", "1234"})
  void shouldNotPopulateNtnWhenNoGmcOrGdcNumber(String referenceNumber, CapturedOutput output) {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(referenceNumber);
    personalDetails.setGdcNumber(referenceNumber);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    assertThat("Unexpected ntn.", ntn, nullValue());

    assertThat("Expected log not found.", output.getOut(),
        containsString("Skipping NTN population as reference number not valid."));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void shouldNotPopulateNtnWhenNoProgrammeNumber(String programmeNumber, CapturedOutput output) {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(programmeNumber);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    assertThat("Unexpected ntn.", ntn, nullValue());

    assertThat("Expected log not found.", output.getOut(),
        containsString("Skipping NTN population as programme number is blank."));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void shouldNotPopulateNtnWhenNoProgrammeName(String programmeName, CapturedOutput output) {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(programmeName);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    assertThat("Unexpected ntn.", ntn, nullValue());

    assertThat("Expected log not found.", output.getOut(),
        containsString("Skipping NTN population as programme name is blank."));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "foundation", "FOUNDATION", "prefix foundation", "foundation suffix",
      "prefix foundation suffix"
  })
  void shouldNotPopulateNtnWhenProgrammeIsFoundation(String programmeName,
      CapturedOutput output) {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(programmeName);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    assertThat("Unexpected ntn.", ntn, nullValue());

    assertThat("Expected log not found.", output.getOut(), containsString(
        "Skipping NTN population as programme name '" + programmeName + "' is excluded."));
  }

  @Test
  void shouldNotPopulateNtnWhenNoCurricula(CapturedOutput output) {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    pm.setCurricula(List.of());
    profile.setProgrammeMemberships(List.of(pm));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    assertThat("Unexpected ntn.", ntn, nullValue());
    assertThat("Expected log not found.", output.getOut(),
        containsString("Skipping NTN population as there are no valid curricula."));
  }

  @Test
  void shouldNotPopulateNtnWhenNoCurrentCurricula(CapturedOutput output) {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum past = new Curriculum();
    past.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    past.setCurriculumStartDate(PAST);
    past.setCurriculumEndDate(NOW.minusDays(1));

    Curriculum future = new Curriculum();
    future.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    future.setCurriculumStartDate(NOW.plusDays(1));
    future.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(past, future));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    assertThat("Unexpected ntn.", ntn, nullValue());
    assertThat("Expected log not found.", output.getOut(),
        containsString("Skipping NTN population as there are no valid curricula."));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void shouldNotPopulateNtnWhenNoCurriculaSpecialtyCode(String specialtyCode,
      CapturedOutput output) {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSpecialtyCode(specialtyCode);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    assertThat("Unexpected ntn.", ntn, nullValue());
    assertThat("Expected log not found.", output.getOut(),
        containsString("Skipping NTN population as there are no valid curricula."));
  }

  @Test
  void shouldNotPopulateNtnWhenTrainingPathwayNull(CapturedOutput output) {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(null);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    assertThat("Unexpected ntn.", ntn, nullValue());
    assertThat("Expected log not found.", output.getOut(),
        containsString("Unable to generate NTN as training pathway was null."));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "Unknown Organization"})
  void shouldThrowExceptionPopulatingNtnWhenParentOrganizationNull(String ownerName) {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(ownerName);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.populateNtns(profile));

    assertThat("Unexpected message.", exception.getMessage(),
        is("Unable to calculate the parent organization."));
  }

  @Test
  void shouldPopulateFullNtnWhenProgrammeIsCurrent() {
    TraineeProfile profile = new TraineeProfile();
    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum1 = new Curriculum();
    curriculum1.setCurriculumSpecialtyCode("ABC");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(FUTURE);

    Curriculum curriculum2 = new Curriculum();
    curriculum2.setCurriculumSpecialtyCode("123");
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_SS);
    curriculum2.setCurriculumStartDate(NOW);
    curriculum2.setCurriculumEndDate(FUTURE);

    Curriculum curriculum3 = new Curriculum();
    curriculum3.setCurriculumSpecialtyCode("XYZ");
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_SS);
    curriculum3.setCurriculumStartDate(PAST);
    curriculum3.setCurriculumEndDate(NOW);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3));

    service.populateNtns(profile);

    assertThat("Unexpected NTN.", pm.getNtn(), is("LDN/ABC.XYZ.123/1234567/D"));
  }

  @Test
  void shouldPopulateFullNtnWhenProgrammeIsFuture() {
    TraineeProfile profile = new TraineeProfile();
    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(FUTURE);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum1 = new Curriculum();
    curriculum1.setCurriculumSpecialtyCode("ABC");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumStartDate(NOW);
    curriculum1.setCurriculumEndDate(FUTURE.plusDays(1));

    Curriculum curriculum2 = new Curriculum();
    curriculum2.setCurriculumSpecialtyCode("123");
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_SS);
    curriculum2.setCurriculumStartDate(FUTURE);
    curriculum2.setCurriculumEndDate(FUTURE.plusDays(1));

    Curriculum curriculum3 = new Curriculum();
    curriculum3.setCurriculumSpecialtyCode("XYZ");
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_SS);
    curriculum3.setCurriculumStartDate(NOW);
    curriculum3.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3));

    service.populateNtns(profile);

    assertThat("Unexpected NTN.", pm.getNtn(), is("LDN/ABC.XYZ.123/1234567/D"));
  }

  @Test
  void shouldPopulateNtnsWhenTraineeProfileHasMultiplePms() {
    TraineeProfile traineeProfile = new TraineeProfile();
    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    traineeProfile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm1 = new ProgrammeMembership();
    pm1.setManagingDeanery(OWNER_NAME);
    pm1.setProgrammeName(PROGRAMME_NAME);
    pm1.setProgrammeNumber(PROGRAMME_NUMBER);
    pm1.setTrainingPathway(TRAINING_PATHWAY);
    pm1.setStartDate(PAST);

    Curriculum curriculum1 = new Curriculum();
    curriculum1.setCurriculumSpecialtyCode("ABC");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(PAST);
    pm1.setCurricula(List.of(curriculum1));

    ProgrammeMembership pm2 = new ProgrammeMembership();
    pm2.setManagingDeanery(OWNER_NAME);
    pm2.setProgrammeName(PROGRAMME_NAME);
    pm2.setProgrammeNumber(PROGRAMME_NUMBER);
    pm2.setTrainingPathway(TRAINING_PATHWAY);
    pm2.setStartDate(NOW);

    Curriculum curriculum2 = new Curriculum();
    curriculum2.setCurriculumSpecialtyCode("123");
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumStartDate(NOW);
    curriculum2.setCurriculumEndDate(NOW);
    pm2.setCurricula(List.of(curriculum2));

    ProgrammeMembership pm3 = new ProgrammeMembership();
    pm3.setManagingDeanery(OWNER_NAME);
    pm3.setProgrammeName(PROGRAMME_NAME);
    pm3.setProgrammeNumber(PROGRAMME_NUMBER);
    pm3.setTrainingPathway(TRAINING_PATHWAY);
    pm3.setStartDate(FUTURE);

    Curriculum curriculum3 = new Curriculum();
    curriculum3.setCurriculumSpecialtyCode("XYZ");
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumStartDate(FUTURE);
    curriculum3.setCurriculumEndDate(FUTURE);
    pm3.setCurricula(List.of(curriculum3));

    traineeProfile.setProgrammeMemberships(List.of(pm1, pm2, pm3));

    service.populateNtns(traineeProfile);

    assertThat("Unexpected NTN.", pm1.getNtn(), nullValue());
    assertThat("Unexpected NTN.", pm2.getNtn(), is("LDN/123/1234567/D"));
    assertThat("Unexpected NTN.", pm3.getNtn(), is("LDN/XYZ/1234567/D"));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      Defence Postgraduate Medical Deanery                   | TSD
      Health Education England East Midlands                 | EMD
      Health Education England East of England               | EAN
      Health Education England Kent, Surrey and Sussex       | KSS
      Health Education England North Central and East London | LDN
      Health Education England North East                    | NTH
      Health Education England North West                    | NWE
      Health Education England North West London             | LDN
      Health Education England South London                  | LDN
      Health Education England Thames Valley                 | OXF
      Health Education England Wessex                        | WES
      Health Education England West Midlands                 | WMD
      Health Education England Yorkshire and The Humber      | YHD
      London LETBs                                           | LDN
      Severn Deanery                                         | SEV
      South West Peninsula Deanery                           | PEN
      """)
  void shouldPopulateNtnWithParentOrganizationWhenMappedByOwner(String ownerName,
      String ownerCode) {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(ownerName);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    String[] ntnParts = ntn.split("/");
    assertThat("Unexpected parent organization.", ntnParts[0], is(ownerCode));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      Health Education England South West | SWPABC | PEN
      Health Education England South West | ABCYXZ | ABC
      Health Education England South West | XYZABC | XYZ
      """)
  void shouldPopulateNtnWithParentOrganizationWhenOwnerIsSouthWest(String ownerName,
      String programmeNumber, String ownerCode) {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(ownerName);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(programmeNumber);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    String[] ntnParts = ntn.split("/");
    assertThat("Unexpected parent organization.", ntnParts[0], is(ownerCode));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      AAA | ZZZ | 111 | 999 | ZZZ-AAA-999-111
      999 | 111 | ZZZ | AAA | ZZZ-AAA-999-111
      001 | 010 | 100 | 111 | 111-100-010-001
      """)
  void shouldPopulateNtnWithOrderedSpecialtyConcatWhenMultipleSpecialty(String specialty1,
      String specialty2, String specialty3, String specialty4, String ntnPart) {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum1 = new Curriculum();
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumSpecialtyCode(specialty1);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(FUTURE);

    Curriculum curriculum2 = new Curriculum();
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumSpecialtyCode(specialty2);
    curriculum2.setCurriculumStartDate(PAST);
    curriculum2.setCurriculumEndDate(FUTURE);

    Curriculum curriculum3 = new Curriculum();
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumSpecialtyCode(specialty3);
    curriculum3.setCurriculumStartDate(PAST);
    curriculum3.setCurriculumEndDate(FUTURE);

    Curriculum curriculum4 = new Curriculum();
    curriculum4.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum4.setCurriculumSpecialtyCode(specialty4);
    curriculum4.setCurriculumStartDate(PAST);
    curriculum4.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3, curriculum4));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    String[] ntnParts = ntn.split("/");
    assertThat("Unexpected parent organization.", ntnParts[1], is(ntnPart));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 10})
  void shouldPopulateNtnWithDotNotatedSpecialtyConcatWhenHasSubSpecialties(
      int additionalCurriculaCount) {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    List<Curriculum> curricula = new ArrayList<>();
    pm.setCurricula(curricula);

    Curriculum curriculum1 = new Curriculum();
    curriculum1.setCurriculumName("A sub spec 1");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_SS);
    curriculum1.setCurriculumSpecialtyCode("888");
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(FUTURE);
    curricula.add(curriculum1);

    Curriculum curriculum2 = new Curriculum();
    curriculum2.setCurriculumName("A sub spec 2");
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_SS);
    curriculum2.setCurriculumSpecialtyCode("999");
    curriculum2.setCurriculumStartDate(PAST);
    curriculum2.setCurriculumEndDate(FUTURE);
    curricula.add(curriculum2);

    for (int i = 1; i <= additionalCurriculaCount; i++) {
      Curriculum additionalCurriculum = new Curriculum();
      additionalCurriculum.setCurriculumName("Not sub spec " + i);
      additionalCurriculum.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
      additionalCurriculum.setCurriculumSpecialtyCode(String.format("%03d", i));
      additionalCurriculum.setCurriculumStartDate(PAST);
      additionalCurriculum.setCurriculumEndDate(FUTURE);
      curricula.add(additionalCurriculum);
    }

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    String[] ntnParts = ntn.split("/");
    assertThat("Unexpected parent organization.", ntnParts[1], endsWith(".999.888"));

    long dotCount = ntnParts[1].chars().filter(ch -> ch == '.').count();
    assertThat("Unexpected sub specialty count.", dotCount, is(2L));
  }

  @Test
  void shouldPopulateNtnWithFixedSpecialtyConcatWhenFirstSpecialtyIsAft() {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum1 = new Curriculum();
    curriculum1.setCurriculumName("AFT");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumSpecialtyCode("ACA");
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(FUTURE);

    Curriculum curriculum2 = new Curriculum();
    curriculum2.setCurriculumName("Not AFT 2");
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumSpecialtyCode("003");
    curriculum2.setCurriculumStartDate(PAST);
    curriculum2.setCurriculumEndDate(FUTURE);

    Curriculum curriculum3 = new Curriculum();
    curriculum3.setCurriculumName("Not AFT 1");
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumSpecialtyCode("777");
    curriculum3.setCurriculumStartDate(PAST);
    curriculum3.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    String[] ntnParts = ntn.split("/");
    assertThat("Unexpected parent organization.", ntnParts[1], is("ACA-FND"));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      AAA | ZZZ | 111
      AAA | 111 | ZZZ
      ZZZ | AAA | 111
      ZZZ | 111 | AAA
      111 | AAA | ZZZ
      111 | ZZZ | AAA
      """)
  void shouldFilterCurriculaWhenPopulatingNtnWithOrderedSpecialtyConcatAndCurriculaEnding(
      String pastSpecialty, String endingSpecialty, String futureSpecialty) {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum1 = new Curriculum();
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumSpecialtyCode(pastSpecialty);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(PAST);

    Curriculum curriculum2 = new Curriculum();
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumSpecialtyCode(futureSpecialty);
    curriculum2.setCurriculumStartDate(FUTURE);
    curriculum2.setCurriculumEndDate(FUTURE);

    Curriculum curriculum3 = new Curriculum();
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumSpecialtyCode(endingSpecialty);
    curriculum3.setCurriculumStartDate(PAST);
    curriculum3.setCurriculumEndDate(NOW);

    Curriculum curriculum4 = new Curriculum();
    curriculum4.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum4.setCurriculumSpecialtyCode(null);
    curriculum4.setCurriculumStartDate(PAST);
    curriculum4.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3, curriculum4));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    String[] ntnParts = ntn.split("/");
    assertThat("Unexpected parent organization.", ntnParts[1], is(endingSpecialty));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      AAA | ZZZ | 111
      AAA | 111 | ZZZ
      ZZZ | AAA | 111
      ZZZ | 111 | AAA
      111 | AAA | ZZZ
      111 | ZZZ | AAA
      """)
  void shouldFilterCurriculaWhenPopulatingNtnWithOrderedSpecialtyConcatAndCurriculaCurrent(
      String pastSpecialty, String currentSpecialty, String futureSpecialty) {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum1 = new Curriculum();
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumSpecialtyCode(pastSpecialty);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(PAST);

    Curriculum curriculum2 = new Curriculum();
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumSpecialtyCode(futureSpecialty);
    curriculum2.setCurriculumStartDate(FUTURE);
    curriculum2.setCurriculumEndDate(FUTURE);

    Curriculum curriculum3 = new Curriculum();
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumSpecialtyCode(currentSpecialty);
    curriculum3.setCurriculumStartDate(PAST);
    curriculum3.setCurriculumEndDate(FUTURE);

    Curriculum curriculum4 = new Curriculum();
    curriculum4.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum4.setCurriculumSpecialtyCode(null);
    curriculum4.setCurriculumStartDate(PAST);
    curriculum4.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3, curriculum4));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    String[] ntnParts = ntn.split("/");
    assertThat("Unexpected parent organization.", ntnParts[1], is(currentSpecialty));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      AAA | ZZZ | 111
      AAA | 111 | ZZZ
      ZZZ | AAA | 111
      ZZZ | 111 | AAA
      111 | AAA | ZZZ
      111 | ZZZ | AAA
      """)
  void shouldFilterCurriculaWhenPopulatingNtnWithOrderedSpecialtyConcatAndCurriculaStarting(
      String pastSpecialty, String startingSpecialty, String futureSpecialty) {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum1 = new Curriculum();
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumSpecialtyCode(pastSpecialty);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(PAST);

    Curriculum curriculum2 = new Curriculum();
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumSpecialtyCode(futureSpecialty);
    curriculum2.setCurriculumStartDate(FUTURE);
    curriculum2.setCurriculumEndDate(FUTURE);

    Curriculum curriculum3 = new Curriculum();
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumSpecialtyCode(startingSpecialty);
    curriculum3.setCurriculumStartDate(NOW);
    curriculum3.setCurriculumEndDate(FUTURE);

    Curriculum curriculum4 = new Curriculum();
    curriculum4.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum4.setCurriculumSpecialtyCode(null);
    curriculum4.setCurriculumStartDate(PAST);
    curriculum4.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3, curriculum4));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    String[] ntnParts = ntn.split("/");
    assertThat("Unexpected parent organization.", ntnParts[1], is(startingSpecialty));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      AAA | ZZZ | 111
      AAA | 111 | ZZZ
      ZZZ | AAA | 111
      ZZZ | 111 | AAA
      111 | AAA | ZZZ
      111 | ZZZ | AAA
      """)
  void shouldFilterCurriculaWhenPopulatingNtnWithOrderedSpecialtyConcatAndProgrammeFuture(
      String currentSpecialty, String futureSpecialty, String farFutureSpecialty) {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(FUTURE);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum1 = new Curriculum();
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumSpecialtyCode(currentSpecialty);
    curriculum1.setCurriculumStartDate(NOW);
    curriculum1.setCurriculumEndDate(NOW);

    Curriculum curriculum2 = new Curriculum();
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumSpecialtyCode(futureSpecialty);
    curriculum2.setCurriculumStartDate(FUTURE);
    curriculum2.setCurriculumEndDate(FUTURE);

    Curriculum curriculum3 = new Curriculum();
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumSpecialtyCode(farFutureSpecialty);
    curriculum3.setCurriculumStartDate(FUTURE.plusDays(1));
    curriculum3.setCurriculumEndDate(FUTURE.plusDays(1));

    Curriculum curriculum4 = new Curriculum();
    curriculum4.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum4.setCurriculumSpecialtyCode(null);
    curriculum4.setCurriculumStartDate(PAST);
    curriculum4.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3, curriculum4));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    String[] ntnParts = ntn.split("/");
    assertThat("Unexpected parent organization.", ntnParts[1], is(futureSpecialty));
  }

  @Test
  void shouldPopulateNtnWithGmcNumberWhenValid() {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    String[] ntnParts = ntn.split("/");
    assertThat("Unexpected parent organization.", ntnParts[2], is(GMC_NUMBER));
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc", "12345678"})
  void shouldPopulateNtnWithGdcNumberWhenValidAndGmcInvalid(String gmcNumber) {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(gmcNumber);
    personalDetails.setGdcNumber(GDC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    String[] ntnParts = ntn.split("/");
    assertThat("Unexpected parent organization.", ntnParts[2], is(GDC_NUMBER));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      CCT  | C
      CESR | CP
      N/A  | D
      """)
  void shouldPopulateNtnWithSuffixWhenMappedByTrainingPathway(String trainingPathway,
      String suffix) {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(trainingPathway);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    String[] ntnParts = ntn.split("/");
    assertThat("Unexpected parent organization.", ntnParts[3], is(suffix));
  }

  @Test
  void shouldPopulateNtnWithSuffixWhenSpecialtyIsAcademic() {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum1 = new Curriculum();
    curriculum1.setCurriculumSpecialtyCode("123");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(FUTURE);

    Curriculum curriculum2 = new Curriculum();
    curriculum2.setCurriculumSpecialtyCode("ACA");
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumStartDate(PAST);
    curriculum2.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    String[] ntnParts = ntn.split("/");
    assertThat("Unexpected parent organization.", ntnParts[3], is("C"));
  }

  @Test
  void shouldFilterCurriculaWhenPopulatingNtnWithSuffixAndCurriculaEnding() {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum1 = new Curriculum();
    curriculum1.setCurriculumName("Past");
    curriculum1.setCurriculumSpecialtyCode("ACA");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(PAST);

    Curriculum curriculum2 = new Curriculum();
    curriculum2.setCurriculumName("Ending");
    curriculum2.setCurriculumSpecialtyCode("123");
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumStartDate(PAST);
    curriculum2.setCurriculumEndDate(NOW);

    Curriculum curriculum3 = new Curriculum();
    curriculum3.setCurriculumName("Future");
    curriculum3.setCurriculumSpecialtyCode("ACA");
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumStartDate(FUTURE);
    curriculum3.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    String[] ntnParts = ntn.split("/");
    assertThat("Unexpected parent organization.", ntnParts[3], is("D"));
  }

  @Test
  void shouldFilterCurriculaWhenPopulatingNtnWithSuffixAndCurriculaCurrent() {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum1 = new Curriculum();
    curriculum1.setCurriculumName("Past");
    curriculum1.setCurriculumSpecialtyCode("ACA");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(PAST);

    Curriculum curriculum2 = new Curriculum();
    curriculum2.setCurriculumName("Current");
    curriculum2.setCurriculumSpecialtyCode("123");
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumStartDate(PAST);
    curriculum2.setCurriculumEndDate(FUTURE);

    Curriculum curriculum3 = new Curriculum();
    curriculum3.setCurriculumName("Future");
    curriculum3.setCurriculumSpecialtyCode("ACA");
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumStartDate(FUTURE);
    curriculum3.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    String[] ntnParts = ntn.split("/");
    assertThat("Unexpected parent organization.", ntnParts[3], is("D"));
  }

  @Test
  void shouldFilterCurriculaWhenPopulatingNtnWithSuffixAndCurriculaStarting() {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum1 = new Curriculum();
    curriculum1.setCurriculumName("Past");
    curriculum1.setCurriculumSpecialtyCode("ACA");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(PAST);

    Curriculum curriculum2 = new Curriculum();
    curriculum2.setCurriculumName("Starting");
    curriculum2.setCurriculumSpecialtyCode("123");
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumStartDate(NOW);
    curriculum2.setCurriculumEndDate(FUTURE);

    Curriculum curriculum3 = new Curriculum();
    curriculum3.setCurriculumName("Future");
    curriculum3.setCurriculumSpecialtyCode("ACA");
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumStartDate(FUTURE);
    curriculum3.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    String[] ntnParts = ntn.split("/");
    assertThat("Unexpected parent organization.", ntnParts[3], is("D"));
  }

  @Test
  void shouldFilterCurriculaWhenPopulatingNtnWithSuffixAndProgrammeFuture() {
    TraineeProfile profile = new TraineeProfile();

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(FUTURE);
    profile.setProgrammeMemberships(List.of(pm));

    Curriculum curriculum1 = new Curriculum();
    curriculum1.setCurriculumName("Past");
    curriculum1.setCurriculumSpecialtyCode("ACA");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(PAST);

    Curriculum curriculum2 = new Curriculum();
    curriculum2.setCurriculumName("Past");
    curriculum2.setCurriculumSpecialtyCode("ACA");
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumStartDate(NOW);
    curriculum2.setCurriculumEndDate(NOW);

    Curriculum curriculum3 = new Curriculum();
    curriculum3.setCurriculumName("Future");
    curriculum3.setCurriculumSpecialtyCode("123");
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumStartDate(FUTURE);
    curriculum3.setCurriculumEndDate(FUTURE);

    Curriculum curriculum4 = new Curriculum();
    curriculum4.setCurriculumName("Future + 1");
    curriculum4.setCurriculumSpecialtyCode("ACA");
    curriculum4.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum4.setCurriculumStartDate(FUTURE.plusDays(1));
    curriculum4.setCurriculumEndDate(FUTURE.plusDays(1));

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3, curriculum4));

    service.populateNtns(profile);

    String ntn = pm.getNtn();
    String[] ntnParts = ntn.split("/");
    assertThat("Unexpected parent organization.", ntnParts[3], is("D"));
  }
}
