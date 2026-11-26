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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.ACCRUED_LEAVE;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.LTFT;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.OOPC;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.OOPE;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.OOPP;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.OOPR;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.OOPT;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.PARENTAL;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.PHASED;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.SHIELDING;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.SICKNESS;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST1;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST2;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST3;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import uk.nhs.hee.trainee.details.dto.CctChangeDto.CctChangeDtoBuilder;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto.CctProgrammeDetailsDtoBuilder;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto.CctTrainingTimeDto;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto.CctTrainingTimeDto.CctTrainingTimeDtoBuilder;
import uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType;
import uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade;

class CctCalculateRequestDtoTest {

  private static final LocalDate EARLIEST_DATE = LocalDate.of(1970, 1, 1);
  private static final LocalDate LATEST_DATE =
      LocalDate.now(ZoneId.of("Europe/London")).plusYears(50);
  private static final String DATE_LIMITS_ERROR =
      "Date must be between 01/01/1970 and 50 years from today.";

  private static ValidatorFactory validatorFactory;
  private static SpringValidatorAdapter validator;

  @BeforeAll
  static void setUp() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = new SpringValidatorAdapter(validatorFactory.getValidator());
  }

  @AfterAll
  static void tearDown() {
    validatorFactory.close();
  }

  @Test
  void shouldAcceptRequestWithNoChanges() {
    List<BodyValidationError> errors = validate(programme(), List.of());

    assertThat("Unexpected errors.", errors, hasSize(0));
  }

  @Test
  void shouldAcceptChangeThatRunsPastDayTrainingCompletes() {
    List<BodyValidationError> errors = validate(programme(),
        List.of(ltft().startDate(date("2030-01-01")).endDate(date("2032-12-31")).build()));

    assertThat("Unexpected errors.", errors, hasSize(0));
  }

  @Test
  void shouldAcceptStartGradeWithoutNote() {
    List<BodyValidationError> errors = validate(programme().startGrade(ST2), List.of());

    assertThat("Unexpected errors.", errors, hasSize(0));
  }

  @Test
  void shouldAcceptChangeThatStartsBeforeProgramme() {
    List<BodyValidationError> errors = validate(programme(),
        List.of(ltft().startDate(date("2023-08-01")).build()));

    assertThat("Unexpected errors.", errors, hasSize(0));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("validChanges")
  void shouldAcceptValidChange(String description, CctChangeDto change) {
    List<BodyValidationError> errors = validate(programme(), List.of(change));

    assertThat("Unexpected errors.", errors, hasSize(0));
  }

  static Stream<Arguments> validChanges() {
    return Stream.of(
        Arguments.of("dated LTFT",
            datedChange(LTFT).wte(0.07).countsTowardsCompletion(true).build()),
        Arguments.of("open-ended LTFT",
            ltft().startDate(date("2025-01-01")).endDate(null).wte(0.2).notes("").build()),
        Arguments.of("counted OOPR",
            datedChange(OOPR).wte(0.07).countsTowardsCompletion(true).build()),
        Arguments.of("uncounted OOPR", datedChange(OOPR).build()),
        Arguments.of("counted OOPT",
            datedChange(OOPT).countsTowardsCompletion(true).build()),
        Arguments.of("uncounted OOPT", datedChange(OOPT).build()),
        Arguments.of("OOPC", datedChange(OOPC).build()),
        Arguments.of("OOPE", datedChange(OOPE).build()),
        Arguments.of("OOPP", datedChange(OOPP).build()),
        Arguments.of("parental leave", datedChange(PARENTAL).build()),
        Arguments.of("phased return", datedChange(PHASED).build()),
        Arguments.of("shielding", datedChange(SHIELDING).build()),
        Arguments.of("sickness", datedChange(SICKNESS).build()),
        Arguments.of("accrued leave", datedChange(ACCRUED_LEAVE).build()));
  }

  private static CctChangeDtoBuilder datedChange(CctChangeType type) {
    return CctChangeDto.builder()
        .type(type)
        .startDate(date("2024-01-01"))
        .endDate(date("2024-06-30"))
        .notes("a note");
  }

  @Test
  void shouldAcceptLtftWteOfWholePercent() {
    List<BodyValidationError> errors = validate(programme(), List.of(ltft().wte(0.12).build()));

    assertThat("Unexpected errors.", errors, hasSize(0));
  }

  @Test
  void shouldAcceptChangeNotesOfMaximumLengthWithSurroundingWhitespace() {
    List<BodyValidationError> errors = validate(programme(),
        List.of(parental().notes("  " + "x".repeat(1000) + "\n").build()));

    assertThat("Unexpected errors.", errors, hasSize(0));
  }

  @Test
  void shouldAcceptMaximumNumberOfChanges() {
    List<BodyValidationError> errors = validate(programme(), oneDayChanges(50));

    assertThat("Unexpected errors.", errors, hasSize(0));
  }

  @Test
  void shouldAcceptDatesOnTheLimits() {
    List<BodyValidationError> errors = validate(programme().startDate(EARLIEST_DATE),
        List.of(ltft().startDate(EARLIEST_DATE).build(),
            ltft().startDate(LATEST_DATE).endDate(LATEST_DATE).build()));

    assertThat("Unexpected errors.", errors, hasSize(0));
  }

  @Test
  void shouldAcceptProgrammeNotesOfMaximumLength() {
    String notes = "x".repeat(4096);
    List<BodyValidationError> errors = validate(programme()
        .lengthOverrideNotes(notes)
        .startGradeOverrideNotes(notes)
        .eighteenMonthFinalGradeNotes(notes)
        .skippedGradeNotes(notes), List.of());

    assertThat("Unexpected errors.", errors, hasSize(0));
  }

  @Test
  void shouldAcceptTrainingTimeOnTheLimits() {
    String notes = "  " + "x".repeat(1000) + "\n";
    List<BodyValidationError> errors = validate(programme()
        .lengthMonths(12)
        .additionalTrainingTime(List.of(trainingTime(ST1, 24).notes(notes).build()))
        .acceleratedTrainingTime(List.of(
            trainingTime(ST1, 1).notes(notes).build(),
            trainingTime(ST2, 11).build())), List.of());

    assertThat("Unexpected errors.", errors, hasSize(0));
  }

  @Test
  void shouldAcceptMaximumNumberOfTrainingTimeEntries() {
    List<CctTrainingTimeDto> entries = IntStream.range(0, 20)
        .mapToObj(index -> trainingTime(ST1, 1).build())
        .toList();
    List<BodyValidationError> errors = validate(programme().additionalTrainingTime(entries),
        List.of());

    assertThat("Unexpected errors.", errors, hasSize(0));
  }

  @Test
  void shouldAcceptEighteenMonthFinalGradeWithoutNotes() {
    List<BodyValidationError> errors = validate(programme().eighteenMonthFinalGrade(ST3),
        List.of());

    assertThat("Unexpected errors.", errors, hasSize(0));
  }

  @Test
  void shouldRejectProgrammeNotesOverMaximumLength() {
    String notes = "x".repeat(4097);
    List<BodyValidationError> errors = validate(programme()
        .lengthOverrideNotes(notes)
        .startGradeOverrideNotes(notes)
        .eighteenMonthFinalGradeNotes(notes)
        .skippedGradeNotes(notes), List.of());

    String detail = "Notes must be 4096 characters or less";
    assertThat("Unexpected errors.", errors, is(List.of(
        error("#/programmeDetails/eighteenMonthFinalGradeNotes", detail),
        error("#/programmeDetails/lengthOverrideNotes", detail),
        error("#/programmeDetails/skippedGradeNotes", detail),
        error("#/programmeDetails/startGradeOverrideNotes", detail))));
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("invalidProgrammeDetails")
  void shouldRejectInvalidProgrammeDetails(String description,
      CctProgrammeDetailsDtoBuilder programme, String field, String detail) {
    List<BodyValidationError> errors = validate(programme, List.of());

    assertThat("Unexpected errors.", errors,
        is(List.of(error("#/programmeDetails/" + field, detail))));
  }

  static Stream<Arguments> invalidProgrammeDetails() {
    return Stream.of(
        Arguments.of("missing specialty", programme().specialty(""),
            "specialty", "Please choose a specialty."),
        Arguments.of("missing start date", programme().startDate(null),
            "startDate", "Please enter a programme start date."),
        Arguments.of("start date before the earliest date",
            programme().startDate(EARLIEST_DATE.minusDays(1)), "startDate", DATE_LIMITS_ERROR),
        Arguments.of("start date after the latest date",
            programme().startDate(LATEST_DATE.plusDays(1)), "startDate", DATE_LIMITS_ERROR),
        Arguments.of("missing start grade", programme().startGrade(null),
            "startGrade", "Please choose a start grade."),
        Arguments.of("missing programme length", programme().lengthMonths(null),
            "lengthMonths", "Programme length must be a whole number of months between 1 and 120."),
        Arguments.of("programme length over 120 months", programme().lengthMonths(121),
            "lengthMonths", "Programme length must be a whole number of months between 1 and 120."),
        Arguments.of("missing training time entry",
            programme().additionalTrainingTime(Arrays.asList((CctTrainingTimeDto) null)),
            "additionalTrainingTime/0", "must not be null"),
        Arguments.of("training time with no grade",
            programme().additionalTrainingTime(List.of(trainingTime(null, 3).build())),
            "additionalTrainingTime/0/grade", "Please choose a grade."),
        Arguments.of("training time with no months",
            programme().additionalTrainingTime(List.of(trainingTime(ST1, null).build())),
            "additionalTrainingTime/0/months",
            "Training time must be a whole number of months between 1 and 24."),
        Arguments.of("training time of zero months",
            programme().additionalTrainingTime(List.of(trainingTime(ST1, 0).build())),
            "additionalTrainingTime/0/months",
            "Training time must be a whole number of months between 1 and 24."),
        Arguments.of("training time over 24 months",
            programme().additionalTrainingTime(List.of(trainingTime(ST1, 25).build())),
            "additionalTrainingTime/0/months",
            "Training time must be a whole number of months between 1 and 24."),
        Arguments.of("accelerated time with no months",
            programme().acceleratedTrainingTime(List.of(trainingTime(ST1, null).build())),
            "acceleratedTrainingTime/0/months",
            "Training time must be a whole number of months between 1 and 12."),
        Arguments.of("accelerated time over 12 months",
            programme().acceleratedTrainingTime(List.of(trainingTime(ST1, 13).build())),
            "acceleratedTrainingTime/0/months",
            "Training time must be a whole number of months between 1 and 12."),
        Arguments.of("accelerated time over 24 months",
            programme().acceleratedTrainingTime(List.of(trainingTime(ST1, 30).build())),
            "acceleratedTrainingTime/0/months",
            "Training time must be a whole number of months between 1 and 12."),
        Arguments.of("training time without a note",
            programme().acceleratedTrainingTime(
                List.of(trainingTime(ST2, 3).build(), trainingTime(ST1, 3).notes("  ").build())),
            "acceleratedTrainingTime/1/notes", "Please enter a reason for the training time."),
        Arguments.of("training time notes over 1000 characters",
            programme().additionalTrainingTime(
                List.of(trainingTime(ST1, 3).notes("x".repeat(1001)).build())),
            "additionalTrainingTime/0/notes", "Notes must be 1000 characters or less"),
        Arguments.of("additional time over 24 months in total",
            programme().additionalTrainingTime(
                List.of(trainingTime(ST1, 12).build(), trainingTime(ST2, 13).build())),
            "additionalTrainingTime",
            "Total additional training time cannot be more than 24 months."),
        Arguments.of("accelerated time over 12 months in total",
            programme().acceleratedTrainingTime(
                List.of(trainingTime(ST1, 6).build(), trainingTime(ST2, 7).build())),
            "acceleratedTrainingTime",
            "Total accelerated training time cannot be more than 12 months."),
        Arguments.of("adjusted length of zero with accelerated time over the limit",
            programme().lengthMonths(12)
                .additionalTrainingTime(List.of(trainingTime(ST1, 1).build()))
                .acceleratedTrainingTime(
                    List.of(trainingTime(ST1, 12).build(), trainingTime(ST1, 1).build())),
            "acceleratedTrainingTime",
            "Total accelerated training time cannot be more than 12 months."),
        Arguments.of("adjusted length of zero",
            programme().lengthMonths(12)
                .acceleratedTrainingTime(List.of(trainingTime(ST1, 12).build())),
            "acceleratedTrainingTime", "Adjusted training duration must be greater than zero."),
        Arguments.of("more than 20 additional training time entries",
            programme().additionalTrainingTime(Stream.generate(() -> trainingTime(ST1, 1).build())
                .limit(21).toList()),
            "additionalTrainingTime",
            "You cannot add more than 20 additional training time entries."),
        Arguments.of("skipped grade without a note", programme().skippedGrade(ST2),
            "skippedGradeNotes", "Please enter a reason for skipping a grade year.")
    );
  }

  @Test
  void shouldRejectMissingTrainingTimeLists() {
    List<BodyValidationError> errors = validate(
        programme().additionalTrainingTime(null).acceleratedTrainingTime(null), List.of());

    assertThat("Unexpected errors.", errors, is(List.of(
        error("#/programmeDetails/acceleratedTrainingTime", "must not be null"),
        error("#/programmeDetails/additionalTrainingTime", "must not be null"))));
  }

  @Test
  void shouldRejectMoreThanMaximumAcceleratedTrainingTimeEntries() {
    List<BodyValidationError> errors = validate(programme().acceleratedTrainingTime(
        Stream.generate(() -> trainingTime(ST1, 1).build()).limit(21).toList()), List.of());

    assertThat("Unexpected errors.", errors, is(List.of(
        error("#/programmeDetails/acceleratedTrainingTime",
            "Total accelerated training time cannot be more than 12 months."),
        error("#/programmeDetails/acceleratedTrainingTime",
            "You cannot add more than 20 accelerated training time entries."))));
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("invalidChanges")
  void shouldRejectInvalidChange(String description, CctChangeDto change, String field,
      String detail) {
    List<BodyValidationError> errors = validate(programme(), List.of(change));

    assertThat("Unexpected errors.", errors, is(List.of(error("#/changes/0/" + field, detail))));
  }

  static Stream<Arguments> invalidChanges() {
    return Stream.of(
        Arguments.of("start date before the earliest date",
            ltft().startDate(EARLIEST_DATE.minusDays(1)).build(), "startDate", DATE_LIMITS_ERROR),
        Arguments.of("open LTFT start date after the latest date",
            ltft().startDate(LATEST_DATE.plusDays(1)).endDate(null).build(),
            "startDate", DATE_LIMITS_ERROR),
        Arguments.of("end date after the latest date",
            ltft().endDate(LATEST_DATE.plusDays(1)).build(), "endDate", DATE_LIMITS_ERROR),
        Arguments.of("missing type", ltft().type(null).wte(1.0).build(),
            "type", "Please choose a change type."),
        Arguments.of("missing start date", ltft().startDate(null).build(),
            "startDate", "Please enter a start date."),
        Arguments.of("end date before start date", ltft().endDate(date("2023-12-31")).build(),
            "startDate", "Start date cannot be after end date."),
        Arguments.of("missing end date on a non-LTFT change", parental().endDate(null).build(),
            "endDate", "Please enter an end date."),
        Arguments.of("LTFT WTE of 100%", ltft().wte(1.0).build(),
            "wte", "LTFT WTE must be a whole number between 1 and 99."),
        Arguments.of("LTFT WTE that isn't a whole percent", ltft().wte(0.505).build(),
            "wte", "LTFT WTE must be a whole number between 1 and 99."),
        Arguments.of("fractional OOPR credit", ltft().type(OOPR).wte(0.125).build(),
            "wte", "Approved OOPR credit must be a whole number between 1 and 100."),
        Arguments.of("LTFT that doesn't count", ltft().countsTowardsCompletion(false).build(),
            "countsTowardsCompletion", "LTFT must count towards completion."),
        Arguments.of("counted OOPR without credit", ltft().type(OOPR).wte(null).build(),
            "wte", "Approved OOPR credit must be a whole number between 1 and 100."),
        Arguments.of("uncounted OOPR with credit",
            ltft().type(OOPR).countsTowardsCompletion(false).build(),
            "wte", "Only an OOPR counted as training can have a credit."),
        Arguments.of("OOPT with WTE", ltft().type(OOPT).build(),
            "wte", "OOPT cannot have a WTE."),
        Arguments.of("counted OOPT over 12 months",
            ltft().type(OOPT).wte(null).endDate(date("2025-01-01")).build(),
            "endDate", "OOPT counted as training cannot be more than 12 months."),
        Arguments.of("parental leave that counts", parental().countsTowardsCompletion(true).build(),
            "countsTowardsCompletion", "PARENTAL cannot count towards completion."),
        Arguments.of("notes over 1000 characters", parental().notes("x".repeat(1001)).build(),
            "notes", "Notes must be 1000 characters or less")
    );
  }

  @ParameterizedTest
  @MethodSource("changesWithBothDatesOutsideLimits")
  void shouldReportBothOutOfRangeDates(CctChangeDto change) {
    List<BodyValidationError> errors = validate(programme(), List.of(change));

    assertThat("Unexpected errors.", errors, is(List.of(
        error("#/changes/0/endDate", DATE_LIMITS_ERROR),
        error("#/changes/0/startDate", DATE_LIMITS_ERROR))));
  }

  static Stream<CctChangeDto> changesWithBothDatesOutsideLimits() {
    return Stream.of(
        ltft().startDate(EARLIEST_DATE.minusDays(2)).endDate(EARLIEST_DATE.minusDays(1)).build(),
        CctChangeDto.builder().type(OOPT).startDate(LocalDate.MAX)
            .endDate(LocalDate.MAX).countsTowardsCompletion(true).build()
    );
  }

  @Test
  void shouldRejectOverlappingChanges() {
    List<BodyValidationError> errors = validate(programme(),
        List.of(ltft().build(), parental().startDate(date("2024-12-31")).build()));

    assertThat("Unexpected errors.", errors, is(List.of(error("#/changes/1/startDate",
        "This change overlaps an existing change (01/01/2024 – 31/12/2024)."))));
  }

  @Test
  void shouldRejectTwoOpenLtftChanges() {
    List<BodyValidationError> errors = validate(programme(), List.of(
        ltft().startDate(date("2030-01-01")).endDate(null).build(),
        ltft().startDate(date("2031-01-01")).endDate(null).build()));

    assertThat("Unexpected errors.", errors, is(List.of(error("#/changes/1/endDate",
        "Only one LTFT change can project the remaining training time."))));
  }

  @Test
  void shouldRejectOpenLtftStartingBeforeDatedChangeEnds() {
    List<BodyValidationError> errors = validate(programme(),
        List.of(ltft().startDate(date("2024-12-31")).endDate(null).build(), ltft().build()));

    assertThat("Unexpected errors.", errors, is(List.of(error("#/changes/0/startDate",
        "Projected change must begin after all other completed changes."))));
  }

  @Test
  void shouldRejectMissingChange() {
    List<BodyValidationError> errors = validate(programme(), Collections.singletonList(null));

    assertThat("Unexpected errors.", errors,
        is(List.of(error("#/changes/0", "must not be null"))));
  }

  @Test
  void shouldRejectTooManyChanges() {
    List<BodyValidationError> errors = validate(programme(), oneDayChanges(51));

    assertThat("Unexpected errors.", errors,
        is(List.of(error("#/changes", "size must be between 0 and 50"))));
  }

  @Test
  void shouldRejectMissingProgrammeDetailsAndChanges() {
    List<BodyValidationError> errors = validate(CctCalculateRequestDto.builder().build());

    assertThat("Unexpected errors.", errors, is(List.of(
        error("#/changes", "must not be null"),
        error("#/programmeDetails", "must not be null"))));
  }

  @Test
  void shouldReportEveryError() {
    List<BodyValidationError> errors = validate(programme().specialty("").lengthMonths(0),
        List.of(ltft().wte(1.0).endDate(date("2023-12-31")).build()));

    List<String> pointers = errors.stream().map(BodyValidationError::pointer).toList();
    assertThat("Unexpected error pointers.", pointers, is(List.of(
        "#/changes/0/startDate",
        "#/changes/0/wte",
        "#/programmeDetails/lengthMonths",
        "#/programmeDetails/specialty")));
  }

  private List<BodyValidationError> validate(CctProgrammeDetailsDtoBuilder programme,
      List<CctChangeDto> changes) {
    return validate(new CctCalculateRequestDto(programme.build(), changes));
  }

  private List<BodyValidationError> validate(CctCalculateRequestDto request) {
    BeanPropertyBindingResult result = new BeanPropertyBindingResult(request, "request");
    validator.validate(request, result);

    assertThat("Unexpected global errors.", result.getGlobalErrors(), hasSize(0));
    return BodyValidationError.fromFieldErrors(result.getFieldErrors());
  }

  private static CctProgrammeDetailsDtoBuilder programme() {
    return CctProgrammeDetailsDto.builder()
        .specialty("Paediatrics")
        .startDate(date("2023-08-02"))
        .lengthMonths(84)
        .lengthOverrideNotes("")
        .startGrade(ST1)
        .startGradeOverrideNotes("")
        .additionalTrainingTime(List.of())
        .acceleratedTrainingTime(List.of())
        .eighteenMonthFinalGradeNotes("")
        .skippedGradeNotes("");
  }

  private static CctTrainingTimeDtoBuilder trainingTime(CctTrainingGrade grade, Integer months) {
    return CctTrainingTimeDto.builder()
        .grade(grade)
        .months(months)
        .notes("why");
  }

  private static CctChangeDtoBuilder ltft() {
    return CctChangeDto.builder()
        .type(LTFT)
        .startDate(date("2024-01-01"))
        .endDate(date("2024-12-31"))
        .wte(0.5)
        .countsTowardsCompletion(true);
  }

  private static CctChangeDtoBuilder parental() {
    return CctChangeDto.builder()
        .type(PARENTAL)
        .startDate(date("2025-01-01"))
        .endDate(date("2025-06-30"))
        .countsTowardsCompletion(false);
  }

  private static List<CctChangeDto> oneDayChanges(int count) {
    LocalDate baseDate = LocalDate.parse("2024-01-01");
    return IntStream.range(0, count).mapToObj(day -> parental()
            .startDate(baseDate.plusDays(day))
            .endDate(baseDate.plusDays(day))
            .build())
        .toList();
  }

  private static LocalDate date(String date) {
    return LocalDate.parse(date);
  }

  private static BodyValidationError error(String pointer, String detail) {
    return new BodyValidationError(pointer, detail);
  }
}
