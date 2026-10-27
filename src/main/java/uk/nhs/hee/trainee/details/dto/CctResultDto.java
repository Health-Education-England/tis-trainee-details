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

/**
 * The result of a CCT calculation.
 *
 * @param cctDate                  The calculated CCT date.
 * @param originalProgrammeEndDate The end date of the programme, before additional and accelerated
 *                                 training time.
 * @param adjustedProgrammeEndDate The end date of the programme, after additional and accelerated
 *                                 training time.
 * @param totalWteMonthsCompleted  The whole time equivalent months completed before the
 *                                 projection.
 * @param monthsRemaining          The months of training remaining, rounded to two decimal places.
 * @param assumedFullTimePeriods   The periods before the projection that no change covers, in
 *                                 start date order.
 * @param projection               The projection of the remaining training to the CCT date.
 */
@Builder
public record CctResultDto(

    LocalDate cctDate,

    LocalDate originalProgrammeEndDate,

    LocalDate adjustedProgrammeEndDate,

    double totalWteMonthsCompleted,

    double monthsRemaining,

    List<CctAssumedFullTimePeriodDto> assumedFullTimePeriods,

    CctProjectionDto projection) {

}
