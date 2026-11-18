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
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.hibernate.validator.constraints.Range;
import uk.nhs.hee.trainee.details.dto.deserializer.WholeNumberDeserializer;
import uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade;

/**
 * Additional or accelerated training time in one grade.
 *
 * @param grade  The grade the training time applies to.
 * @param months The training time, in months.
 * @param notes  The reason for the training time, stripped of leading and trailing whitespace.
 */
@Builder
public record CctTrainingTimeDto(

    @NotNull(message = "Please choose a grade.")
    CctTrainingGrade grade,

    @NotNull(message = "Training time must be a whole number of months between 1 and 24.")
    @Range(min = 1, max = 24,
        message = "Training time must be a whole number of months between 1 and 24.")
    @JsonDeserialize(using = WholeNumberDeserializer.class)
    Integer months,

    @NotBlank(message = "Please enter a reason for the training time.")
    @Size(max = CctCalculationInput.MAX_NOTES_LENGTH,
        message = "Notes must be {max} characters or less")
    String notes) {

  public CctTrainingTimeDto {
    notes = notes == null ? null : notes.strip();
  }
}
