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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Builder;
import org.hibernate.validator.constraints.Range;
import uk.nhs.hee.trainee.details.dto.deserializer.WholeNumberDeserializer;
import uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade;

/**
 * The details of the programme a CCT date is calculated for.
 *
 * @param specialty                    The name of the programme's specialty.
 * @param startDate                    The start date of the programme.
 * @param lengthMonths                 The length of the programme, in months.
 * @param startGrade                   The grade the programme starts at.
 * @param startGradeOverrideNotes      The reason the start grade differs from the specialty's.
 * @param additionalMonths             The additional training time, in months.
 * @param additionalMonthsNotes        The reason for the additional training time.
 * @param acceleratedMonths            The accelerated training time, in months.
 * @param acceleratedMonthsNotes       The reason for the accelerated training time.
 * @param eighteenMonthFinalGrade      The grade with an 18-month final year, if any.
 * @param eighteenMonthFinalGradeNotes The reason for the 18-month final year.
 * @param skippedGrade                 The grade year that is skipped, if any.
 * @param skippedGradeNotes            The reason for skipping the grade year.
 */
@Builder
public record CctProgrammeDetailsDto(

    @NotBlank(message = "Please choose a specialty.")
    String specialty,

    @NotNull(message = "Please enter a programme start date.")
    LocalDate startDate,

    @NotNull(message = "Programme length must be a whole number of months between 1 and 120.")
    @Range(min = 1, max = 120,
        message = "Programme length must be a whole number of months between 1 and 120.")
    @JsonDeserialize(using = WholeNumberDeserializer.class)
    Integer lengthMonths,

    @NotNull(message = "Please choose a start grade.")
    CctTrainingGrade startGrade,

    String startGradeOverrideNotes,

    @NotNull(message =
        "Additional training time must be a whole number of months between 0 and 24.")
    @Range(min = 0, max = 24,
        message = "Additional training time must be a whole number of months between 0 and 24.")
    @JsonDeserialize(using = WholeNumberDeserializer.class)
    Integer additionalMonths,

    String additionalMonthsNotes,

    @NotNull(message =
        "Accelerated training time must be a whole number of months between 0 and 12.")
    @Range(min = 0, max = 12,
        message = "Accelerated training time must be a whole number of months between 0 and 12.")
    @JsonDeserialize(using = WholeNumberDeserializer.class)
    Integer acceleratedMonths,

    String acceleratedMonthsNotes,

    CctTrainingGrade eighteenMonthFinalGrade,

    String eighteenMonthFinalGradeNotes,

    CctTrainingGrade skippedGrade,

    String skippedGradeNotes) {

}
