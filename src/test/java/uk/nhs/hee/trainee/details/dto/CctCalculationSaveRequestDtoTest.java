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
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST1;

import jakarta.validation.Validation;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import uk.nhs.hee.trainee.details.dto.CctCalculationSaveRequestDto.CctCalculationSaveRequestDtoBuilder;
import uk.nhs.hee.trainee.details.dto.CctChangeDto.CctChangeDtoBuilder;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto.CctProgrammeDetailsDtoBuilder;
import uk.nhs.hee.trainee.details.dto.validation.Create;

class CctCalculationSaveRequestDtoTest {

  private SpringValidatorAdapter validator;

  @BeforeEach
  void setUp() {
    validator = new SpringValidatorAdapter(
        Validation.buildDefaultValidatorFactory().getValidator());
  }

  @Test
  void shouldAcceptNewCalculation() {
    List<BodyValidationError> errors = validate(request().build(), Create.class);

    assertThat("Unexpected errors.", errors, is(empty()));
  }

  @Test
  void shouldAcceptCalculationWithNoChanges() {
    List<BodyValidationError> errors = validate(request().changes(List.of()).build(),
        Create.class);

    assertThat("Unexpected errors.", errors, is(empty()));
  }

  @Test
  void shouldAcceptIdWhenNotCreating() {
    List<BodyValidationError> errors = validate(request().id(UUID.randomUUID()).build());

    assertThat("Unexpected errors.", errors, is(empty()));
  }

  @Test
  void shouldRejectIdWhenCreating() {
    List<BodyValidationError> errors = validate(request().id(UUID.randomUUID()).build(),
        Create.class);

    assertThat("Unexpected errors.", errors, is(List.of(error("#/id", "must be null"))));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void shouldRejectBlankName(String name) {
    List<BodyValidationError> errors = validate(request().name(name).build());

    assertThat("Unexpected errors.", errors, is(List.of(error("#/name", "must not be blank"))));
  }

  @Test
  void shouldRejectMissingProgrammeDetailsAndChanges() {
    List<BodyValidationError> errors = validate(CctCalculationSaveRequestDto.builder()
        .name("Test Calculation")
        .build());

    assertThat("Unexpected errors.", errors, is(List.of(
        error("#/changes", "must not be null"),
        error("#/programmeDetails", "must not be null"))));
  }

  @Test
  void shouldApplyCalculationRulesWhenCreating() {
    List<BodyValidationError> errors = validate(request()
            .programmeDetails(programme().additionalMonths(6).build())
            .changes(List.of(ltft().wte(1.0).build()))
            .build(),
        Create.class);

    assertThat("Unexpected errors.", errors, is(List.of(
        error("#/changes[0]/wte", "LTFT WTE must be a whole number between 1 and 99."),
        error("#/programmeDetails/additionalMonthsNotes",
            "Please enter a reason for additional training time."))));
  }

  @Test
  void shouldApplyCalculationRulesWhenNotCreating() {
    List<BodyValidationError> errors = validate(request()
        .id(UUID.randomUUID())
        .changes(List.of(ltft().endDate(LocalDate.of(2023, 12, 31)).build()))
        .build());

    assertThat("Unexpected errors.", errors, is(List.of(
        error("#/changes[0]/startDate", "Start date cannot be after end date."))));
  }

  private List<BodyValidationError> validate(CctCalculationSaveRequestDto request,
      Object... groups) {
    BeanPropertyBindingResult result = new BeanPropertyBindingResult(request, "request");
    validator.validate(request, result, groups);

    assertThat("Unexpected global errors.", result.getGlobalErrors(), is(empty()));
    return result.getFieldErrors().stream()
        .map(fieldError -> error("#/" + fieldError.getField().replace('.', '/'),
            fieldError.getDefaultMessage()))
        .sorted(Comparator.comparing(BodyValidationError::pointer)
            .thenComparing(BodyValidationError::detail))
        .toList();
  }

  private static CctCalculationSaveRequestDtoBuilder request() {
    return CctCalculationSaveRequestDto.builder()
        .name("Test Calculation")
        .programmeDetails(programme().build())
        .changes(List.of(ltft().build()));
  }

  private static CctProgrammeDetailsDtoBuilder programme() {
    return CctProgrammeDetailsDto.builder()
        .specialty("Paediatrics")
        .startDate(LocalDate.of(2023, 8, 2))
        .lengthMonths(84)
        .startGrade(ST1)
        .additionalMonths(0)
        .acceleratedMonths(0);
  }

  private static CctChangeDtoBuilder ltft() {
    return CctChangeDto.builder()
        .type(LTFT)
        .startDate(LocalDate.of(2024, 1, 1))
        .endDate(LocalDate.of(2024, 12, 31))
        .wte(0.5)
        .countsTowardsCompletion(true);
  }

  private static BodyValidationError error(String pointer, String detail) {
    return new BodyValidationError(pointer, detail);
  }

  record BodyValidationError(String pointer, String detail) {

  }
}
