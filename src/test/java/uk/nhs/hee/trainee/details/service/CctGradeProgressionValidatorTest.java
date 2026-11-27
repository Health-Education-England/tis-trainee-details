/*
 * The MIT License (MIT)
 *
 * Copyright 2026 Crown Copyright (Health Education England)
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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.DFT;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST1;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST2;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST3;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST7;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST8;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST9;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.validation.FieldError;
import uk.nhs.hee.trainee.details.dto.BodyValidationError;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto.CctProgrammeDetailsDtoBuilder;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto.CctTrainingTimeDto;
import uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade;

class CctGradeProgressionValidatorTest {

  private CctGradeProgressionValidator validator;

  @BeforeEach
  void setUp() {
    validator = new CctGradeProgressionValidator(new CctGradeProgression());
  }

  @Test
  void shouldHaveNoErrorsWhenGradesInProgression() {
    List<FieldError> errors = validator.validate(programme().build());

    assertThat("Unexpected errors.", errors, is(empty()));
  }

  @Test
  void shouldHaveErrorsWhenGradesNotInProgression() {
    CctProgrammeDetailsDto details = programme()
        .additionalTrainingTime(trainingTime(ST8, 2))
        .acceleratedTrainingTime(trainingTime(ST9, 1))
        .eighteenMonthFinalGrade(ST9)
        .skippedGrade(ST8)
        .build();

    List<FieldError> errors = validator.validate(details);

    assertThat("Unexpected errors.", BodyValidationError.fromFieldErrors(errors), is(List.of(
        new BodyValidationError("#/programmeDetails/acceleratedTrainingTime/0/grade",
            "ST9 is not part of this programme's grade progression."),
        new BodyValidationError("#/programmeDetails/additionalTrainingTime/0/grade",
            "ST8 is not part of this programme's grade progression."),
        new BodyValidationError("#/programmeDetails/eighteenMonthFinalGrade",
            "ST9 is not part of this programme's grade progression."),
        new BodyValidationError("#/programmeDetails/skippedGrade",
            "ST8 is not part of this programme's grade progression."))));
  }

  @Test
  void shouldHaveErrorWhenEighteenMonthFinalGradeIsSkipped() {
    CctProgrammeDetailsDto details = programme()
        .eighteenMonthFinalGrade(ST3)
        .skippedGrade(ST3)
        .build();

    List<FieldError> errors = validator.validate(details);

    assertThat("Unexpected errors.", BodyValidationError.fromFieldErrors(errors), is(List.of(
        new BodyValidationError("#/programmeDetails/eighteenMonthFinalGrade",
            "ST3 is not part of this programme's grade progression."))));
  }

  @ParameterizedTest
  @EnumSource(value = CctTrainingGrade.class, names = {"ST1", "DFT"})
  void shouldHaveErrorWhenStartGradeIsSkipped(CctTrainingGrade grade) {
    CctProgrammeDetailsDto details = programme()
        .startGrade(grade)
        .skippedGrade(grade)
        .build();

    List<FieldError> errors = validator.validate(details);

    assertThat("Unexpected errors.", BodyValidationError.fromFieldErrors(errors), is(List.of(
        new BodyValidationError("#/programmeDetails/skippedGrade",
            "The start grade cannot be skipped."))));
  }

  @ParameterizedTest
  @CsvSource({"90, ST5", "90, ST8", "84, ST7", "84, ST3"})
  void shouldHaveErrorWhenEighteenMonthGradeIsNotLastFullYearBeforeHalfYear(int lengthMonths,
      CctTrainingGrade grade) {
    CctProgrammeDetailsDto details = programme()
        .lengthMonths(lengthMonths)
        .eighteenMonthFinalGrade(grade)
        .build();

    List<FieldError> errors = validator.validate(details);

    assertThat("Unexpected errors.", BodyValidationError.fromFieldErrors(errors), is(List.of(
        new BodyValidationError("#/programmeDetails/eighteenMonthFinalGrade",
            "An 18-month final year must be the last full year of a programme length that "
                + "includes a half year, such as 66 months."))));
  }

  @Test
  void shouldHaveNoErrorsWhenEighteenMonthGradeIsLastFullYearBeforeHalfYear() {
    CctProgrammeDetailsDto details = programme()
        .lengthMonths(90)
        .eighteenMonthFinalGrade(ST7)
        .build();

    List<FieldError> errors = validator.validate(details);

    assertThat("Unexpected errors.", errors, is(empty()));
  }

  @Test
  void shouldHaveNoErrorsWhenEighteenMonthGradeIsUnnumberedFinalGrade() {
    CctProgrammeDetailsDto details = programme()
        .lengthMonths(30)
        .startGrade(DFT)
        .eighteenMonthFinalGrade(DFT)
        .build();

    List<FieldError> errors = validator.validate(details);

    assertThat("Unexpected errors.", errors, is(empty()));
  }

  @ParameterizedTest
  @CsvSource({"84, ST1, ST2, 12", "90, ST1, ST8, 6", "24, DFT, DFT, 12", "30, DFT, DFT, 6"})
  void shouldHaveErrorWhenAcceleratedTimeFillsGrade(int lengthMonths, CctTrainingGrade startGrade,
      CctTrainingGrade grade, int gradeMonths) {
    List<CctTrainingTimeDto> accelerated = List.of(
        trainingTime(grade, gradeMonths - 1).get(0),
        trainingTime(grade, 1).get(0));
    CctProgrammeDetailsDto details = programme()
        .lengthMonths(lengthMonths)
        .startGrade(startGrade)
        .acceleratedTrainingTime(accelerated)
        .build();

    List<FieldError> errors = validator.validate(details);

    assertThat("Unexpected errors.", BodyValidationError.fromFieldErrors(errors), is(List.of(
        new BodyValidationError("#/programmeDetails/acceleratedTrainingTime/0/months",
            "Accelerated training time in " + grade + " must be less than " + gradeMonths
                + " months."))));
  }

  @Test
  void shouldHaveNoErrorsWhenAcceleratedTimeLessThanGradeAndItsAdditionalTime() {
    CctProgrammeDetailsDto details = programme()
        .additionalTrainingTime(trainingTime(ST2, 1))
        .acceleratedTrainingTime(trainingTime(ST2, 12))
        .build();

    List<FieldError> errors = validator.validate(details);

    assertThat("Unexpected errors.", errors, is(empty()));
  }

  private static CctProgrammeDetailsDtoBuilder programme() {
    return CctProgrammeDetailsDto.builder()
        .specialty("Paediatrics")
        .startDate(LocalDate.of(2023, 8, 2))
        .lengthMonths(84)
        .startGrade(ST1)
        .additionalTrainingTime(List.of())
        .acceleratedTrainingTime(List.of());
  }

  private static List<CctTrainingTimeDto> trainingTime(CctTrainingGrade grade, int months) {
    return List.of(CctTrainingTimeDto.builder().grade(grade).months(months).notes("why").build());
  }
}
