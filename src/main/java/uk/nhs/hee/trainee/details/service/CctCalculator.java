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

package uk.nhs.hee.trainee.details.service;

import static java.time.temporal.ChronoUnit.DAYS;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.LTFT;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.OOPT;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.dto.CctCalculateResponseDto;
import uk.nhs.hee.trainee.details.dto.CctCalculatedChangeDto;
import uk.nhs.hee.trainee.details.dto.CctCalculatedChangeDto.CctChangeDto;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto;
import uk.nhs.hee.trainee.details.dto.CctResultDto;
import uk.nhs.hee.trainee.details.dto.CctResultDto.CctAssumedFullTimePeriodDto;
import uk.nhs.hee.trainee.details.dto.CctResultDto.CctProjectionDto;

/**
 * A calculator for CCT dates.
 */
@Service
public class CctCalculator {

  private static final double PROJECTED_DAYS_PER_MONTH = 30.4;
  private static final double RECORDED_DAYS_PER_MONTH = 365.0 / 12;
  private static final double FLOOR_TOLERANCE_DAYS = 1e-9;

  /**
   * Calculate the CCT date for a programme and its changes.
   *
   * @param programmeDetails The details of the programme.
   * @param changes          The changes to the programme.
   * @return The changes, with the months they credit, and the CCT date calculated from them.
   */
  public CctCalculateResponseDto calculate(CctProgrammeDetailsDto programmeDetails,
      List<CctChangeDto> changes) {
    LocalDate programmeStartDate = programmeDetails.startDate();
    List<CctChangeDto> countedChanges = changes.stream()
        .flatMap(change -> countedChange(change, programmeStartDate).stream())
        .toList();
    List<CctChangeDto> datedChanges = countedChanges.stream()
        .filter(change -> !isOpenLtftChange(change))
        .toList();
    Projection projection = deriveProjection(programmeStartDate, countedChanges, datedChanges);
    List<CctAssumedFullTimePeriodDto> assumedPeriods =
        deriveAssumedPeriods(programmeStartDate, datedChanges, projection.startDate());

    double totalCalendarMonthsCompleted = 0;
    double totalWteMonthsCompleted = 0;
    for (CctChangeDto change : datedChanges) {
      totalCalendarMonthsCompleted += calendarMonths(change);
      totalWteMonthsCompleted += wteMonths(change);
    }
    for (CctAssumedFullTimePeriodDto period : assumedPeriods) {
      totalCalendarMonthsCompleted += period.months();
      totalWteMonthsCompleted += period.months();
    }

    int adjustedLengthMonths = programmeDetails.lengthMonths()
        + programmeDetails.additionalMonths() - programmeDetails.acceleratedMonths();
    double monthsRemaining = roundToTwoDecimals(adjustedLengthMonths - totalWteMonthsCompleted);

    double calendarMonthsToCompletion = monthsRemaining / projection.wte();
    LocalDate cctDate = findCompletionDate(adjustedLengthMonths, datedChanges, assumedPeriods)
        .orElse(dateAfterMonths(projection.startDate(), calendarMonthsToCompletion));

    CctResultDto result = CctResultDto.builder()
        .cctDate(cctDate)
        .originalProgrammeEndDate(
            dateAfterMonths(programmeStartDate, programmeDetails.lengthMonths()))
        .adjustedProgrammeEndDate(dateAfterMonths(programmeStartDate, adjustedLengthMonths))
        .totalCalendarMonthsCompleted(totalCalendarMonthsCompleted)
        .totalWteMonthsCompleted(totalWteMonthsCompleted)
        .monthsRemaining(monthsRemaining)
        .assumedFullTimePeriods(assumedPeriods)
        .projection(CctProjectionDto.builder()
            .startDate(projection.startDate())
            .wte(projection.wte())
            .calendarMonths(calendarMonthsToCompletion)
            .wteMonths(monthsRemaining)
            .build())
        .build();

    return CctCalculateResponseDto.builder()
        .changes(changes.stream()
            .map(change -> calculatedChange(change, programmeStartDate))
            .toList())
        .result(result)
        .build();
  }

  /**
   * Adjust a change to the programme start and discard changes completed before it.
   *
   * @param change             The change to count.
   * @param programmeStartDate The programme start date.
   * @return The change adjusted to the programme, or empty when it does not overlap it.
   */
  private static Optional<CctChangeDto> countedChange(CctChangeDto change,
      LocalDate programmeStartDate) {
    if (!change.startDate().isBefore(programmeStartDate)) {
      return Optional.of(change);
    }
    if (change.endDate() != null && change.endDate().isBefore(programmeStartDate)) {
      return Optional.empty();
    }
    return Optional.of(change.withStartDate(programmeStartDate));
  }

  /**
   * Determine whether a change is an open-ended LTFT period.
   *
   * @param change The change to inspect.
   * @return {@code true} when the change is an open-ended LTFT period.
   */
  private static boolean isOpenLtftChange(CctChangeDto change) {
    return change.type() == LTFT && change.endDate() == null;
  }

  /**
   * Derive the projection used after the recorded training periods.
   *
   * @param programmeStartDate The programme start date.
   * @param changes            All counted changes.
   * @param datedChanges       Counted changes with an end date.
   * @return The projection start date and WTE.
   */
  private static Projection deriveProjection(LocalDate programmeStartDate,
      List<CctChangeDto> changes, List<CctChangeDto> datedChanges) {
    Optional<CctChangeDto> openLtftChange = changes.stream()
        .filter(CctCalculator::isOpenLtftChange)
        .findFirst();
    if (openLtftChange.isPresent()) {
      return new Projection(openLtftChange.get().startDate(), openLtftChange.get().wte());
    }

    LocalDate startDate = datedChanges.stream()
        .map(CctChangeDto::endDate)
        .max(Comparator.naturalOrder())
        .map(endDate -> endDate.plusDays(1))
        .orElse(programmeStartDate);
    return new Projection(startDate, 1);
  }

  /**
   * Find periods that are assumed to have been completed full time between changes.
   *
   * @param programmeStartDate  The programme start date.
   * @param datedChanges        Counted changes with an end date.
   * @param projectionStartDate The start date of the projection.
   * @return The assumed full-time periods.
   */
  private static List<CctAssumedFullTimePeriodDto> deriveAssumedPeriods(
      LocalDate programmeStartDate, List<CctChangeDto> datedChanges,
      LocalDate projectionStartDate) {
    List<CctChangeDto> sortedChanges = datedChanges.stream()
        .sorted(Comparator.comparing(CctChangeDto::startDate))
        .toList();
    List<CctAssumedFullTimePeriodDto> periods = new ArrayList<>();
    LocalDate cursor = programmeStartDate;

    for (CctChangeDto change : sortedChanges) {
      if (change.startDate().isAfter(cursor)) {
        periods.add(assumedPeriod(cursor, change.startDate().minusDays(1)));
      }

      LocalDate dayAfterChange = change.endDate().plusDays(1);
      if (dayAfterChange.isAfter(cursor)) {
        cursor = dayAfterChange;
      }
    }

    if (cursor.isBefore(projectionStartDate)) {
      periods.add(assumedPeriod(cursor, projectionStartDate.minusDays(1)));
    }
    return periods;
  }

  /**
   * Calculate the credited calendar and WTE months for a change.
   *
   * @param change             The change to calculate.
   * @param programmeStartDate The programme start date.
   * @return The calculated change.
   */
  private static CctCalculatedChangeDto calculatedChange(CctChangeDto change,
      LocalDate programmeStartDate) {
    if (isOpenLtftChange(change)) {
      return CctCalculatedChangeDto.builder()
          .change(change)
          .build();
    }

    Optional<CctChangeDto> countedChange = countedChange(change, programmeStartDate);
    return CctCalculatedChangeDto.builder()
        .change(change)
        .calendarMonths(countedChange.map(CctCalculator::calendarMonths).orElse(0.0))
        .wteMonths(countedChange.map(CctCalculator::wteMonths).orElse(0.0))
        .build();
  }

  /**
   * Create an assumed full-time period for a date range.
   *
   * @param startDate The period start date.
   * @param endDate   The period end date.
   * @return The assumed full-time period.
   */
  private static CctAssumedFullTimePeriodDto assumedPeriod(LocalDate startDate,
      LocalDate endDate) {
    return CctAssumedFullTimePeriodDto.builder()
        .startDate(startDate)
        .endDate(endDate)
        .months(recordedMonths(startDate, endDate))
        .build();
  }

  /**
   * Find the date on which the required programme length is completed.
   *
   * @param lengthMonths   The adjusted programme length in months.
   * @param datedChanges   Counted changes with an end date.
   * @param assumedPeriods Periods assumed to have been completed full time.
   * @return The completion date, if it falls within the supplied periods.
   */
  private static Optional<LocalDate> findCompletionDate(int lengthMonths,
      List<CctChangeDto> datedChanges, List<CctAssumedFullTimePeriodDto> assumedPeriods) {
    List<TrainingPeriod> sortedPeriods = Stream.concat(
            datedChanges.stream().map(change -> new TrainingPeriod(
                change.startDate(), change.endDate(), wteMonths(change))),
            assumedPeriods.stream().map(period -> new TrainingPeriod(
                period.startDate(), period.endDate(), period.months())))
        .sorted(Comparator.comparing(TrainingPeriod::startDate))
        .toList();
    double monthsCompleted = 0;

    for (TrainingPeriod period : sortedPeriods) {
      if (period.wteMonths() == 0) {
        continue;
      }

      long days = inclusiveDays(period.startDate(), period.endDate());
      for (long daysIn = 1; daysIn <= days; daysIn++) {
        double remaining = lengthMonths - monthsCompleted - period.wteMonths() * daysIn / days;
        if (roundToTwoDecimals(remaining) <= 0) {
          return Optional.of(period.startDate().plusDays(daysIn - 1));
        }
      }
      monthsCompleted += period.wteMonths();
    }
    return Optional.empty();
  }

  /**
   * Calculate the calendar months covered by a change.
   *
   * @param change The change to calculate.
   * @return The calendar months covered by the change.
   */
  private static double calendarMonths(CctChangeDto change) {
    return recordedMonths(change.startDate(), change.endDate());
  }

  /**
   * Calculate the WTE months credited by a change.
   *
   * @param change The change to calculate.
   * @return The WTE months credited by the change.
   */
  private static double wteMonths(CctChangeDto change) {
    return calendarMonths(change) * wteFraction(change);
  }

  /**
   * Determine the WTE fraction credited by a change.
   *
   * @param change The change to inspect.
   * @return The credited WTE fraction.
   */
  private static double wteFraction(CctChangeDto change) {
    if (!change.countsTowardsCompletion()) {
      return 0;
    }
    return change.type() == OOPT ? 1 : change.wte();
  }

  /**
   * Convert an inclusive date range to recorded months.
   *
   * @param startDate The range start date.
   * @param endDate   The range end date.
   * @return The number of recorded months.
   */
  private static double recordedMonths(LocalDate startDate, LocalDate endDate) {
    return inclusiveDays(startDate, endDate) / RECORDED_DAYS_PER_MONTH;
  }

  /**
   * Count the days in an inclusive date range.
   *
   * @param startDate The range start date.
   * @param endDate   The range end date.
   * @return The number of days in the range.
   */
  private static long inclusiveDays(LocalDate startDate, LocalDate endDate) {
    return DAYS.between(startDate, endDate) + 1;
  }

  /**
   * Calculate a projected date using the configured average days per month.
   *
   * @param startDate The projection start date.
   * @param months    The number of months to add.
   * @return The projected date.
   */
  private static LocalDate dateAfterMonths(LocalDate startDate, double months) {
    return startDate.plusDays(
        (long) Math.floor(months * PROJECTED_DAYS_PER_MONTH + FLOOR_TOLERANCE_DAYS));
  }

  /**
   * Round a month value to two decimal places.
   *
   * @param months The value to round.
   * @return The rounded value.
   */
  private static double roundToTwoDecimals(double months) {
    return Math.round(months * 100) / 100.0;
  }

  private record Projection(LocalDate startDate, double wte) {

  }

  private record TrainingPeriod(LocalDate startDate, LocalDate endDate, double wteMonths) {

  }
}
