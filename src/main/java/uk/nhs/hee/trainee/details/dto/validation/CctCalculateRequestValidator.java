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

package uk.nhs.hee.trainee.details.dto.validation;

import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.LTFT;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.OOPR;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.OOPT;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;
import uk.nhs.hee.trainee.details.dto.CctCalculateRequestDto;
import uk.nhs.hee.trainee.details.dto.CctChangeDto;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto;
import uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType;

/**
 * Implementation of the {@link CctCalculateRequestValidation} validator.
 */
public class CctCalculateRequestValidator implements
    ConstraintValidator<CctCalculateRequestValidation, CctCalculateRequestDto> {

  private static final int NOTES_MAX_LENGTH = 1000;
  private static final DateTimeFormatter MESSAGE_DATE_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");

  @Override
  public boolean isValid(CctCalculateRequestDto request, ConstraintValidatorContext context) {
    Violations violations = new Violations(context);

    if (request.programmeDetails() != null) {
      validateProgrammeDetails(request.programmeDetails(), violations);
    }

    List<CctChangeDto> changes = request.changes();
    if (changes != null) {
      for (int index = 0; index < changes.size(); index++) {
        CctChangeDto change = changes.get(index);
        if (change != null && change.type() != null) {
          validateChangeDates(change, index, violations);
          validateChangeCredit(change, index, violations);
          validateChangeNotes(change, index, violations);
        }
      }
      validateAcrossChanges(changes, violations);
    }

    return violations.isEmpty();
  }

  private static void validateProgrammeDetails(CctProgrammeDetailsDto details,
      Violations violations) {
    if (isWholeNumberBetween(details.lengthMonths(), 1, 120)
        && isWholeNumberBetween(details.additionalMonths(), 0, 24)
        && isWholeNumberBetween(details.acceleratedMonths(), 0, 12)
        && details.lengthMonths() + details.additionalMonths() - details.acceleratedMonths() <= 0) {
      violations.onProgrammeDetails("acceleratedMonths",
          "Adjusted training duration must be greater than zero.");
    }

    if (isPositive(details.additionalMonths()) && isBlank(details.additionalMonthsNotes())) {
      violations.onProgrammeDetails("additionalMonthsNotes",
          "Please enter a reason for additional training time.");
    }
    if (isPositive(details.acceleratedMonths()) && isBlank(details.acceleratedMonthsNotes())) {
      violations.onProgrammeDetails("acceleratedMonthsNotes",
          "Please enter a reason for accelerated training time.");
    }
    if (details.eighteenMonthFinalGrade() != null
        && isBlank(details.eighteenMonthFinalGradeNotes())) {
      violations.onProgrammeDetails("eighteenMonthFinalGradeNotes",
          "Please enter a reason for the 18-month final year.");
    }
    if (details.skippedGrade() != null && isBlank(details.skippedGradeNotes())) {
      violations.onProgrammeDetails("skippedGradeNotes",
          "Please enter a reason for skipping a grade year.");
    }
  }

  private static void validateChangeDates(CctChangeDto change, int index,
      Violations violations) {
    LocalDate startDate = change.startDate();
    LocalDate endDate = change.endDate();

    if (startDate == null) {
      violations.onChange(index, "startDate", "Please enter a start date.");
    }
    if (endDate == null && change.type() != LTFT) {
      violations.onChange(index, "endDate", "Please enter an end date.");
    } else if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
      violations.onChange(index, "startDate", "Start date cannot be after end date.");
    }
  }

  private static void validateChangeCredit(CctChangeDto change, int index,
      Violations violations) {
    CctChangeType type = change.type();
    Double wte = change.wte();
    boolean counts = change.countsTowardsCompletion();

    if (type == LTFT) {
      if (!counts) {
        violations.onChange(index, "countsTowardsCompletion",
            "LTFT must count towards completion.");
      }
      if (!isWholePercent(wte, 99)) {
        violations.onChange(index, "wte", "LTFT WTE must be a whole number between 1 and 99.");
      }
    } else if (type == OOPR) {
      if (counts && !isWholePercent(wte, 100)) {
        violations.onChange(index, "wte",
            "Approved OOPR credit must be a whole number between 1 and 100.");
      }
      if (!counts && wte != null) {
        violations.onChange(index, "wte", "Only an OOPR counted as training can have a credit.");
      }
    } else {
      if (wte != null) {
        violations.onChange(index, "wte", type + " cannot have a WTE.");
      }
      if (type != OOPT && counts) {
        violations.onChange(index, "countsTowardsCompletion",
            type + " cannot count towards completion.");
      }
    }

    LocalDate startDate = change.startDate();
    LocalDate endDate = change.endDate();
    if (type == OOPT && counts && startDate != null && endDate != null
        && endDate.isAfter(startDate.plusMonths(12).minusDays(1))) {
      violations.onChange(index, "endDate",
          "OOPT counted as training cannot be more than 12 months.");
    }
  }

  private static void validateChangeNotes(CctChangeDto change, int index,
      Violations violations) {
    String notes = change.notes();
    if (notes != null && notes.strip().length() > NOTES_MAX_LENGTH) {
      violations.onChange(index, "notes",
          "Notes must be " + NOTES_MAX_LENGTH + " characters or less");
    }
  }

  private static void validateAcrossChanges(List<CctChangeDto> changes, Violations violations) {
    List<IndexedChange> entries = IntStream.range(0, changes.size())
        .filter(index -> changes.get(index) != null)
        .mapToObj(index -> new IndexedChange(index, changes.get(index)))
        .toList();
    List<IndexedChange> dated = entries.stream()
        .filter(entry -> entry.change().startDate() != null && entry.change().endDate() != null)
        .toList();
    List<IndexedChange> open = entries.stream()
        .filter(entry -> entry.change().type() == LTFT && entry.change().endDate() == null
            && entry.change().startDate() != null)
        .toList();

    for (IndexedChange entry : dated) {
      CctChangeDto change = entry.change();
      dated.stream()
          .filter(other -> other.index() < entry.index()
              && !change.endDate().isBefore(other.change().startDate())
              && !change.startDate().isAfter(other.change().endDate()))
          .findFirst()
          .ifPresent(overlapped -> violations.onChange(entry.index(), "startDate",
              "This change overlaps an existing change ("
                  + overlapped.change().startDate().format(MESSAGE_DATE_FORMAT) + " – "
                  + overlapped.change().endDate().format(MESSAGE_DATE_FORMAT) + ")."));
    }

    open.stream()
        .skip(1)
        .forEach(entry -> violations.onChange(entry.index(), "endDate",
            "Only one LTFT change can project the remaining training time."));
    for (IndexedChange entry : open) {
      LocalDate startDate = entry.change().startDate();
      if (dated.stream().anyMatch(other -> !startDate.isAfter(other.change().endDate()))) {
        violations.onChange(entry.index(), "startDate",
            "Projected change must begin after all other completed changes.");
      }
    }
  }

  private static boolean isWholeNumberBetween(Integer value, int min, int max) {
    return value != null && value >= min && value <= max;
  }

  private static boolean isPositive(Integer value) {
    return value != null && value > 0;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static boolean isWholePercent(Double fraction, int maxPercent) {
    if (fraction == null) {
      return false;
    }
    long percent = Math.round(fraction * 100);
    return Math.abs(fraction * 100 - percent) < 1e-9 && percent >= 1 && percent <= maxPercent;
  }

  private record IndexedChange(int index, CctChangeDto change) {

  }

  private static final class Violations {

    private final ConstraintValidatorContext context;
    private boolean empty = true;

    private Violations(ConstraintValidatorContext context) {
      this.context = context;
      context.disableDefaultConstraintViolation();
    }

    private void onProgrammeDetails(String field, String message) {
      empty = false;
      context.buildConstraintViolationWithTemplate(message)
          .addPropertyNode("programmeDetails")
          .addPropertyNode(field)
          .addConstraintViolation();
    }

    private void onChange(int index, String field, String message) {
      empty = false;
      context.buildConstraintViolationWithTemplate(message)
          .addPropertyNode("changes")
          .addPropertyNode(field).inIterable().atIndex(index)
          .addConstraintViolation();
    }

    private boolean isEmpty() {
      return empty;
    }
  }
}
