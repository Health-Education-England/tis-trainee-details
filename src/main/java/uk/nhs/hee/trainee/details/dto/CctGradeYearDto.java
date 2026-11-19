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
import lombok.Builder;
import uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade;

/**
 * A year of the grade progression in a CCT calculation result.
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
