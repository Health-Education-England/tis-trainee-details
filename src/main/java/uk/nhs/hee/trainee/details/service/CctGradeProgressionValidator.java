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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toSet;
import static uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto.CctTrainingTimeDto.monthsIn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto.CctTrainingTimeDto;
import uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade;
import uk.nhs.hee.trainee.details.service.CctGradeProgression.Year;

/**
 * A validator for CCT programme details against their grade progression.
 */
@Component
public class CctGradeProgressionValidator {

  private final CctGradeProgression gradeProgression;

  /**
   * Create a validator for CCT programme details against their grade progression.
   *
   * @param gradeProgression The grade progression for CCT programmes.
   */
  public CctGradeProgressionValidator(CctGradeProgression gradeProgression) {
    this.gradeProgression = gradeProgression;
  }

  /**
   * Validate the programme details against the grade progression: each grade they name must be in
   * it, the skipped grade cannot be the start grade, and the accelerated training time in a grade
   * must be less than the grade's length plus its additional training time.
   *
   * @param details The programme details.
   * @return The errors, empty when the programme details are valid.
   */
  public List<FieldError> validate(CctProgrammeDetailsDto details) {
    List<FieldError> errors = new ArrayList<>();
    List<Year> years = gradeProgression.years(details);
    Set<CctTrainingGrade> grades = gradesIn(years);
    validateTrainingTimeGrades(details.additionalTrainingTime(), "additionalTrainingTime", grades,
        errors);
    validateTrainingTimeGrades(details.acceleratedTrainingTime(), "acceleratedTrainingTime",
        grades, errors);

    CctTrainingGrade skippedGrade = details.skippedGrade();
    if (skippedGrade != null && skippedGrade == details.startGrade()) {
      addError(errors, "skippedGrade", "The start grade cannot be skipped.");
    } else if (skippedGrade != null
        && !gradesIn(gradeProgression.years(details, null, null)).contains(skippedGrade)) {
      addError(errors, "skippedGrade", notInProgression(skippedGrade));
    }
    CctTrainingGrade eighteenMonthFinalGrade = details.eighteenMonthFinalGrade();
    if (eighteenMonthFinalGrade != null
        && !gradesIn(gradeProgression.years(details, skippedGrade, null))
        .contains(eighteenMonthFinalGrade)) {
      addError(errors, "eighteenMonthFinalGrade", notInProgression(eighteenMonthFinalGrade));
    } else if (eighteenMonthFinalGrade != null && !endsInEighteenMonthYear(years)) {
      addError(errors, "eighteenMonthFinalGrade",
          "An 18-month final year must be the last full year of a programme length that includes "
              + "a half year, such as 66 months.");
    }

    Map<CctTrainingGrade, Integer> gradeMonths = years.stream()
        .filter(year -> year.grade() != null)
        .collect(groupingBy(Year::grade, summingInt(Year::gradeMonths)));
    List<CctTrainingTimeDto> accelerated = details.acceleratedTrainingTime();
    Set<CctTrainingGrade> checked = new HashSet<>();
    for (int index = 0; index < accelerated.size(); index++) {
      CctTrainingGrade grade = accelerated.get(index).grade();
      if (gradeMonths.containsKey(grade) && checked.add(grade)) {
        int limit = gradeMonths.get(grade) + monthsIn(details.additionalTrainingTime(), grade);
        if (monthsIn(accelerated, grade) >= limit) {
          addError(errors, "acceleratedTrainingTime[%d].months".formatted(index),
              "Accelerated training time in %s must be less than %d months."
                  .formatted(grade, limit));
        }
      }
    }

    return errors;
  }

  /**
   * Validate that each training time entry's grade is in the grade progression.
   *
   * @param entries The training time entries.
   * @param field   The name of the programme details field holding them.
   * @param grades  The grades in the grade progression.
   * @param errors  The errors to add to.
   */
  private static void validateTrainingTimeGrades(List<CctTrainingTimeDto> entries, String field,
      Set<CctTrainingGrade> grades, List<FieldError> errors) {
    for (int index = 0; index < entries.size(); index++) {
      CctTrainingGrade grade = entries.get(index).grade();
      if (!grades.contains(grade)) {
        addError(errors, "%s[%d].grade".formatted(field, index), notInProgression(grade));
      }
    }
  }

  private static boolean endsInEighteenMonthYear(List<Year> years) {
    Year last = years.get(years.size() - 1);
    return last.extendedToEighteenMonths() && last.gradeMonths() == 18;
  }

  private static Set<CctTrainingGrade> gradesIn(List<Year> years) {
    return years.stream()
        .map(Year::grade)
        .filter(Objects::nonNull)
        .collect(toSet());
  }

  private static String notInProgression(CctTrainingGrade grade) {
    return grade + " is not part of this programme's grade progression.";
  }

  /**
   * Add an error for a {@link CctProgrammeDetailsDto} field.
   *
   * @param errors  The errors to add to.
   * @param field   The name of the field.
   * @param message The error message.
   */
  private static void addError(List<FieldError> errors, String field, String message) {
    errors.add(new FieldError("CctProgrammeDetailsDto", "programmeDetails." + field, message));
  }
}
