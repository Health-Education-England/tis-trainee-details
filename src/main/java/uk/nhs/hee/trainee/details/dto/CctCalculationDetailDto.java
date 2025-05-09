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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.hibernate.validator.constraints.Range;
import uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType;
import uk.nhs.hee.trainee.details.dto.validation.Create;

/**
 * A representation of a CCT calculation's detail.
 *
 * @param id                  The ID of the calculation.
 * @param name                A name for the calculation.
 * @param programmeMembership The programme membership data for the calculation.
 * @param changes             The CCT changes to be calculated.
 * @param cctDate             The calculated CCT end date based on the changes.
 * @param created             When the calculation was created (auto-generated).
 * @param lastModified        When the calculation was last modified (auto-generated).
 */
@Builder
public record CctCalculationDetailDto(

    @Null(groups = Create.class)
    UUID id,

    @NotBlank
    String name,

    @NotNull
    @Valid
    CctProgrammeMembershipDto programmeMembership,

    @NotEmpty
    @Valid
    List<CctChangeDto> changes,

    LocalDate cctDate,

    Instant created,
    Instant lastModified) {

  /**
   * Programme membership data for a calculation.
   *
   * @param id        The ID of the programme membership.
   * @param name      The name of the programme.
   * @param startDate The start date of the programme.
   * @param endDate   The end date of the programme.
   * @param wte       The whole time equivalent of the programme membership.
   */
  @Builder
  public record CctProgrammeMembershipDto(

      @NotNull
      UUID id,

      @NotBlank
      String name,

      @NotNull
      LocalDate startDate,

      @NotNull
      LocalDate endDate,

      @NotNull
      @Range(min = 0, max = 1)
      Double wte,

      String designatedBodyCode,

      String managingDeanery) {

  }

  /**
   * A CCT changes associated with a calculation.
   *
   * @param id        The identifier of the CCT change.
   * @param type      The type of CCT change.
   * @param startDate The start date of the CCT change.
   * @param wte       The new desired whole time equivalent.
   */
  @Builder
  public record CctChangeDto(

      @Null(groups = Create.class)
      UUID id,

      @NotNull
      CctChangeType type,

      @NotNull
      LocalDate startDate,

      @NotNull
      @Range(min = 0, max = 1)
      Double wte) {

  }
}
