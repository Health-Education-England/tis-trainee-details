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

package uk.nhs.hee.trainee.details.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade;

/**
 * The result of a CCT calculation.
 *
 * @param cctDate                      The calculated CCT date.
 * @param originalProgrammeEndDate     The end date of the programme, before additional and
 *                                     accelerated training time.
 * @param adjustedProgrammeEndDate     The end date of the programme, after additional and
 *                                     accelerated training time.
 * @param totalCalendarMonthsCompleted The calendar months completed before the projection.
 * @param totalWteMonthsCompleted      The whole time equivalent months completed before the
 *                                     projection.
 * @param monthsRemaining              The months of training remaining, rounded to two decimal
 *                                     places.
 * @param assumedFullTimePeriods       The periods before the projection that no change covers, in
 *                                     start date order.
 * @param projection                   The projection of the remaining training to the CCT date.
 * @param gradeProgression             The grade for each year of the programme, and when it ends.
 */
@Builder
public record CctResultDto(

    LocalDate cctDate,

    LocalDate originalProgrammeEndDate,

    LocalDate adjustedProgrammeEndDate,

    double totalCalendarMonthsCompleted,

    double totalWteMonthsCompleted,

    double monthsRemaining,

    List<CctAssumedFullTimePeriodDto> assumedFullTimePeriods,

    CctProjectionDto projection,

    List<CctGradeYearDto> gradeProgression) {

  /**
   * A period of training that no change covers, assumed to be full time.
   *
   * @param startDate The start date of the period.
   * @param endDate   The end date of the period, inclusive.
   * @param months    The months in the period, both calendar and whole time equivalent.
   */
  @Builder
  public record CctAssumedFullTimePeriodDto(

      LocalDate startDate,

      LocalDate endDate,

      double months) {

  }

  /**
   * The projection of the remaining training to the CCT date.
   *
   * @param startDate      The start date of the projection.
   * @param wte            The whole time equivalent of the projection, as a fraction of full time.
   * @param calendarMonths The calendar months the remaining training takes at that whole time
   *                       equivalent.
   * @param wteMonths      The months of training remaining, rounded to two decimal places.
   */
  @Builder
  public record CctProjectionDto(

      LocalDate startDate,

      double wte,

      double calendarMonths,

      double wteMonths) {

  }

  /**
   * A year of the grade progression.
   *
   * @param yearNumber                The year of the programme, from 1.
   * @param grade                     The grade for the year, or null when it would be past the
   *                                  highest known grade.
   * @param endDate                   The date the grade ends.
   * @param skippedGradeBeforeThisRow The grade skipped just before this year, if any.
   * @param extendedToEighteenMonths  Whether the grade is an 18-month final year.
   * @param exceedsKnownTrainingGrade Whether the year is past the highest known grade.
   * @param additionalMonths          The additional training time in the grade, in months.
   * @param acceleratedMonths         The accelerated training time in the grade, in months.
   */
  @Builder
  public record CctGradeYearDto(

      int yearNumber,

      CctTrainingGrade grade,

      LocalDate endDate,

      CctTrainingGrade skippedGradeBeforeThisRow,

      boolean extendedToEighteenMonths,

      boolean exceedsKnownTrainingGrade,

      int additionalMonths,

      int acceleratedMonths) {

  }
}
