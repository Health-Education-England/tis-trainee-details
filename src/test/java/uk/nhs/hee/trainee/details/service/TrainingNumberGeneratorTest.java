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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
import uk.nhs.hee.trainee.details.dto.CurriculumDto;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsDto;
import uk.nhs.hee.trainee.details.dto.ProgrammeMembershipDto;
import uk.nhs.hee.trainee.details.dto.TraineeProfileDto;
import uk.nhs.hee.trainee.details.dto.signature.Signature;

@ExtendWith(OutputCaptureExtension.class)
class TrainingNumberGeneratorTest {

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

  private TrainingNumberGenerator service;
  private SignatureService signatureServiceMock;

  @BeforeEach
  void setUp() {
    signatureServiceMock = mock(SignatureService.class);
    service = new TrainingNumberGenerator(signatureServiceMock);
  }

  @Test
  void shouldNotPopulateTrainingNumberWhenNoPersonalDetails(CapturedOutput output) {
    TraineeProfileDto profile = new TraineeProfileDto();

    profile.setPersonalDetails(null);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum = new CurriculumDto();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    assertThat("Unexpected training number.", trainingNumber, nullValue());

    assertThat("Expected log not found.", output.getOut(),
        containsString("Skipping training number population as personal details not available."));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "abcd", "1234"})
  void shouldNotPopulateTrainingNumberWhenNoGmcOrGdcNumber(String referenceNumber,
      CapturedOutput output) {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(referenceNumber);
    personalDetails.setGdcNumber(referenceNumber);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum = new CurriculumDto();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    assertThat("Unexpected training number.", trainingNumber, nullValue());

    assertThat("Expected log not found.", output.getOut(),
        containsString("Skipping training number population as reference number not valid."));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void shouldNotPopulateTrainingNumberWhenNoProgrammeNumber(String programmeNumber,
      CapturedOutput output) {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(programmeNumber);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum = new CurriculumDto();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    assertThat("Unexpected training number.", trainingNumber, nullValue());

    assertThat("Expected log not found.", output.getOut(),
        containsString("Skipping training number population as programme number is blank."));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void shouldNotPopulateTrainingNumberWhenNoProgrammeName(String programmeName,
      CapturedOutput output) {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(programmeName);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum = new CurriculumDto();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    assertThat("Unexpected training number.", trainingNumber, nullValue());

    assertThat("Expected log not found.", output.getOut(),
        containsString("Skipping training number population as programme localOffice is blank."));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "foundation", "FOUNDATION", "prefix foundation", "foundation suffix",
      "prefix foundation suffix"
  })
  void shouldNotPopulateTrainingNumberWhenProgrammeIsFoundation(String programmeName,
      CapturedOutput output) {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(programmeName);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum = new CurriculumDto();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    assertThat("Unexpected training number.", trainingNumber, nullValue());

    assertThat("Expected log not found.", output.getOut(), containsString(
        "Skipping training number population as programme localOffice '" + programmeName
            + "' is excluded."));
  }

  @Test
  void shouldNotPopulateTrainingNumberWhenNoCurricula(CapturedOutput output) {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    pm.setCurricula(List.of());
    profile.setProgrammeMemberships(List.of(pm));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    assertThat("Unexpected training number.", trainingNumber, nullValue());
    assertThat("Expected log not found.", output.getOut(),
        containsString("Skipping training number population as there are no valid curricula."));
  }

  @Test
  void shouldNotPopulateTrainingNumberWhenNoCurrentCurricula(CapturedOutput output) {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto past = new CurriculumDto();
    past.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    past.setCurriculumStartDate(PAST);
    past.setCurriculumEndDate(NOW.minusDays(1));

    CurriculumDto future = new CurriculumDto();
    future.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    future.setCurriculumStartDate(NOW.plusDays(1));
    future.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(past, future));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    assertThat("Unexpected training number.", trainingNumber, nullValue());
    assertThat("Expected log not found.", output.getOut(),
        containsString("Skipping training number population as there are no valid curricula."));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void shouldNotPopulateTrainingNumberWhenNoCurriculaSpecialtyCode(String specialtyCode,
      CapturedOutput output) {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum = new CurriculumDto();
    curriculum.setCurriculumSpecialtyCode(specialtyCode);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    assertThat("Unexpected training number.", trainingNumber, nullValue());
    assertThat("Expected log not found.", output.getOut(),
        containsString("Skipping training number population as there are no valid curricula."));
  }

  @Test
  void shouldNotPopulateTrainingNumberWhenTrainingPathwayNull(CapturedOutput output) {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(null);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum = new CurriculumDto();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    assertThat("Unexpected training number.", trainingNumber, nullValue());
    assertThat("Expected log not found.", output.getOut(),
        containsString("Unable to generate training number as training pathway was null."));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "Unknown Organization"})
  void shouldThrowExceptionPopulatingTrainingNumberWhenParentOrganizationNull(String ownerName) {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(ownerName);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum = new CurriculumDto();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.populateTrainingNumbers(profile));

    assertThat("Unexpected message.", exception.getMessage(),
        is("Unable to calculate the parent organization."));
  }

  @Test
  void shouldUseTsdPrefixWhenMilitaryProgrammeMembershipType() {
    TraineeProfileDto profile = new TraineeProfileDto();
    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setProgrammeMembershipType("MILITARY");
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum1 = new CurriculumDto();
    curriculum1.setCurriculumSpecialtyCode("ABC");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1));

    service.populateTrainingNumbers(profile);

    assertThat("Unexpected training number.", pm.getTrainingNumber(),
        is("TSD/ABC/1234567/D"));
  }

  @Test
  void shouldPopulateFullTrainingNumberWhenProgrammeIsCurrent() {
    TraineeProfileDto profile = new TraineeProfileDto();
    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum1 = new CurriculumDto();
    curriculum1.setCurriculumSpecialtyCode("ABC");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(FUTURE);

    CurriculumDto curriculum2 = new CurriculumDto();
    curriculum2.setCurriculumSpecialtyCode("123");
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_SS);
    curriculum2.setCurriculumStartDate(NOW);
    curriculum2.setCurriculumEndDate(FUTURE);

    CurriculumDto curriculum3 = new CurriculumDto();
    curriculum3.setCurriculumSpecialtyCode("XYZ");
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_SS);
    curriculum3.setCurriculumStartDate(PAST);
    curriculum3.setCurriculumEndDate(NOW);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3));

    service.populateTrainingNumbers(profile);

    assertThat("Unexpected training number.", pm.getTrainingNumber(),
        is("LDN/ABC.XYZ.123/1234567/D"));
  }

  @Test
  void shouldPopulateFullTrainingNumberWhenProgrammeIsFuture() {
    TraineeProfileDto profile = new TraineeProfileDto();
    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(FUTURE);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum1 = new CurriculumDto();
    curriculum1.setCurriculumSpecialtyCode("ABC");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumStartDate(NOW);
    curriculum1.setCurriculumEndDate(FUTURE.plusDays(1));

    CurriculumDto curriculum2 = new CurriculumDto();
    curriculum2.setCurriculumSpecialtyCode("123");
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_SS);
    curriculum2.setCurriculumStartDate(FUTURE);
    curriculum2.setCurriculumEndDate(FUTURE.plusDays(1));

    CurriculumDto curriculum3 = new CurriculumDto();
    curriculum3.setCurriculumSpecialtyCode("XYZ");
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_SS);
    curriculum3.setCurriculumStartDate(NOW);
    curriculum3.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3));

    service.populateTrainingNumbers(profile);

    assertThat("Unexpected training number.", pm.getTrainingNumber(),
        is("LDN/ABC.XYZ.123/1234567/D"));
  }

  @Test
  void shouldPopulateTrainingNumbersWhenTraineeProfileHasMultiplePms() {
    TraineeProfileDto traineeProfile = new TraineeProfileDto();
    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    traineeProfile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm1 = new ProgrammeMembershipDto();
    pm1.setManagingDeanery(OWNER_NAME);
    pm1.setProgrammeName(PROGRAMME_NAME);
    pm1.setProgrammeNumber(PROGRAMME_NUMBER);
    pm1.setTrainingPathway(TRAINING_PATHWAY);
    pm1.setStartDate(PAST);

    CurriculumDto curriculum1 = new CurriculumDto();
    curriculum1.setCurriculumSpecialtyCode("ABC");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(PAST);
    pm1.setCurricula(List.of(curriculum1));

    ProgrammeMembershipDto pm2 = new ProgrammeMembershipDto();
    pm2.setManagingDeanery(OWNER_NAME);
    pm2.setProgrammeName(PROGRAMME_NAME);
    pm2.setProgrammeNumber(PROGRAMME_NUMBER);
    pm2.setTrainingPathway(TRAINING_PATHWAY);
    pm2.setStartDate(NOW);

    CurriculumDto curriculum2 = new CurriculumDto();
    curriculum2.setCurriculumSpecialtyCode("123");
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumStartDate(NOW);
    curriculum2.setCurriculumEndDate(NOW);
    pm2.setCurricula(List.of(curriculum2));

    ProgrammeMembershipDto pm3 = new ProgrammeMembershipDto();
    pm3.setManagingDeanery(OWNER_NAME);
    pm3.setProgrammeName(PROGRAMME_NAME);
    pm3.setProgrammeNumber(PROGRAMME_NUMBER);
    pm3.setTrainingPathway(TRAINING_PATHWAY);
    pm3.setStartDate(FUTURE);

    CurriculumDto curriculum3 = new CurriculumDto();
    curriculum3.setCurriculumSpecialtyCode("XYZ");
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumStartDate(FUTURE);
    curriculum3.setCurriculumEndDate(FUTURE);
    pm3.setCurricula(List.of(curriculum3));

    traineeProfile.setProgrammeMemberships(List.of(pm1, pm2, pm3));

    service.populateTrainingNumbers(traineeProfile);

    assertThat("Unexpected training number.", pm1.getTrainingNumber(), nullValue());
    assertThat("Unexpected training number.", pm2.getTrainingNumber(), is("LDN/123/1234567/D"));
    assertThat("Unexpected training number.", pm3.getTrainingNumber(), is("LDN/XYZ/1234567/D"));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      Defence Postgraduate Medical Deanery                   | TSD
      East Midlands                                          | EMD
      East of England                                        | EAN
      Kent, Surrey and Sussex                                | KSS
      North Central and East London                          | LDN
      North East                                             | NTH
      North West                                             | NWE
      North West London                                      | LDN
      South London                                           | LDN
      South West                                             | SWN
      Thames Valley                                          | OXF
      Wessex                                                 | WES
      West Midlands                                          | WMD
      Yorkshire and the Humber                               | YHD
      London LETBs                                           | LDN
      """)
  void shouldPopulateTrainingNumberWithParentOrganizationWhenMappedByOwner(String ownerName,
      String ownerCode) {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(ownerName);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum = new CurriculumDto();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    String[] trainingNumberParts = trainingNumber.split("/");
    assertThat("Unexpected parent organization.", trainingNumberParts[0], is(ownerCode));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      AAA | ZZZ | 111 | 999 | ZZZ-AAA-999-111
      999 | 111 | ZZZ | AAA | ZZZ-AAA-999-111
      001 | 010 | 100 | 111 | 111-100-010-001
      """)
  void shouldPopulateTrainingNumberWithOrderedSpecialtyConcatWhenMultipleSpecialty(
      String specialty1,
      String specialty2, String specialty3, String specialty4, String trainingNumberPart) {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum1 = new CurriculumDto();
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumSpecialtyCode(specialty1);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(FUTURE);

    CurriculumDto curriculum2 = new CurriculumDto();
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumSpecialtyCode(specialty2);
    curriculum2.setCurriculumStartDate(PAST);
    curriculum2.setCurriculumEndDate(FUTURE);

    CurriculumDto curriculum3 = new CurriculumDto();
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumSpecialtyCode(specialty3);
    curriculum3.setCurriculumStartDate(PAST);
    curriculum3.setCurriculumEndDate(FUTURE);

    CurriculumDto curriculum4 = new CurriculumDto();
    curriculum4.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum4.setCurriculumSpecialtyCode(specialty4);
    curriculum4.setCurriculumStartDate(PAST);
    curriculum4.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3, curriculum4));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    String[] trainingNumberParts = trainingNumber.split("/");
    assertThat("Unexpected parent organization.", trainingNumberParts[1], is(trainingNumberPart));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 10})
  void shouldPopulateTrainingNumberWithDotNotatedSpecialtyConcatWhenHasSubSpecialties(
      int additionalCurriculaCount) {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    List<CurriculumDto> curricula = new ArrayList<>();
    pm.setCurricula(curricula);

    CurriculumDto curriculum1 = new CurriculumDto();
    curriculum1.setCurriculumName("A sub spec 1");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_SS);
    curriculum1.setCurriculumSpecialtyCode("888");
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(FUTURE);
    curricula.add(curriculum1);

    CurriculumDto curriculum2 = new CurriculumDto();
    curriculum2.setCurriculumName("A sub spec 2");
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_SS);
    curriculum2.setCurriculumSpecialtyCode("999");
    curriculum2.setCurriculumStartDate(PAST);
    curriculum2.setCurriculumEndDate(FUTURE);
    curricula.add(curriculum2);

    for (int i = 1; i <= additionalCurriculaCount; i++) {
      CurriculumDto additionalCurriculum = new CurriculumDto();
      additionalCurriculum.setCurriculumName("Not sub spec " + i);
      additionalCurriculum.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
      additionalCurriculum.setCurriculumSpecialtyCode(String.format("%03d", i));
      additionalCurriculum.setCurriculumStartDate(PAST);
      additionalCurriculum.setCurriculumEndDate(FUTURE);
      curricula.add(additionalCurriculum);
    }

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    String[] trainingNumberParts = trainingNumber.split("/");
    assertThat("Unexpected parent organization.", trainingNumberParts[1], endsWith(".999.888"));

    long dotCount = trainingNumberParts[1].chars().filter(ch -> ch == '.').count();
    assertThat("Unexpected sub specialty count.", dotCount, is(2L));
  }

  @Test
  void shouldPopulateTrainingNumberWithFixedSpecialtyConcatWhenFirstSpecialtyIsAft() {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum1 = new CurriculumDto();
    curriculum1.setCurriculumName("AFT");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumSpecialtyCode("ACA");
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(FUTURE);

    CurriculumDto curriculum2 = new CurriculumDto();
    curriculum2.setCurriculumName("Not AFT 2");
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumSpecialtyCode("003");
    curriculum2.setCurriculumStartDate(PAST);
    curriculum2.setCurriculumEndDate(FUTURE);

    CurriculumDto curriculum3 = new CurriculumDto();
    curriculum3.setCurriculumName("Not AFT 1");
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumSpecialtyCode("777");
    curriculum3.setCurriculumStartDate(PAST);
    curriculum3.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    String[] trainingNumberParts = trainingNumber.split("/");
    assertThat("Unexpected parent organization.", trainingNumberParts[1], is("ACA-FND"));
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
  void shouldFilterCurriculaWhenPopulatingTrainingNumberAndCurriculaEnding(String pastSpecialty,
      String endingSpecialty, String futureSpecialty) {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum1 = new CurriculumDto();
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumSpecialtyCode(pastSpecialty);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(PAST);

    CurriculumDto curriculum2 = new CurriculumDto();
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumSpecialtyCode(futureSpecialty);
    curriculum2.setCurriculumStartDate(FUTURE);
    curriculum2.setCurriculumEndDate(FUTURE);

    CurriculumDto curriculum3 = new CurriculumDto();
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumSpecialtyCode(endingSpecialty);
    curriculum3.setCurriculumStartDate(PAST);
    curriculum3.setCurriculumEndDate(NOW);

    CurriculumDto curriculum4 = new CurriculumDto();
    curriculum4.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum4.setCurriculumSpecialtyCode(null);
    curriculum4.setCurriculumStartDate(PAST);
    curriculum4.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3, curriculum4));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    String[] trainingNumberParts = trainingNumber.split("/");
    assertThat("Unexpected parent organization.", trainingNumberParts[1], is(endingSpecialty));
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
  void shouldFilterCurriculaWhenPopulatingTrainingNumberCurriculaCurrent(String pastSpecialty,
      String currentSpecialty, String futureSpecialty) {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum1 = new CurriculumDto();
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumSpecialtyCode(pastSpecialty);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(PAST);

    CurriculumDto curriculum2 = new CurriculumDto();
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumSpecialtyCode(futureSpecialty);
    curriculum2.setCurriculumStartDate(FUTURE);
    curriculum2.setCurriculumEndDate(FUTURE);

    CurriculumDto curriculum3 = new CurriculumDto();
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumSpecialtyCode(currentSpecialty);
    curriculum3.setCurriculumStartDate(PAST);
    curriculum3.setCurriculumEndDate(FUTURE);

    CurriculumDto curriculum4 = new CurriculumDto();
    curriculum4.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum4.setCurriculumSpecialtyCode(null);
    curriculum4.setCurriculumStartDate(PAST);
    curriculum4.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3, curriculum4));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    String[] trainingNumberParts = trainingNumber.split("/");
    assertThat("Unexpected parent organization.", trainingNumberParts[1], is(currentSpecialty));
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
  void shouldFilterCurriculaWhenPopulatingTrainingNumberAndCurriculaStarting(String pastSpecialty,
      String startingSpecialty, String futureSpecialty) {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum1 = new CurriculumDto();
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumSpecialtyCode(pastSpecialty);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(PAST);

    CurriculumDto curriculum2 = new CurriculumDto();
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumSpecialtyCode(futureSpecialty);
    curriculum2.setCurriculumStartDate(FUTURE);
    curriculum2.setCurriculumEndDate(FUTURE);

    CurriculumDto curriculum3 = new CurriculumDto();
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumSpecialtyCode(startingSpecialty);
    curriculum3.setCurriculumStartDate(NOW);
    curriculum3.setCurriculumEndDate(FUTURE);

    CurriculumDto curriculum4 = new CurriculumDto();
    curriculum4.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum4.setCurriculumSpecialtyCode(null);
    curriculum4.setCurriculumStartDate(PAST);
    curriculum4.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3, curriculum4));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    String[] trainingNumberParts = trainingNumber.split("/");
    assertThat("Unexpected parent organization.", trainingNumberParts[1], is(startingSpecialty));
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
  void shouldFilterCurriculaWhenPopulatingTrainingNumberAndProgrammeFuture(String currentSpecialty,
      String futureSpecialty, String farFutureSpecialty) {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(FUTURE);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum1 = new CurriculumDto();
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumSpecialtyCode(currentSpecialty);
    curriculum1.setCurriculumStartDate(NOW);
    curriculum1.setCurriculumEndDate(NOW);

    CurriculumDto curriculum2 = new CurriculumDto();
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumSpecialtyCode(futureSpecialty);
    curriculum2.setCurriculumStartDate(FUTURE);
    curriculum2.setCurriculumEndDate(FUTURE);

    CurriculumDto curriculum3 = new CurriculumDto();
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumSpecialtyCode(farFutureSpecialty);
    curriculum3.setCurriculumStartDate(FUTURE.plusDays(1));
    curriculum3.setCurriculumEndDate(FUTURE.plusDays(1));

    CurriculumDto curriculum4 = new CurriculumDto();
    curriculum4.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum4.setCurriculumSpecialtyCode(null);
    curriculum4.setCurriculumStartDate(PAST);
    curriculum4.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3, curriculum4));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    String[] trainingNumberParts = trainingNumber.split("/");
    assertThat("Unexpected parent organization.", trainingNumberParts[1], is(futureSpecialty));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      111 | AAA | AAA
      AAA | 111 | AAA
      AAA | AAA | 111
      AAA | 111 | 111
      111 | AAA | 111
      111 | 111 | AAA
      """)
  void shouldFilterCurriculaWhenPopulatingTrainingNumberAndDuplicateSpecialties(String specialty1,
      String specialty2, String specialty3) {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum1 = new CurriculumDto();
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumSpecialtyCode(specialty1);
    curriculum1.setCurriculumStartDate(NOW);
    curriculum1.setCurriculumEndDate(FUTURE);

    CurriculumDto curriculum2 = new CurriculumDto();
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumSpecialtyCode(specialty2);
    curriculum2.setCurriculumStartDate(NOW);
    curriculum2.setCurriculumEndDate(NOW);

    CurriculumDto curriculum3 = new CurriculumDto();
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumSpecialtyCode(specialty3);
    curriculum3.setCurriculumStartDate(PAST);
    curriculum3.setCurriculumEndDate(NOW);

    CurriculumDto curriculum4 = new CurriculumDto();
    curriculum4.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum4.setCurriculumSpecialtyCode(null);
    curriculum4.setCurriculumStartDate(PAST);
    curriculum4.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3, curriculum4));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    String[] trainingNumberParts = trainingNumber.split("/");
    assertThat("Unexpected parent organization.", trainingNumberParts[1], is("AAA-111"));
  }

  @Test
  void shouldPopulateTrainingNumberWithGmcNumberWhenValid() {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum = new CurriculumDto();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    String[] trainingNumberParts = trainingNumber.split("/");
    assertThat("Unexpected parent organization.", trainingNumberParts[2], is(GMC_NUMBER));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"abc", "12345678"})
  void shouldPopulateTrainingNumberWithGdcNumberWhenValidAndGmcInvalid(String gmcNumber) {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(gmcNumber);
    personalDetails.setGdcNumber(GDC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum = new CurriculumDto();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    String[] trainingNumberParts = trainingNumber.split("/");
    assertThat("Unexpected parent organization.", trainingNumberParts[2], is(GDC_NUMBER));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      CCT  | C
      CESR | CP
      N/A  | D
      """)
  void shouldPopulateTrainingNumberWithSuffixWhenMappedByTrainingPathway(String trainingPathway,
      String suffix) {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(trainingPathway);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum = new CurriculumDto();
    curriculum.setCurriculumSpecialtyCode(CURRICULUM_SPECIALTY_CODE);
    curriculum.setCurriculumStartDate(PAST);
    curriculum.setCurriculumEndDate(FUTURE);
    pm.setCurricula(List.of(curriculum));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    String[] trainingNumberParts = trainingNumber.split("/");
    assertThat("Unexpected parent organization.", trainingNumberParts[3], is(suffix));
  }

  @Test
  void shouldPopulateTrainingNumberWithSuffixWhenSpecialtyIsAcademic() {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum1 = new CurriculumDto();
    curriculum1.setCurriculumSpecialtyCode("123");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(FUTURE);

    CurriculumDto curriculum2 = new CurriculumDto();
    curriculum2.setCurriculumSpecialtyCode("ACA");
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumStartDate(PAST);
    curriculum2.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    String[] trainingNumberParts = trainingNumber.split("/");
    assertThat("Unexpected parent organization.", trainingNumberParts[3], is("C"));
  }

  @Test
  void shouldFilterCurriculaWhenPopulatingTrainingNumberWithSuffixAndCurriculaEnding() {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum1 = new CurriculumDto();
    curriculum1.setCurriculumName("Past");
    curriculum1.setCurriculumSpecialtyCode("ACA");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(PAST);

    CurriculumDto curriculum2 = new CurriculumDto();
    curriculum2.setCurriculumName("Ending");
    curriculum2.setCurriculumSpecialtyCode("123");
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumStartDate(PAST);
    curriculum2.setCurriculumEndDate(NOW);

    CurriculumDto curriculum3 = new CurriculumDto();
    curriculum3.setCurriculumName("Future");
    curriculum3.setCurriculumSpecialtyCode("ACA");
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumStartDate(FUTURE);
    curriculum3.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    String[] trainingNumberParts = trainingNumber.split("/");
    assertThat("Unexpected parent organization.", trainingNumberParts[3], is("D"));
  }

  @Test
  void shouldFilterCurriculaWhenPopulatingTrainingNumberWithSuffixAndCurriculaCurrent() {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum1 = new CurriculumDto();
    curriculum1.setCurriculumName("Past");
    curriculum1.setCurriculumSpecialtyCode("ACA");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(PAST);

    CurriculumDto curriculum2 = new CurriculumDto();
    curriculum2.setCurriculumName("Current");
    curriculum2.setCurriculumSpecialtyCode("123");
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumStartDate(PAST);
    curriculum2.setCurriculumEndDate(FUTURE);

    CurriculumDto curriculum3 = new CurriculumDto();
    curriculum3.setCurriculumName("Future");
    curriculum3.setCurriculumSpecialtyCode("ACA");
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumStartDate(FUTURE);
    curriculum3.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    String[] trainingNumberParts = trainingNumber.split("/");
    assertThat("Unexpected parent organization.", trainingNumberParts[3], is("D"));
  }

  @Test
  void shouldFilterCurriculaWhenPopulatingTrainingNumberWithSuffixAndCurriculaStarting() {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum1 = new CurriculumDto();
    curriculum1.setCurriculumName("Past");
    curriculum1.setCurriculumSpecialtyCode("ACA");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(PAST);

    CurriculumDto curriculum2 = new CurriculumDto();
    curriculum2.setCurriculumName("Starting");
    curriculum2.setCurriculumSpecialtyCode("123");
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumStartDate(NOW);
    curriculum2.setCurriculumEndDate(FUTURE);

    CurriculumDto curriculum3 = new CurriculumDto();
    curriculum3.setCurriculumName("Future");
    curriculum3.setCurriculumSpecialtyCode("ACA");
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumStartDate(FUTURE);
    curriculum3.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    String[] trainingNumberParts = trainingNumber.split("/");
    assertThat("Unexpected parent organization.", trainingNumberParts[3], is("D"));
  }

  @Test
  void shouldFilterCurriculaWhenPopulatingTrainingNumberWithSuffixAndProgrammeFuture() {
    TraineeProfileDto profile = new TraineeProfileDto();

    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(FUTURE);
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum1 = new CurriculumDto();
    curriculum1.setCurriculumName("Past");
    curriculum1.setCurriculumSpecialtyCode("ACA");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(PAST);

    CurriculumDto curriculum2 = new CurriculumDto();
    curriculum2.setCurriculumName("Past");
    curriculum2.setCurriculumSpecialtyCode("ACA");
    curriculum2.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum2.setCurriculumStartDate(NOW);
    curriculum2.setCurriculumEndDate(NOW);

    CurriculumDto curriculum3 = new CurriculumDto();
    curriculum3.setCurriculumName("Future");
    curriculum3.setCurriculumSpecialtyCode("123");
    curriculum3.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum3.setCurriculumStartDate(FUTURE);
    curriculum3.setCurriculumEndDate(FUTURE);

    CurriculumDto curriculum4 = new CurriculumDto();
    curriculum4.setCurriculumName("Future + 1");
    curriculum4.setCurriculumSpecialtyCode("ACA");
    curriculum4.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum4.setCurriculumStartDate(FUTURE.plusDays(1));
    curriculum4.setCurriculumEndDate(FUTURE.plusDays(1));

    pm.setCurricula(List.of(curriculum1, curriculum2, curriculum3, curriculum4));

    service.populateTrainingNumbers(profile);

    String trainingNumber = pm.getTrainingNumber();
    String[] trainingNumberParts = trainingNumber.split("/");
    assertThat("Unexpected parent organization.", trainingNumberParts[3], is("D"));
  }

  @Test
  void shouldReSignProgrammeMembershipAfterSettingTrainingNumber() throws JsonProcessingException {
    TraineeProfileDto profile = new TraineeProfileDto();
    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    pm.setSignature(new Signature(Duration.of(1, ChronoUnit.DAYS)));
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum1 = new CurriculumDto();
    curriculum1.setCurriculumSpecialtyCode("ABC");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1));

    service.populateTrainingNumbers(profile);
    verify(signatureServiceMock).signDto(profile.getProgrammeMemberships().get(0));
  }

  @Test
  void shouldNotReSignProgrammeMembershipWithoutOriginalSignatureAfterSettingTrainingNumber() {
    TraineeProfileDto profile = new TraineeProfileDto();
    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    //no signature
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum1 = new CurriculumDto();
    curriculum1.setCurriculumSpecialtyCode("ABC");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1));

    service.populateTrainingNumbers(profile);
    verifyNoInteractions(signatureServiceMock);
  }

  @Test
  void shouldThrowRuntimeExceptionIfErrorReSigningProgrammeMembershipAfterSettingTrainingNumber()
      throws JsonProcessingException {
    TraineeProfileDto profile = new TraineeProfileDto();
    PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setGmcNumber(GMC_NUMBER);
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembershipDto pm = new ProgrammeMembershipDto();
    pm.setManagingDeanery(OWNER_NAME);
    pm.setProgrammeName(PROGRAMME_NAME);
    pm.setProgrammeNumber(PROGRAMME_NUMBER);
    pm.setTrainingPathway(TRAINING_PATHWAY);
    pm.setStartDate(NOW);
    pm.setSignature(new Signature(Duration.of(1, ChronoUnit.DAYS)));
    profile.setProgrammeMemberships(List.of(pm));

    CurriculumDto curriculum1 = new CurriculumDto();
    curriculum1.setCurriculumSpecialtyCode("ABC");
    curriculum1.setCurriculumSubType(CURRICULUM_SUB_TYPE_MC);
    curriculum1.setCurriculumStartDate(PAST);
    curriculum1.setCurriculumEndDate(FUTURE);

    pm.setCurricula(List.of(curriculum1));

    doThrow(JsonProcessingException.class).when(signatureServiceMock).signDto(any());

    assertThrows(RuntimeException.class, () -> service.populateTrainingNumbers(profile));
  }
}
