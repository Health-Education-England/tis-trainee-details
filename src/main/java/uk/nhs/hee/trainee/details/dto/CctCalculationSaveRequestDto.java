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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import uk.nhs.hee.trainee.details.dto.validation.CctCalculateRequestValidation;
import uk.nhs.hee.trainee.details.dto.validation.Create;

/**
 * A request to save a CCT calculation.
 *
 * @param id               The ID of the calculation, or null for a new calculation.
 * @param name             A name for the calculation.
 * @param programmeDetails The details of the programme.
 * @param changes          The changes to the programme.
 */
@Builder
@CctCalculateRequestValidation
public record CctCalculationSaveRequestDto(

    @Null(groups = Create.class)
    UUID id,

    @NotBlank
    String name,

    @NotNull
    @Valid
    CctProgrammeDetailsDto programmeDetails,

    @NotNull
    @Size(max = 200)
    @Valid
    List<@NotNull CctChangeDto> changes) implements CctCalculationInput {

}
