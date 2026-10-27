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

import com.amazonaws.xray.spring.aop.XRayEnabled;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.dto.CctAssumedFullTimePeriodDto;
import uk.nhs.hee.trainee.details.dto.CctCalculateResponseDto;
import uk.nhs.hee.trainee.details.dto.CctCalculatedChangeDto;
import uk.nhs.hee.trainee.details.dto.CctChangeDto;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto;
import uk.nhs.hee.trainee.details.dto.CctProjectionDto;
import uk.nhs.hee.trainee.details.dto.CctResultDto;

/**
 * A calculator for CCT dates.
 */
@Service
@XRayEnabled
public class CctEngine {

  private static final double DAYS_PER_MONTH = 30.4;
  private static final double COMPLETED_PERIOD_DAYS_PER_MONTH = 365.0 / 12;
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
    List<CctChangeDto> datedChanges = changes.stream()
        .filter(change -> !isOpenLtftChange(change))
        .toList();
    Projection projection = deriveProjection(programmeStartDate, changes, datedChanges);
    double calendarMonthsBeforeProjection =
        DAYS.between(programmeStartDate, projection.startDate()) / COMPLETED_PERIOD_DAYS_PER_MONTH;

    double changesCalendarMonths = 0;
    double changesWteMonths = 0;
    for (CctChangeDto change : datedChanges) {
      changesCalendarMonths += calendarMonths(change);
      changesWteMonths += wteMonths(change);
    }
    double gapCalendarMonths = Math.max(0, calendarMonthsBeforeProjection - changesCalendarMonths);
    double totalWteMonthsCompleted = changesWteMonths + gapCalendarMonths;

    int adjustedLengthMonths = programmeDetails.lengthMonths()
        + programmeDetails.additionalMonths() - programmeDetails.acceleratedMonths();
    double monthsRemaining = roundToTwoDecimals(adjustedLengthMonths - totalWteMonthsCompleted);

    List<CctAssumedFullTimePeriodDto> assumedPeriods =
        deriveAssumedPeriods(programmeStartDate, datedChanges, projection.startDate());

    double calendarMonthsToCompletion = monthsRemaining / projection.wte();
    LocalDate cctDate = findCompletionDate(adjustedLengthMonths, datedChanges, assumedPeriods)
        .orElse(dateAfterMonths(projection.startDate(), calendarMonthsToCompletion));

    CctResultDto result = CctResultDto.builder()
        .cctDate(cctDate)
        .originalProgrammeEndDate(
            dateAfterMonths(programmeStartDate, programmeDetails.lengthMonths()))
        .adjustedProgrammeEndDate(dateAfterMonths(programmeStartDate, adjustedLengthMonths))
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
        .changes(changes.stream().map(CctEngine::calculatedChange).toList())
        .result(result)
        .build();
  }

  private static boolean isOpenLtftChange(CctChangeDto change) {
    return change.type() == LTFT && change.endDate() == null;
  }

  private static Projection deriveProjection(LocalDate programmeStartDate,
      List<CctChangeDto> changes, List<CctChangeDto> datedChanges) {
    Optional<CctChangeDto> openLtftChange = changes.stream()
        .filter(CctEngine::isOpenLtftChange)
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

  private static List<CctAssumedFullTimePeriodDto> deriveAssumedPeriods(
      LocalDate programmeStartDate, List<CctChangeDto> datedChanges,
      LocalDate projectionStartDate) {
    List<CctChangeDto> sortedChanges = datedChanges.stream()
        .sorted(Comparator.comparing(CctChangeDto::startDate))
        .toList();
    List<CctAssumedFullTimePeriodDto> periods = new ArrayList<>();
    LocalDate cursor = programmeStartDate;

    for (CctChangeDto change : sortedChanges) {
      if (!cursor.isBefore(projectionStartDate)) {
        break;
      }

      if (change.startDate().isAfter(cursor)) {
        LocalDate dayBeforeChange = change.startDate().minusDays(1);
        LocalDate gapEnd = dayBeforeChange.isBefore(projectionStartDate)
            ? dayBeforeChange
            : projectionStartDate.minusDays(1);
        periods.add(assumedPeriod(cursor, gapEnd));
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

  private static CctCalculatedChangeDto calculatedChange(CctChangeDto change) {
    if (isOpenLtftChange(change)) {
      return CctCalculatedChangeDto.builder()
          .change(change)
          .build();
    }

    return CctCalculatedChangeDto.builder()
        .change(change)
        .calendarMonths(calendarMonths(change))
        .wteMonths(wteMonths(change))
        .build();
  }

  private static CctAssumedFullTimePeriodDto assumedPeriod(LocalDate startDate,
      LocalDate endDate) {
    return CctAssumedFullTimePeriodDto.builder()
        .startDate(startDate)
        .endDate(endDate)
        .months(completedMonths(startDate, endDate))
        .build();
  }

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

  private static double calendarMonths(CctChangeDto change) {
    return completedMonths(change.startDate(), change.endDate());
  }

  private static double wteMonths(CctChangeDto change) {
    return calendarMonths(change) * wteFraction(change);
  }

  private static double wteFraction(CctChangeDto change) {
    if (!change.countsTowardsCompletion()) {
      return 0;
    }
    return change.type() == OOPT ? 1 : change.wte();
  }

  private static double completedMonths(LocalDate startDate, LocalDate endDate) {
    return inclusiveDays(startDate, endDate) / COMPLETED_PERIOD_DAYS_PER_MONTH;
  }

  private static long inclusiveDays(LocalDate startDate, LocalDate endDate) {
    return DAYS.between(startDate, endDate) + 1;
  }

  private static LocalDate dateAfterMonths(LocalDate startDate, double months) {
    return startDate.plusDays((long) Math.floor(months * DAYS_PER_MONTH + FLOOR_TOLERANCE_DAYS));
  }

  private static double roundToTwoDecimals(double months) {
    return Math.round(months * 100) / 100.0;
  }

  private record Projection(LocalDate startDate, double wte) {

  }

  private record TrainingPeriod(LocalDate startDate, LocalDate endDate, double wteMonths) {

  }
}
