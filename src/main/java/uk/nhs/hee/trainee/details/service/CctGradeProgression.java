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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto;
import uk.nhs.hee.trainee.details.dto.CctTrainingTimeDto;
import uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade;

/**
 * The grade for each year of a CCT programme, following the NW Training End Date Calculator.
 */
@Service
public class CctGradeProgression {

  private static final int MONTHS_PER_YEAR = 12;
  private static final int EIGHTEEN_MONTH_EXTENSION = 6;
  private static final Pattern NUMBERED_GRADE = Pattern.compile("([A-Z]+)(\\d+)");

  /**
   * Get the grade for each year of a programme.
   *
   * @param details The details of the programme.
   * @return The years of the programme, in order.
   */
  public List<Year> years(CctProgrammeDetailsDto details) {
    return years(details, details.skippedGrade(), details.eighteenMonthFinalGrade());
  }

  /**
   * Get the grade for each year of a programme, with the given grade progression adjustments in
   * place of its own.
   *
   * @param details                 The details of the programme.
   * @param skippedGrade            The grade year that is skipped, if any.
   * @param eighteenMonthFinalGrade The grade with an 18-month final year, if any.
   * @return The years of the programme, in order.
   */
  public List<Year> years(CctProgrammeDetailsDto details, CctTrainingGrade skippedGrade,
      CctTrainingGrade eighteenMonthFinalGrade) {
    int lengthMonths = details.lengthMonths();
    int adjustedLengthMonths = lengthMonths + totalMonths(details.additionalTrainingTime())
        - totalMonths(details.acceleratedTrainingTime());

    List<Year> years = new ArrayList<>();
    int previousEighteenMonthExtension = 0;
    int carriedAdditionalMonths = 0;
    int carriedAcceleratedMonths = 0;
    int previousBaseMonths = 0;
    int skippedGradeOffset = 0;

    for (int yearNumber = 1; (yearNumber - 1) * MONTHS_PER_YEAR < lengthMonths; yearNumber++) {
      int gate = (yearNumber - 1) * MONTHS_PER_YEAR + 1 + previousEighteenMonthExtension;
      if (gate > lengthMonths) {
        previousEighteenMonthExtension = 0;
        continue;
      }

      boolean beginsAfterSkippedGrade = skippedGrade != null
          && isNumbered(details.startGrade())
          && gradeAfter(details.startGrade(), yearNumber - 1) == skippedGrade;
      if (beginsAfterSkippedGrade) {
        skippedGradeOffset = 1;
      }
      CctTrainingGrade grade = gradeAfter(details.startGrade(),
          yearNumber - 1 + skippedGradeOffset);

      boolean isEighteenMonthFinalYear = grade != null && grade == eighteenMonthFinalGrade;
      int finalYearExtension = isEighteenMonthFinalYear ? EIGHTEEN_MONTH_EXTENSION : 0;
      int additionalMonths = monthsIn(details.additionalTrainingTime(), grade);
      int acceleratedMonths = monthsIn(details.acceleratedTrainingTime(), grade);
      carriedAdditionalMonths += additionalMonths;
      carriedAcceleratedMonths += acceleratedMonths;

      int baseMonths = yearNumber * MONTHS_PER_YEAR + finalYearExtension;
      int targetMonths = Math.min(
          baseMonths + carriedAdditionalMonths - carriedAcceleratedMonths, adjustedLengthMonths);
      int gradeMonths = Math.min(baseMonths, lengthMonths) - previousBaseMonths;

      years.add(new Year(yearNumber, grade, beginsAfterSkippedGrade ? skippedGrade : null,
          isEighteenMonthFinalYear, grade == null, additionalMonths,
          acceleratedMonths, gradeMonths, targetMonths));

      previousEighteenMonthExtension = finalYearExtension;
      previousBaseMonths = Math.min(baseMonths, lengthMonths);
    }

    return years;
  }

  private static boolean isNumbered(CctTrainingGrade grade) {
    return NUMBERED_GRADE.matcher(grade.name()).matches();
  }

  private static CctTrainingGrade gradeAfter(CctTrainingGrade startGrade, int years) {
    Matcher start = NUMBERED_GRADE.matcher(startGrade.name());
    if (!start.matches()) {
      return startGrade;
    }

    String name = start.group(1) + (Integer.parseInt(start.group(2)) + years);
    return Arrays.stream(CctTrainingGrade.values())
        .filter(grade -> grade.name().equals(name))
        .findFirst()
        .orElse(null);
  }

  private static int monthsIn(List<CctTrainingTimeDto> entries, CctTrainingGrade grade) {
    return entries.stream()
        .filter(entry -> grade != null && entry.grade() == grade)
        .mapToInt(CctTrainingTimeDto::months)
        .sum();
  }

  private static int totalMonths(List<CctTrainingTimeDto> entries) {
    return entries.stream()
        .mapToInt(CctTrainingTimeDto::months)
        .sum();
  }

  /**
   * A year of a programme's grade progression.
   *
   * @param yearNumber                 The year of the programme, from 1.
   * @param grade                      The grade for the year, or null when it would be past the
   *                                   highest known grade.
   * @param skippedGradeBeforeThisRow  The grade skipped just before this year, if any.
   * @param extendedToEighteenMonths  Whether the grade is an 18-month final year.
   * @param exceedsKnownTrainingGrade Whether the year is past the highest known grade.
   * @param additionalMonths          The additional training time in the grade, in months.
   * @param acceleratedMonths         The accelerated training time in the grade, in months.
   * @param gradeMonths               The months in the year, before additional and accelerated
   *                                  training time.
   * @param targetMonths              The months of training completed when the year ends.
   */
  public record Year(
      int yearNumber,
      CctTrainingGrade grade,
      CctTrainingGrade skippedGradeBeforeThisRow,
      boolean extendedToEighteenMonths,
      boolean exceedsKnownTrainingGrade,
      int additionalMonths,
      int acceleratedMonths,
      int gradeMonths,
      int targetMonths) {

  }
}
