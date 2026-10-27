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

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Builder;
import uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType;

/**
 * A change to the programme a CCT date is calculated for.
 *
 * @param type                    The type of change.
 * @param startDate               The start date of the change.
 * @param endDate                 The end date of the change, inclusive, or null for an LTFT change
 *                                that lasts for the rest of training.
 * @param wte                     The whole time equivalent of an LTFT change, or the approved
 *                                credit for an OOPR, as a fraction of full time.
 * @param countsTowardsCompletion Whether the change counts towards completion of training.
 * @param notes                   Notes about the change.
 */
@Builder
public record CctChangeDto(

    @NotNull(message = "Please choose a change type.")
    CctChangeType type,

    LocalDate startDate,

    LocalDate endDate,

    Double wte,

    boolean countsTowardsCompletion,

    String notes) {

}
