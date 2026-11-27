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
import static org.hamcrest.Matchers.is;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.DFT;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST1;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST2;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST3;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST4;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST5;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST6;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST8;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST9;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto.CctTrainingTimeDto;
import uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade;
import uk.nhs.hee.trainee.details.service.CctGradeProgression.Year;

class CctGradeProgressionTest {

  private CctGradeProgression gradeProgression;

  @BeforeEach
  void setUp() {
    gradeProgression = new CctGradeProgression();
  }

  @Test
  void shouldGiveEachYearTheNextGradeAndEndWithPartYear() {
    CctProgrammeDetailsDto programme = programmeBuilder(66, ST1).build();

    List<Year> years = gradeProgression.years(programme);

    assertThat("Unexpected years.", years, is(List.of(
        year(1, ST1, 12, 12),
        year(2, ST2, 12, 24),
        year(3, ST3, 12, 36),
        year(4, ST4, 12, 48),
        year(5, ST5, 12, 60),
        year(6, ST6, 6, 66))));
  }

  @Test
  void shouldExtendEighteenMonthFinalGradeAndDropFollowingYear() {
    CctProgrammeDetailsDto programme = programmeBuilder(66, ST1)
        .eighteenMonthFinalGrade(ST5)
        .build();

    List<Year> years = gradeProgression.years(programme);

    assertThat("Unexpected years.", years, is(List.of(
        year(1, ST1, 12, 12),
        year(2, ST2, 12, 24),
        year(3, ST3, 12, 36),
        year(4, ST4, 12, 48),
        new Year(5, ST5, null, true, false, 0, 0, 18, 66))));
  }

  @Test
  void shouldCarryTrainingTimeIntoLaterYearsUpToAdjustedLength() {
    CctProgrammeDetailsDto programme = programmeBuilder(30, ST1)
        .additionalTrainingTime(List.of(trainingTime(ST1, 2), trainingTime(ST1, 1)))
        .acceleratedTrainingTime(List.of(trainingTime(ST2, 2)))
        .build();

    List<Year> years = gradeProgression.years(programme);

    assertThat("Unexpected years.", years, is(List.of(
        new Year(1, ST1, null, false, false, 3, 0, 12, 15),
        new Year(2, ST2, null, false, false, 0, 2, 12, 25),
        year(3, ST3, 6, 31))));
  }

  @Test
  void shouldMoveLaterGradesOnAfterSkippedGrade() {
    CctProgrammeDetailsDto programme = programmeBuilder(36, ST1)
        .skippedGrade(ST2)
        .build();

    List<Year> years = gradeProgression.years(programme);

    assertThat("Unexpected years.", years, is(List.of(
        year(1, ST1, 12, 12),
        new Year(2, ST3, ST2, false, false, 0, 0, 12, 24),
        year(3, ST4, 12, 36))));
  }

  @Test
  void shouldUseGivenAdjustmentsInPlaceOfProgrammesOwn() {
    CctProgrammeDetailsDto programme = programmeBuilder(36, ST1)
        .skippedGrade(ST2)
        .eighteenMonthFinalGrade(ST2)
        .build();

    List<Year> years = gradeProgression.years(programme, ST3, null);

    assertThat("Unexpected years.", years, is(List.of(
        year(1, ST1, 12, 12),
        year(2, ST2, 12, 24),
        new Year(3, ST4, ST3, false, false, 0, 0, 12, 36))));
  }

  @Test
  void shouldNotSkipUnnumberedGrade() {
    CctProgrammeDetailsDto programme = programmeBuilder(24, DFT)
        .skippedGrade(DFT)
        .build();

    List<Year> years = gradeProgression.years(programme);

    assertThat("Unexpected years.", years, is(List.of(
        year(1, DFT, 12, 12),
        year(2, DFT, 12, 24))));
  }

  @Test
  void shouldCountUnnumberedGradeTrainingTimeOnlyOnItsLastYear() {
    CctProgrammeDetailsDto programme = programmeBuilder(24, DFT)
        .additionalTrainingTime(List.of(trainingTime(DFT, 1)))
        .acceleratedTrainingTime(List.of(trainingTime(DFT, 3)))
        .build();

    List<Year> years = gradeProgression.years(programme);

    assertThat("Unexpected years.", years, is(List.of(
        year(1, DFT, 12, 12),
        new Year(2, DFT, null, false, false, 1, 3, 12, 22))));
  }

  @Test
  void shouldExtendOnlyLastYearOfUnnumberedEighteenMonthFinalGrade() {
    CctProgrammeDetailsDto programme = programmeBuilder(30, DFT)
        .eighteenMonthFinalGrade(DFT)
        .build();

    List<Year> years = gradeProgression.years(programme);

    assertThat("Unexpected years.", years, is(List.of(
        year(1, DFT, 12, 12),
        new Year(2, DFT, null, true, false, 0, 0, 18, 30))));
  }

  @Test
  void shouldNotNameGradePastHighestKnownGrade() {
    CctProgrammeDetailsDto programme = programmeBuilder(36, ST8)
        .additionalTrainingTime(List.of(trainingTime(ST9, 1)))
        .build();

    List<Year> years = gradeProgression.years(programme);

    assertThat("Unexpected years.", years, is(List.of(
        year(1, ST8, 12, 12),
        new Year(2, ST9, null, false, false, 1, 0, 12, 25),
        new Year(3, null, null, false, true, 0, 0, 12, 37))));
  }

  private static CctProgrammeDetailsDto.CctProgrammeDetailsDtoBuilder programmeBuilder(
      int lengthMonths, CctTrainingGrade startGrade) {
    return CctProgrammeDetailsDto.builder()
        .specialty("Paediatrics")
        .startDate(LocalDate.parse("2023-08-02"))
        .lengthMonths(lengthMonths)
        .startGrade(startGrade)
        .additionalTrainingTime(List.of())
        .acceleratedTrainingTime(List.of());
  }

  private static CctTrainingTimeDto trainingTime(CctTrainingGrade grade, int months) {
    return CctTrainingTimeDto.builder().grade(grade).months(months).notes("why").build();
  }

  private static Year year(int yearNumber, CctTrainingGrade grade, int gradeMonths,
      int targetMonths) {
    return new Year(yearNumber, grade, null, false, false, 0, 0, gradeMonths, targetMonths);
  }
}
