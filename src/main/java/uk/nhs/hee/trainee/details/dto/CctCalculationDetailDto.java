/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

/**
 * A representation of a CCT calculation's detail.
 *
 * @param id               The ID of the calculation.
 * @param name             A name for the calculation.
 * @param programmeDetails The details of the programme.
 * @param changes          The changes to the programme, with the months they credit.
 * @param result           The result of the calculation.
 * @param created          When the calculation was created (auto-generated).
 * @param lastModified     When the calculation was last modified (auto-generated).
 */
@Builder
public record CctCalculationDetailDto(

    UUID id,

    String name,

    CctProgrammeDetailsDto programmeDetails,

    List<CctCalculatedChangeDto> changes,

    CctResultDto result,

    Instant created,
    Instant lastModified) {

}
