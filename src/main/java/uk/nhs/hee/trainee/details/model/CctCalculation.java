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
import lombok.With;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType;
import uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade;

/**
 * A representation of a CCT calculation.
 *
 * @param id               The ID of the calculation.
 * @param traineeId        The ID of the trainee associated with the calculation.
 * @param name             A name for the calculation.
 * @param programmeDetails The details of the programme.
 * @param changes          The changes to the programme, with the months they credit.
 * @param result           The result of the calculation.
 * @param created          When the calculation was created (auto-generated).
 * @param lastModified     When the calculation was last modified (auto-generated).
 */
@Document("CctCalculation")
@Builder
public record CctCalculation(
    @Id
    @With
    UUID id,
    @Indexed
    @With
    String traineeId,
    String name,
    CctProgrammeDetails programmeDetails,
    List<CctChange> changes,
    CctResult result,

    @CreatedDate
    @With
    Instant created,

    @With
    @LastModifiedDate
    Instant lastModified) implements UuidIdentifiedRecord<CctCalculation> {

  /**
   * The details of the programme a calculation is for.
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
  public record CctProgrammeDetails(
      String specialty,
      LocalDate startDate,
      Integer lengthMonths,
      CctTrainingGrade startGrade,
      String startGradeOverrideNotes,
      Integer additionalMonths,
      String additionalMonthsNotes,
      Integer acceleratedMonths,
      String acceleratedMonthsNotes,
      CctTrainingGrade eighteenMonthFinalGrade,
      String eighteenMonthFinalGradeNotes,
      CctTrainingGrade skippedGrade,
      String skippedGradeNotes) {

  }

  /**
   * A change to the programme a calculation is for, with the months it credits.
   *
   * @param type                    The type of change.
   * @param startDate               The start date of the change.
   * @param endDate                 The end date of the change, inclusive, or null for an LTFT
   *                                change that lasts for the rest of training.
   * @param wte                     The whole time equivalent of an LTFT change, or the approved
   *                                credit for an OOPR, as a fraction of full time.
   * @param countsTowardsCompletion Whether the change counts towards completion of training.
   * @param notes                   Notes about the change.
   * @param calendarMonths          The calendar months in the change, or null for an LTFT change
   *                                that lasts for the rest of training.
   * @param wteMonths               The whole time equivalent months the change credits, or null
   *                                for an LTFT change that lasts for the rest of training.
   */
  @Builder
  public record CctChange(
      CctChangeType type,
      LocalDate startDate,
      LocalDate endDate,
      Double wte,
      boolean countsTowardsCompletion,
      String notes,
      Double calendarMonths,
      Double wteMonths) {

  }

  /**
   * The result of a calculation.
   *
   * @param cctDate                  The calculated CCT date.
   * @param originalProgrammeEndDate The end date of the programme, before additional and
   *                                 accelerated training time.
   * @param adjustedProgrammeEndDate The end date of the programme, after additional and
   *                                 accelerated training time.
   * @param totalWteMonthsCompleted  The whole time equivalent months completed before the
   *                                 projection.
   * @param monthsRemaining          The months of training remaining, rounded to two decimal
   *                                 places.
   * @param assumedFullTimePeriods   The periods before the projection that no change covers, in
   *                                 start date order.
   * @param projection               The projection of the remaining training to the CCT date.
   */
  @Builder
  public record CctResult(
      LocalDate cctDate,
      LocalDate originalProgrammeEndDate,
      LocalDate adjustedProgrammeEndDate,
      double totalWteMonthsCompleted,
      double monthsRemaining,
      List<CctAssumedFullTimePeriod> assumedFullTimePeriods,
      CctProjection projection) {

  }

  /**
   * A period of training that no change covers, assumed to be full time.
   *
   * @param startDate The start date of the period.
   * @param endDate   The end date of the period, inclusive.
   * @param months    The months in the period, both calendar and whole time equivalent.
   */
  @Builder
  public record CctAssumedFullTimePeriod(
      LocalDate startDate,
      LocalDate endDate,
      double months) {

  }

  /**
   * The projection of the remaining training to the CCT date.
   *
   * @param startDate      The start date of the projection.
   * @param wte            The whole time equivalent of the projection, as a fraction of full
   *                       time.
   * @param calendarMonths The calendar months the remaining training takes at that whole time
   *                       equivalent.
   * @param wteMonths      The months of training remaining, rounded to two decimal places.
   */
  @Builder
  public record CctProjection(
      LocalDate startDate,
      double wte,
      double calendarMonths,
      double wteMonths) {

  }
}
