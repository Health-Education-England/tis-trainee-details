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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.LTFT;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.OOPC;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.OOPR;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.OOPT;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.PARENTAL;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST1;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import uk.nhs.hee.trainee.details.dto.CctAssumedFullTimePeriodDto;
import uk.nhs.hee.trainee.details.dto.CctCalculateResponseDto;
import uk.nhs.hee.trainee.details.dto.CctCalculatedChangeDto;
import uk.nhs.hee.trainee.details.dto.CctChangeDto;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto;
import uk.nhs.hee.trainee.details.dto.CctProjectionDto;
import uk.nhs.hee.trainee.details.dto.CctResultDto;
import uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType;

class CctEngineTest {

  private static final String LONG_PROGRAMME_START = "2018-01-01";
  private static final String PAEDIATRICS_START = "2023-08-02";

  private CctEngine engine;

  @BeforeEach
  void setUp() {
    engine = new CctEngine();
  }

  @Test
  void shouldProjectFullTimeFromDayAfterLastChangeEnds() {
    CctResultDto result = engine.calculate(programme(LONG_PROGRAMME_START, 120),
        List.of(change(LTFT, "2018-01-01", "2022-12-31", 0.5, true))).result();

    assertThat("Unexpected total WTE months completed.", result.totalWteMonthsCompleted(),
        closeTo(30.0164383562, 1e-9));
    assertThat("Unexpected projection start date.", result.projection().startDate(),
        is(date("2023-01-01")));
    assertThat("Unexpected CCT date.", result.cctDate(), is(date("2030-06-28")));
  }

  @Test
  void shouldMatchPocWhenUncountedAbsenceThenOpenLtftChange() {
    CctResultDto result = engine.calculate(programme(LONG_PROGRAMME_START, 120), List.of(
        change(OOPC, "2020-01-01", "2022-12-31", null, false),
        change(LTFT, "2024-01-01", null, 0.5, true))).result();

    assertThat("Unexpected total WTE months completed.", result.totalWteMonthsCompleted(),
        closeTo(36, 1e-9));
    assertThat("Unexpected CCT date.", result.cctDate(), is(date("2037-12-25")));
  }

  @Test
  void shouldCreditOoptAtFullTimeOnlyWhenCounted() {
    CctCalculateResponseDto counted = engine.calculate(programme(LONG_PROGRAMME_START, 120),
        List.of(change(OOPT, "2020-01-01", "2020-06-30", null, true)));
    CctCalculateResponseDto uncounted = engine.calculate(programme(LONG_PROGRAMME_START, 120),
        List.of(change(OOPT, "2020-01-01", "2020-06-30", null, false)));

    assertThat("Unexpected counted WTE months.", counted.changes().get(0).wteMonths(),
        closeTo(6, 0.05));
    assertThat("Unexpected uncounted WTE months.", uncounted.changes().get(0).wteMonths(),
        is(0.0));
  }

  @Test
  void shouldCreditCountedOoprAtApprovedWte() {
    CctCalculateResponseDto response = engine.calculate(programme(LONG_PROGRAMME_START, 120),
        List.of(change(OOPR, "2020-01-01", "2020-06-30", 0.8, true)));

    assertThat("Unexpected WTE months.", response.changes().get(0).wteMonths(),
        closeTo(4.8, 0.05));
  }

  @Test
  void shouldLeaveWholeAdjustedLengthRemainingWhenNoChanges() {
    CctResultDto result = engine.calculate(programme(LONG_PROGRAMME_START, 120, 6, 3),
        List.of()).result();

    assertThat("Unexpected total WTE months completed.", result.totalWteMonthsCompleted(),
        is(0.0));
    assertThat("Unexpected months remaining.", result.monthsRemaining(), is(123.0));
    assertThat("Unexpected assumed periods.", result.assumedFullTimePeriods(), empty());
    assertThat("Unexpected projection WTE months.", result.projection().wteMonths(), is(123.0));
  }

  @Test
  void shouldBreakResultDownIntoPeriods() {
    CctCalculateResponseDto response = engine.calculate(programme(PAEDIATRICS_START, 96),
        List.of(change(PARENTAL, "2024-02-01", "2024-08-01", null, false)));
    List<CctAssumedFullTimePeriodDto> assumedPeriods = response.result().assumedFullTimePeriods();

    assertThat("Unexpected change WTE months.", response.changes().get(0).wteMonths(), is(0.0));
    assertThat("Unexpected assumed period count.", assumedPeriods, hasSize(1));
    assertThat("Unexpected assumed start date.", assumedPeriods.get(0).startDate(),
        is(date("2023-08-02")));
    assertThat("Unexpected assumed end date.", assumedPeriods.get(0).endDate(),
        is(date("2024-01-31")));
  }

  @Test
  void shouldRoundMonthsRemainingBeforeProjecting() {
    CctResultDto result = engine.calculate(programme(PAEDIATRICS_START, 84),
        List.of(change(LTFT, "2023-11-21", "2025-11-09", 0.5, true))).result();

    assertThat("Unexpected months remaining.", result.monthsRemaining(), is(68.52));
    assertThat("Unexpected projection WTE months.", result.projection().wteMonths(), is(68.52));
    assertThat("Unexpected CCT date.", result.cctDate(), is(date("2031-07-25")));
  }

  @Test
  void shouldProjectRoundedMonthsRemainingAtOpenLtftWte() {
    CctResultDto result = engine.calculate(programme(PAEDIATRICS_START, 84),
        List.of(change(LTFT, "2024-02-01", null, 0.6, true))).result();

    assertThat("Unexpected months remaining.", result.monthsRemaining(), is(77.98));
    assertThat("Unexpected projection calendar months.", result.projection().calendarMonths(),
        closeTo(77.98 / 0.6, 1e-9));
    assertThat("Unexpected CCT date.", result.cctDate(), is(date("2034-11-25")));
  }

  @Test
  void shouldFloorWholeNumberOfDaysWhenFloatingPointNoiseLeavesItJustShort() {
    CctResultDto result = engine.calculate(programme(PAEDIATRICS_START, 84),
        List.of(change(LTFT, PAEDIATRICS_START, null, 0.56, true))).result();

    assertThat("Unexpected months remaining.", result.monthsRemaining(), is(84.0));
    assertThat("Unexpected CCT date.", result.cctDate(), is(date("2036-01-26")));
  }

  @Test
  void shouldGiveDayTrainingCompletesWhenChangeRunsPastIt() {
    CctResultDto result = engine.calculate(programme(PAEDIATRICS_START, 84),
        List.of(change(LTFT, "2030-01-01", "2032-12-31", 0.5, true))).result();

    assertThat("Unexpected months remaining.", result.monthsRemaining(), is(-11.08));
    assertThat("Unexpected CCT date.", result.cctDate(), is(date("2031-02-26")));
    assertThat("Unexpected projection start date.", result.projection().startDate(),
        is(date("2033-01-01")));
    assertThat("Unexpected projection WTE months.", result.projection().wteMonths(),
        is(-11.08));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      2031-02-25 |  0.02 | 2031-02-26
      2031-02-26 |  0    | 2031-02-27
      2031-02-27 | -0.02 | 2031-02-28
      """)
  void shouldGiveDayTrainingCompletesWhenChangeEndsAroundIt(String endDate,
      double monthsRemaining, String projectionStartDate) {
    CctResultDto result = engine.calculate(programme(PAEDIATRICS_START, 84),
        List.of(change(LTFT, "2030-01-01", endDate, 0.5, true))).result();

    assertThat("Unexpected months remaining.", result.monthsRemaining(), is(monthsRemaining));
    assertThat("Unexpected CCT date.", result.cctDate(), is(date("2031-02-26")));
    assertThat("Unexpected projection start date.", result.projection().startDate(),
        is(date(projectionStartDate)));
    assertThat("Unexpected projection WTE months.", result.projection().wteMonths(),
        is(monthsRemaining));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      2026-01-30 | -0.02
      2026-01-31 | -0.05
      """)
  void shouldAllowSmallNegativeMonthsRemainingWhenChangeCompletesTraining(String endDate,
      double monthsRemaining) {
    CctResultDto result = engine.calculate(programme(PAEDIATRICS_START, 30),
        List.of(change(OOPR, PAEDIATRICS_START, endDate, 1.0, true))).result();

    assertThat("Unexpected months remaining.", result.monthsRemaining(), is(monthsRemaining));
    assertThat("Unexpected CCT date.", result.cctDate(), is(date("2026-01-30")));
  }

  @ParameterizedTest
  @MethodSource("listCompletingChanges")
  void shouldGiveDayTrainingCompletesWhenChangesComplete(List<CctChangeDto> changes,
      String cctDate) {
    CctResultDto result = engine.calculate(programme(PAEDIATRICS_START, 84), changes).result();

    assertThat("Unexpected CCT date.", result.cctDate(), is(date(cctDate)));
  }

  static Stream<Arguments> listCompletingChanges() {
    return Stream.of(
        Arguments.of(List.of(change(LTFT, "2030-01-01", "2032-12-31", 0.5, true)), "2031-02-26"),
        Arguments.of(List.of(change(OOPR, "2029-01-01", "2031-06-30", 1.0, true)), "2030-07-30"),
        Arguments.of(List.of(
            change(OOPR, "2029-01-01", "2031-06-30", 1.0, true),
            change(PARENTAL, "2031-07-01", "2032-03-31", null, false)), "2030-07-30"),
        Arguments.of(List.of(change(LTFT, "2031-01-01", null, 0.2, true)), "2030-07-30"),
        Arguments.of(List.of(change(LTFT, "2029-01-01", "2031-08-18", 0.6, true)), "2031-08-18")
    );
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      0 | 2030-07-29
      6 | 2031-01-28
      """)
  void shouldFloorProgrammeEndDates(int additionalMonths, String adjustedProgrammeEndDate) {
    CctResultDto result = engine.calculate(
        programme(PAEDIATRICS_START, 84, additionalMonths, 0), List.of()).result();

    assertThat("Unexpected original programme end date.", result.originalProgrammeEndDate(),
        is(date("2030-07-29")));
    assertThat("Unexpected adjusted programme end date.", result.adjustedProgrammeEndDate(),
        is(date(adjustedProgrammeEndDate)));
    assertThat("Unexpected CCT date.", result.cctDate(), is(date(adjustedProgrammeEndDate)));
  }

  @Test
  void shouldCalculateResultWhenNoChanges() {
    CctCalculateResponseDto response = engine.calculate(programme(PAEDIATRICS_START, 84),
        List.of());

    CctCalculateResponseDto expected = CctCalculateResponseDto.builder()
        .changes(List.of())
        .result(CctResultDto.builder()
            .cctDate(date("2030-07-29"))
            .originalProgrammeEndDate(date("2030-07-29"))
            .adjustedProgrammeEndDate(date("2030-07-29"))
            .totalWteMonthsCompleted(0)
            .monthsRemaining(84)
            .assumedFullTimePeriods(List.of())
            .projection(projection("2023-08-02", 1, 84, 84))
            .build())
        .build();
    assertThat("Unexpected response.", response, is(expected));
  }

  @Test
  void shouldCalculateResultWhenDatedLtftChange() {
    CctChangeDto change = change(LTFT, "2024-02-01", "2024-08-01", 0.6, true);
    CctCalculateResponseDto response = engine.calculate(programme(PAEDIATRICS_START, 84),
        List.of(change));

    CctCalculateResponseDto expected = CctCalculateResponseDto.builder()
        .changes(List.of(
            calculatedChange(change, 6.016438356164383, 3.6098630136986296)))
        .result(CctResultDto.builder()
            .cctDate(date("2030-10-10"))
            .originalProgrammeEndDate(date("2030-07-29"))
            .adjustedProgrammeEndDate(date("2030-07-29"))
            .totalWteMonthsCompleted(9.626301369863013)
            .monthsRemaining(74.37)
            .assumedFullTimePeriods(List.of(
                assumedPeriod("2023-08-02", "2024-01-31", 6.016438356164383)))
            .projection(projection("2024-08-02", 1, 74.37, 74.37))
            .build())
        .build();
    assertThat("Unexpected response.", response, is(expected));
  }

  @Test
  void shouldCalculateResultWhenOpenLtftChange() {
    CctChangeDto change = change(LTFT, "2025-02-01", null, 0.6, true);
    CctCalculateResponseDto response = engine.calculate(programme(PAEDIATRICS_START, 84),
        List.of(change));

    CctCalculateResponseDto expected = CctCalculateResponseDto.builder()
        .changes(List.of(
            calculatedChange(change, null, null)))
        .result(CctResultDto.builder()
            .cctDate(date("2034-03-27"))
            .originalProgrammeEndDate(date("2030-07-29"))
            .adjustedProgrammeEndDate(date("2030-07-29"))
            .totalWteMonthsCompleted(18.04931506849315)
            .monthsRemaining(65.95)
            .assumedFullTimePeriods(List.of(
                assumedPeriod("2023-08-02", "2025-01-31", 18.04931506849315)))
            .projection(projection("2025-02-01", 0.6, 109.91666666666667, 65.95))
            .build())
        .build();
    assertThat("Unexpected response.", response, is(expected));
  }

  @Test
  void shouldProjectFromDayAfterLatestEndWhenOnlyDatedChanges() {
    CctCalculateResponseDto response = engine.calculate(programme(PAEDIATRICS_START, 84),
        List.of(
            change(LTFT, "2025-01-01", "2025-06-30", 0.5, true),
            change(OOPC, "2024-01-01", "2024-03-31", null, false)));
    CctResultDto result = response.result();

    assertThat("Unexpected change count.", response.changes(), hasSize(2));
    assertThat("Unexpected first change start date.",
        response.changes().get(0).change().startDate(), is(date("2025-01-01")));
    assertThat("Unexpected assumed period count.", result.assumedFullTimePeriods(), hasSize(2));
    assertThat("Unexpected projection start date.", result.projection().startDate(),
        is(date("2025-07-01")));
    assertThat("Unexpected months remaining.", result.monthsRemaining(), is(66.99));
    assertThat("Unexpected CCT date.", result.cctDate(), is(date("2031-01-27")));
  }

  @Test
  void shouldProjectAtOpenLtftWteWhenItFollowsDatedChange() {
    CctResultDto result = engine.calculate(programme(PAEDIATRICS_START, 84), List.of(
        change(LTFT, "2024-02-01", "2024-08-01", 0.5, true),
        change(LTFT, "2024-08-02", null, 0.8, true))).result();

    assertThat("Unexpected assumed period count.", result.assumedFullTimePeriods(), hasSize(1));
    assertThat("Unexpected projection start date.", result.projection().startDate(),
        is(date("2024-08-02")));
    assertThat("Unexpected projection calendar months.", result.projection().calendarMonths(),
        is(93.725));
    assertThat("Unexpected months remaining.", result.monthsRemaining(), is(74.98));
    assertThat("Unexpected CCT date.", result.cctDate(), is(date("2032-05-21")));
  }

  @Test
  void shouldAssumeFullTimeBeforeOpenLtftChangeWhenItFollowsGap() {
    CctResultDto result = engine.calculate(programme(PAEDIATRICS_START, 84), List.of(
        change(LTFT, "2024-02-01", "2024-08-01", 0.5, true),
        change(LTFT, "2025-02-01", null, 0.8, true))).result();
    List<CctAssumedFullTimePeriodDto> assumedPeriods = result.assumedFullTimePeriods();

    assertThat("Unexpected assumed period count.", assumedPeriods, hasSize(2));
    assertThat("Unexpected assumed start date.", assumedPeriods.get(1).startDate(),
        is(date("2024-08-02")));
    assertThat("Unexpected assumed end date.", assumedPeriods.get(1).endDate(),
        is(date("2025-01-31")));
    assertThat("Unexpected projection start date.", result.projection().startDate(),
        is(date("2025-02-01")));
    assertThat("Unexpected months remaining.", result.monthsRemaining(), is(68.96));
    assertThat("Unexpected CCT date.", result.cctDate(), is(date("2032-04-05")));
  }

  @Test
  void shouldCalculateWhenChangeStartsBeforeProgramme() {
    CctResultDto result = engine.calculate(programme("2024-03-01", 84),
        List.of(change(PARENTAL, "2024-02-01", "2024-08-01", null, false))).result();

    assertThat("Unexpected months remaining.", result.monthsRemaining(), is(84.0));
    assertThat("Unexpected CCT date.", result.cctDate(), is(date("2031-07-30")));
  }

  @Test
  void shouldCalculateWhenChangeEndsBeforeProgramme() {
    CctResultDto result = engine.calculate(programme("2024-09-01", 84),
        List.of(change(LTFT, "2024-02-01", "2024-08-01", 0.5, true))).result();

    assertThat("Unexpected assumed periods.", result.assumedFullTimePeriods(), empty());
    assertThat("Unexpected months remaining.", result.monthsRemaining(), is(80.99));
    assertThat("Unexpected CCT date.", result.cctDate(), is(date("2031-04-30")));
  }

  @Test
  void shouldGiveAdjustedProgrammeEndDateWhenNoChanges() {
    for (int length = 1; length <= 120; length++) {
      for (int additional = 0; additional <= 24; additional++) {
        for (int accelerated = 0; accelerated <= 12 && accelerated < length + additional;
            accelerated++) {
          CctResultDto result = engine.calculate(
              programme(PAEDIATRICS_START, length, additional, accelerated), List.of()).result();

          String months = length + " + " + additional + " - " + accelerated;
          assertThat("Unexpected CCT date for " + months + " months.", result.cctDate(),
              is(result.adjustedProgrammeEndDate()));
        }
      }
    }
  }

  @ParameterizedTest
  @MethodSource("listChangesWithinProgramme")
  void shouldCreditAdjustedLengthAcrossPeriods(List<CctChangeDto> changes) {
    CctCalculateResponseDto response = engine.calculate(programme(PAEDIATRICS_START, 84, 6, 3),
        changes);
    CctResultDto result = response.result();

    double changesWteMonths = response.changes().stream()
        .map(CctCalculatedChangeDto::wteMonths)
        .filter(Objects::nonNull)
        .mapToDouble(Double::doubleValue)
        .sum();
    double assumedWteMonths = result.assumedFullTimePeriods().stream()
        .mapToDouble(CctAssumedFullTimePeriodDto::months)
        .sum();
    double wteMonths = changesWteMonths + assumedWteMonths + result.projection().wteMonths();
    assertThat("Unexpected total WTE months.", wteMonths, closeTo(87, 0.005));
  }

  static Stream<List<CctChangeDto>> listChangesWithinProgramme() {
    return Stream.of(
        List.of(),
        List.of(change(LTFT, "2024-02-01", "2024-08-01", 0.6, true)),
        List.of(change(LTFT, "2025-02-01", null, 0.6, true)),
        List.of(
            change(OOPT, "2024-01-01", "2024-12-31", null, true),
            change(OOPR, "2025-03-01", "2026-02-28", 0.5, true),
            change(LTFT, "2026-06-01", null, 0.8, true)),
        List.of(
            change(PARENTAL, "2026-01-01", "2026-09-30", null, false),
            change(OOPR, "2024-01-01", "2024-06-30", null, false)),
        List.of(change(LTFT, "2030-01-01", "2032-12-31", 0.5, true))
    );
  }

  private static CctProgrammeDetailsDto programme(String startDate, int lengthMonths) {
    return programme(startDate, lengthMonths, 0, 0);
  }

  private static CctProgrammeDetailsDto programme(String startDate, int lengthMonths,
      int additionalMonths, int acceleratedMonths) {
    return CctProgrammeDetailsDto.builder()
        .specialty("Paediatrics")
        .startDate(date(startDate))
        .lengthMonths(lengthMonths)
        .startGrade(ST1)
        .additionalMonths(additionalMonths)
        .acceleratedMonths(acceleratedMonths)
        .build();
  }

  private static CctChangeDto change(CctChangeType type, String startDate, String endDate,
      Double wte, boolean countsTowardsCompletion) {
    return CctChangeDto.builder()
        .type(type)
        .startDate(date(startDate))
        .endDate(date(endDate))
        .wte(wte)
        .countsTowardsCompletion(countsTowardsCompletion)
        .build();
  }

  private static CctCalculatedChangeDto calculatedChange(CctChangeDto change,
      Double calendarMonths, Double wteMonths) {
    return CctCalculatedChangeDto.builder()
        .change(change)
        .calendarMonths(calendarMonths)
        .wteMonths(wteMonths)
        .build();
  }

  private static CctAssumedFullTimePeriodDto assumedPeriod(String startDate, String endDate,
      double months) {
    return CctAssumedFullTimePeriodDto.builder()
        .startDate(date(startDate))
        .endDate(date(endDate))
        .months(months)
        .build();
  }

  private static CctProjectionDto projection(String startDate, double wte, double calendarMonths,
      double wteMonths) {
    return CctProjectionDto.builder()
        .startDate(date(startDate))
        .wte(wte)
        .calendarMonths(calendarMonths)
        .wteMonths(wteMonths)
        .build();
  }

  private static LocalDate date(String date) {
    return date == null ? null : LocalDate.parse(date);
  }
}
