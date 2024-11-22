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

package uk.nhs.hee.trainee.details.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType;

/**
 * A representation of a CCT calculation.
 *
 * @param id                  The ID of the calculation.
 * @param traineeId           The ID of the trainee associated with the calculation.
 * @param name                A localOffice for the calculation.
 * @param programmeMembership The programme membership data for the calculation.
 * @param changes             The CCT changes to be calculated.
 * @param created             When the calculation was created (auto-generated).
 * @param lastModified        When the calculation was last modified (auto-generated).
 */
@Document("CctCalculation")
@Builder
public record CctCalculation(
    @Id
    ObjectId id,
    @Indexed
    String traineeId,
    String name,
    CctProgrammeMembership programmeMembership,
    List<CctChange> changes,

    @CreatedDate
    Instant created,

    @LastModifiedDate
    Instant lastModified
) {

  /**
   * Programme membership data for a calculation.
   *
   * @param id        The ID of the programme membership.
   * @param name      The localOffice of the programme.
   * @param startDate The start date of the programme.
   * @param endDate   The end date of the programme.
   * @param wte       The whole time equivalent of the programme membership.
   */
  @Builder
  public record CctProgrammeMembership(
      @Indexed
      @Field("id")
      UUID id,
      String name,
      LocalDate startDate,
      LocalDate endDate,
      double wte) {

  }

  /**
   * A CCT changes associated with a calculation.
   *
   * @param type      The type of CCT change.
   * @param startDate The start date of the CCT change.
   * @param wte       The new desired whole time equivalent.
   */
  @Builder
  public record CctChange(
      CctChangeType type,
      LocalDate startDate,
      Double wte) {

  }
}
