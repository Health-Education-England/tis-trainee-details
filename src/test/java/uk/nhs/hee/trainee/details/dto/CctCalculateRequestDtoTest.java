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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.LTFT;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.OOPR;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.OOPT;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.PARENTAL;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST1;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST2;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST3;

import jakarta.validation.Validation;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import uk.nhs.hee.trainee.details.dto.CctChangeDto.CctChangeDtoBuilder;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto.CctProgrammeDetailsDtoBuilder;
import uk.nhs.hee.trainee.details.dto.CctTrainingTimeDto.CctTrainingTimeDtoBuilder;
import uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType;
import uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade;

class CctCalculateRequestDtoTest {

  private static final LocalDate EARLIEST_DATE = LocalDate.of(1970, 1, 1);
  private static final LocalDate LATEST_DATE = LocalDate.now().plusYears(50);
  private static final String DATE_LIMITS_ERROR =
      "Date must be between 01/01/1970 and 50 years from today.";

  private SpringValidatorAdapter validator;

  @BeforeEach
  void setUp() {
    validator = new SpringValidatorAdapter(
        Validation.buildDefaultValidatorFactory().getValidator());
  }

  @Test
  void shouldAcceptRequestWithNoChanges() {
    List<BodyValidationError> errors = validate(programme(), List.of());

    assertThat("Unexpected errors.", errors, is(empty()));
  }

  @Test
  void shouldAcceptChangeThatRunsPastDayTrainingCompletes() {
    List<BodyValidationError> errors = validate(programme(),
        List.of(ltft().startDate(date("2030-01-01")).endDate(date("2032-12-31")).build()));

    assertThat("Unexpected errors.", errors, is(empty()));
  }

  @Test
  void shouldAcceptStartGradeWithoutNote() {
    List<BodyValidationError> errors = validate(programme().startGrade(ST2), List.of());

    assertThat("Unexpected errors.", errors, is(empty()));
  }

  @Test
  void shouldAcceptChangeThatStartsBeforeProgramme() {
    List<BodyValidationError> errors = validate(programme(),
        List.of(ltft().startDate(date("2023-08-01")).build()));

    assertThat("Unexpected errors.", errors, is(empty()));
  }

  @ParameterizedTest
  @MethodSource("listChangesBuiltByFrontend")
  void shouldAcceptChangesBuiltByFrontend(CctChangeDto change) {
    List<BodyValidationError> errors = validate(programme(), List.of(change));

    assertThat("Unexpected errors.", errors, is(empty()));
  }

  static Stream<CctChangeDto> listChangesBuiltByFrontend() {
    Stream<CctChangeDto> datedChanges = Arrays.stream(CctChangeType.values())
        .flatMap(type -> Stream.of(false, true).map(counts -> {
          boolean wteShown = type == LTFT || (type == OOPR && counts);
          return CctChangeDto.builder()
              .type(type)
              .startDate(date("2024-01-01"))
              .endDate(date("2024-06-30"))
              .wte(wteShown ? 0.07 : null)
              .countsTowardsCompletion((type == OOPT || type == OOPR) ? counts : type == LTFT)
              .notes("a note")
              .build();
        }));
    CctChangeDto openLtftChange = ltft()
        .startDate(date("2025-01-01"))
        .endDate(null)
        .wte(0.2)
        .notes("")
        .build();
    return Stream.concat(datedChanges, Stream.of(openLtftChange));
  }

  @Test
  void shouldAcceptLtftWteOfWholePercent() {
    List<BodyValidationError> errors = validate(programme(), List.of(ltft().wte(0.12).build()));

    assertThat("Unexpected errors.", errors, is(empty()));
  }

  @Test
  void shouldAcceptChangeNotesOfMaximumLengthWithSurroundingWhitespace() {
    List<BodyValidationError> errors = validate(programme(),
        List.of(parental().notes("  " + "x".repeat(1000) + "\n").build()));

    assertThat("Unexpected errors.", errors, is(empty()));
  }

  @Test
  void shouldAcceptMaximumNumberOfChanges() {
    List<BodyValidationError> errors = validate(programme(), oneDayChanges(50));

    assertThat("Unexpected errors.", errors, is(empty()));
  }

  @Test
  void shouldAcceptDatesOnTheLimits() {
    List<BodyValidationError> errors = validate(programme().startDate(EARLIEST_DATE),
        List.of(ltft().startDate(EARLIEST_DATE).build(),
            ltft().startDate(LATEST_DATE).endDate(LATEST_DATE).build()));

    assertThat("Unexpected errors.", errors, is(empty()));
  }

  @Test
  void shouldAcceptProgrammeNotesOfMaximumLength() {
    String notes = "x".repeat(4096);
    List<BodyValidationError> errors = validate(programme()
        .lengthOverrideNotes(notes)
        .startGradeOverrideNotes(notes)
        .eighteenMonthFinalGradeNotes(notes)
        .skippedGradeNotes(notes), List.of());

    assertThat("Unexpected errors.", errors, is(empty()));
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

    assertThat("Unexpected errors.", errors, is(empty()));
  }

  @Test
  void shouldAcceptMaximumNumberOfTrainingTimeEntries() {
    List<CctTrainingTimeDto> entries = IntStream.range(0, 20)
        .mapToObj(index -> trainingTime(ST1, 1).build())
        .toList();
    List<BodyValidationError> errors = validate(programme().additionalTrainingTime(entries),
        List.of());

    assertThat("Unexpected errors.", errors, is(empty()));
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
  @MethodSource("listInvalidRequests")
  void shouldRejectInvalidRequest(String description, CctProgrammeDetailsDtoBuilder programme,
      List<CctChangeDto> changes, List<BodyValidationError> expected) {
    List<BodyValidationError> errors = validate(programme, changes);

    assertThat("Unexpected errors.", errors, is(expected));
  }

  static Stream<Arguments> listInvalidRequests() {
    return Stream.of(
        Arguments.of("a missing specialty", programme().specialty(""), List.of(),
            List.of(error("#/programmeDetails/specialty", "Please choose a specialty."))),
        Arguments.of("a missing programme start date", programme().startDate(null), List.of(),
            List.of(error("#/programmeDetails/startDate",
                "Please enter a programme start date."))),
        Arguments.of("a programme start date before the earliest date",
            programme().startDate(EARLIEST_DATE.minusDays(1)), List.of(),
            List.of(error("#/programmeDetails/startDate", DATE_LIMITS_ERROR))),
        Arguments.of("a programme start date after the latest date",
            programme().startDate(LATEST_DATE.plusDays(1)), List.of(),
            List.of(error("#/programmeDetails/startDate", DATE_LIMITS_ERROR))),
        Arguments.of("a change start date before the earliest date", programme(),
            List.of(ltft().startDate(EARLIEST_DATE.minusDays(1)).build()),
            List.of(error("#/changes[0]/startDate", DATE_LIMITS_ERROR))),
        Arguments.of("an open LTFT start date after the latest date", programme(),
            List.of(ltft().startDate(LATEST_DATE.plusDays(1)).endDate(null).build()),
            List.of(error("#/changes[0]/startDate", DATE_LIMITS_ERROR))),
        Arguments.of("a change end date after the latest date", programme(),
            List.of(ltft().endDate(LATEST_DATE.plusDays(1)).build()),
            List.of(error("#/changes[0]/endDate", DATE_LIMITS_ERROR))),
        Arguments.of("a change entirely before the earliest date", programme(),
            List.of(ltft().startDate(EARLIEST_DATE.minusDays(2))
                .endDate(EARLIEST_DATE.minusDays(1)).build()),
            List.of(error("#/changes[0]/endDate", DATE_LIMITS_ERROR),
                error("#/changes[0]/startDate", DATE_LIMITS_ERROR))),
        Arguments.of("a counted OOPT in the last year of the date range", programme(),
            List.of(CctChangeDto.builder().type(OOPT).startDate(LocalDate.MAX)
                .endDate(LocalDate.MAX).countsTowardsCompletion(true).build()),
            List.of(error("#/changes[0]/endDate", DATE_LIMITS_ERROR),
                error("#/changes[0]/startDate", DATE_LIMITS_ERROR))),
        Arguments.of("a missing start grade", programme().startGrade(null), List.of(),
            List.of(error("#/programmeDetails/startGrade", "Please choose a start grade."))),
        Arguments.of("a missing programme length", programme().lengthMonths(null), List.of(),
            List.of(error("#/programmeDetails/lengthMonths",
                "Programme length must be a whole number of months between 1 and 120."))),
        Arguments.of("a programme length over 120 months", programme().lengthMonths(121),
            List.of(),
            List.of(error("#/programmeDetails/lengthMonths",
                "Programme length must be a whole number of months between 1 and 120."))),
        Arguments.of("missing training time lists",
            programme().additionalTrainingTime(null).acceleratedTrainingTime(null), List.of(),
            List.of(error("#/programmeDetails/acceleratedTrainingTime", "must not be null"),
                error("#/programmeDetails/additionalTrainingTime", "must not be null"))),
        Arguments.of("a missing training time entry",
            programme().additionalTrainingTime(Arrays.asList((CctTrainingTimeDto) null)),
            List.of(),
            List.of(error("#/programmeDetails/additionalTrainingTime[0]", "must not be null"))),
        Arguments.of("training time with no grade",
            programme().additionalTrainingTime(List.of(trainingTime(null, 3).build())),
            List.of(),
            List.of(error("#/programmeDetails/additionalTrainingTime[0]/grade",
                "Please choose a grade."))),
        Arguments.of("training time with no months",
            programme().additionalTrainingTime(List.of(trainingTime(ST1, null).build())),
            List.of(),
            List.of(error("#/programmeDetails/additionalTrainingTime[0]/months",
                "Training time must be a whole number of months between 1 and 24."))),
        Arguments.of("training time of zero months",
            programme().additionalTrainingTime(List.of(trainingTime(ST1, 0).build())),
            List.of(),
            List.of(error("#/programmeDetails/additionalTrainingTime[0]/months",
                "Training time must be a whole number of months between 1 and 24."))),
        Arguments.of("training time over 24 months",
            programme().additionalTrainingTime(List.of(trainingTime(ST1, 25).build())),
            List.of(),
            List.of(error("#/programmeDetails/additionalTrainingTime[0]/months",
                "Training time must be a whole number of months between 1 and 24."))),
        Arguments.of("training time without a note",
            programme().acceleratedTrainingTime(
                List.of(trainingTime(ST2, 3).build(), trainingTime(ST1, 3).notes("  ").build())),
            List.of(),
            List.of(error("#/programmeDetails/acceleratedTrainingTime[1]/notes",
                "Please enter a reason for the training time."))),
        Arguments.of("training time notes over 1000 characters",
            programme().additionalTrainingTime(
                List.of(trainingTime(ST1, 3).notes("x".repeat(1001)).build())),
            List.of(),
            List.of(error("#/programmeDetails/additionalTrainingTime[0]/notes",
                "Notes must be 1000 characters or less"))),
        Arguments.of("additional time over 24 months in total",
            programme().additionalTrainingTime(
                List.of(trainingTime(ST1, 12).build(), trainingTime(ST2, 13).build())),
            List.of(),
            List.of(error("#/programmeDetails/additionalTrainingTime",
                "Total additional training time cannot be more than 24 months."))),
        Arguments.of("accelerated time over 12 months in total",
            programme().acceleratedTrainingTime(
                List.of(trainingTime(ST1, 6).build(), trainingTime(ST2, 7).build())),
            List.of(),
            List.of(error("#/programmeDetails/acceleratedTrainingTime",
                "Total accelerated training time cannot be more than 12 months."))),
        Arguments.of("an adjusted length of zero with accelerated time over the limit",
            programme().lengthMonths(12)
                .additionalTrainingTime(List.of(trainingTime(ST1, 1).build()))
                .acceleratedTrainingTime(
                    List.of(trainingTime(ST1, 12).build(), trainingTime(ST1, 1).build())),
            List.of(),
            List.of(error("#/programmeDetails/acceleratedTrainingTime",
                "Total accelerated training time cannot be more than 12 months."))),
        Arguments.of("an adjusted length of zero",
            programme().lengthMonths(12)
                .acceleratedTrainingTime(List.of(trainingTime(ST1, 12).build())),
            List.of(),
            List.of(error("#/programmeDetails/acceleratedTrainingTime",
                "Adjusted training duration must be greater than zero."))),
        Arguments.of("more than 20 additional training time entries",
            programme().additionalTrainingTime(Stream.generate(() -> trainingTime(ST1, 1).build())
                .limit(21).toList()),
            List.of(),
            List.of(error("#/programmeDetails/additionalTrainingTime",
                "You cannot add more than 20 additional training time entries."))),
        Arguments.of("more than 20 accelerated training time entries",
            programme().acceleratedTrainingTime(Stream.generate(() -> trainingTime(ST1, 1).build())
                .limit(21).toList()),
            List.of(),
            List.of(error("#/programmeDetails/acceleratedTrainingTime",
                    "Total accelerated training time cannot be more than 12 months."),
                error("#/programmeDetails/acceleratedTrainingTime",
                    "You cannot add more than 20 accelerated training time entries."))),
        Arguments.of("an 18-month final grade without a note",
            programme().eighteenMonthFinalGrade(ST3), List.of(),
            List.of(error("#/programmeDetails/eighteenMonthFinalGradeNotes",
                "Please enter a reason for the 18-month final year."))),
        Arguments.of("a skipped grade without a note", programme().skippedGrade(ST2), List.of(),
            List.of(error("#/programmeDetails/skippedGradeNotes",
                "Please enter a reason for skipping a grade year."))),
        Arguments.of("a change with no type", programme(),
            List.of(ltft().type(null).startDate(null).wte(1.0).build()),
            List.of(error("#/changes[0]/type", "Please choose a change type."))),
        Arguments.of("a change with no start date", programme(),
            List.of(ltft().startDate(null).build()),
            List.of(error("#/changes[0]/startDate", "Please enter a start date."))),
        Arguments.of("a change ending before it starts", programme(),
            List.of(ltft().endDate(date("2023-12-31")).build()),
            List.of(error("#/changes[0]/startDate", "Start date cannot be after end date."))),
        Arguments.of("no end date on a change that isn't LTFT", programme(),
            List.of(parental().endDate(null).build()),
            List.of(error("#/changes[0]/endDate", "Please enter an end date."))),
        Arguments.of("an LTFT WTE of 100%", programme(), List.of(ltft().wte(1.0).build()),
            List.of(error("#/changes[0]/wte",
                "LTFT WTE must be a whole number between 1 and 99."))),
        Arguments.of("an LTFT WTE that isn't a whole percent", programme(),
            List.of(ltft().wte(0.505).build()),
            List.of(error("#/changes[0]/wte",
                "LTFT WTE must be a whole number between 1 and 99."))),
        Arguments.of("a counted OOPR credit of a fraction of a percent", programme(),
            List.of(ltft().type(OOPR).wte(0.125).build()),
            List.of(error("#/changes[0]/wte",
                "Approved OOPR credit must be a whole number between 1 and 100."))),
        Arguments.of("an LTFT change that doesn't count", programme(),
            List.of(ltft().countsTowardsCompletion(false).build()),
            List.of(error("#/changes[0]/countsTowardsCompletion",
                "LTFT must count towards completion."))),
        Arguments.of("a counted OOPR with no credit", programme(),
            List.of(ltft().type(OOPR).wte(null).build()),
            List.of(error("#/changes[0]/wte",
                "Approved OOPR credit must be a whole number between 1 and 100."))),
        Arguments.of("an uncounted OOPR with a credit", programme(),
            List.of(ltft().type(OOPR).countsTowardsCompletion(false).build()),
            List.of(error("#/changes[0]/wte",
                "Only an OOPR counted as training can have a credit."))),
        Arguments.of("an OOPT with a WTE", programme(), List.of(ltft().type(OOPT).build()),
            List.of(error("#/changes[0]/wte", "OOPT cannot have a WTE."))),
        Arguments.of("a counted OOPT over 12 months", programme(),
            List.of(ltft().type(OOPT).wte(null).endDate(date("2025-01-01")).build()),
            List.of(error("#/changes[0]/endDate",
                "OOPT counted as training cannot be more than 12 months."))),
        Arguments.of("a parental leave that counts", programme(),
            List.of(parental().countsTowardsCompletion(true).build()),
            List.of(error("#/changes[0]/countsTowardsCompletion",
                "PARENTAL cannot count towards completion."))),
        Arguments.of("notes over 1000 characters", programme(),
            List.of(parental().notes("x".repeat(1001)).build()),
            List.of(error("#/changes[0]/notes", "Notes must be 1000 characters or less"))),
        Arguments.of("overlapping changes", programme(),
            List.of(ltft().build(), parental().startDate(date("2024-12-31")).build()),
            List.of(error("#/changes[1]/startDate",
                "This change overlaps an existing change (01/01/2024 – 31/12/2024)."))),
        Arguments.of("two open LTFT changes", programme(), List.of(
                ltft().startDate(date("2030-01-01")).endDate(null).build(),
                ltft().startDate(date("2031-01-01")).endDate(null).build()),
            List.of(error("#/changes[1]/endDate",
                "Only one LTFT change can project the remaining training time."))),
        Arguments.of("an open LTFT change that starts before a dated change ends", programme(),
            List.of(ltft().startDate(date("2024-12-31")).endDate(null).build(), ltft().build()),
            List.of(error("#/changes[0]/startDate",
                "Projected change must begin after all other completed changes."))),
        Arguments.of("a missing change", programme(), Arrays.asList((CctChangeDto) null),
            List.of(error("#/changes[0]", "must not be null"))),
        Arguments.of("more than 50 changes", programme(), oneDayChanges(51),
            List.of(error("#/changes", "size must be between 0 and 50")))
    );
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
        "#/changes[0]/startDate",
        "#/changes[0]/wte",
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

    assertThat("Unexpected global errors.", result.getGlobalErrors(), is(empty()));
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
    List<CctChangeDto> changes = new ArrayList<>();
    IntStream.range(0, count).forEach(day -> changes.add(parental()
        .startDate(date("2024-01-01").plusDays(day))
        .endDate(date("2024-01-01").plusDays(day))
        .build()));
    return changes;
  }

  private static LocalDate date(String date) {
    return LocalDate.parse(date);
  }

  private static BodyValidationError error(String pointer, String detail) {
    return new BodyValidationError(pointer, detail);
  }
}
