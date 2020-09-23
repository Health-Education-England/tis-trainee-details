/*
 * The MIT License (MIT)
 *
 * Copyright 2020 Crown Copyright (Health Education England)
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.trainee.details.dto.validation.Create;

class TraineeProfileDtoTest {

  private TraineeProfileDto dto;

  private Validator validator;

  @BeforeEach
  void setUp() {
    dto = new TraineeProfileDto();

    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void shouldPassValidationWhenIdNull() {
    Set<ConstraintViolation<TraineeProfileDto>> violations = validator.validateProperty(dto, "id");

    assertThat("Unexpected number of violations.", violations.size(), is(0));
  }

  @Test
  void shouldPassValidationWhenIdNotNull() {
    dto.setId("notNull");

    Set<ConstraintViolation<TraineeProfileDto>> violations = validator.validateProperty(dto, "id");

    assertThat("Unexpected number of violations.", violations.size(), is(0));
  }

  @Test
  void shouldPassValidationWhenTisIdNull() {
    Set<ConstraintViolation<TraineeProfileDto>> violations = validator
        .validateProperty(dto, "traineeTisId");

    assertThat("Unexpected number of violations.", violations.size(), is(0));
  }

  @Test
  void shouldPassValidationWhenTisIdNotNull() {
    dto.setTraineeTisId("notNull");
    Set<ConstraintViolation<TraineeProfileDto>> violations = validator
        .validateProperty(dto, "traineeTisId");

    assertThat("Unexpected number of violations.", violations.size(), is(0));
  }

  @Test
  void shouldPassValidationWhenCreateIdNull() {
    Set<ConstraintViolation<TraineeProfileDto>> violations = validator
        .validateProperty(dto, "id", Create.class);

    assertThat("Unexpected number of violations.", violations.size(), is(0));
  }

  @Test
  void shouldPassValidationWhenCreateIdNotNull() {
    dto.setId("notNull");

    Set<ConstraintViolation<TraineeProfileDto>> violations = validator
        .validateProperty(dto, "id", Create.class);

    assertThat("Unexpected number of violations.", violations.size(), is(1));

    ConstraintViolation<TraineeProfileDto> violation = violations.iterator().next();
    assertThat("Unexpected violation.", violation.getMessage(), is("must be null"));
  }

  @Test
  void shouldFailValidationWhenCreateTisIdNull() {
    Set<ConstraintViolation<TraineeProfileDto>> violations = validator
        .validateProperty(dto, "traineeTisId", Create.class);

    assertThat("Unexpected number of violations.", violations.size(), is(1));

    ConstraintViolation<TraineeProfileDto> violation = violations.iterator().next();
    assertThat("Unexpected violation.", violation.getMessage(), is("must not be null"));
  }

  @Test
  void shouldPassValidationWhenCreateTisIdNotNull() {
    dto.setTraineeTisId("notNull");
    Set<ConstraintViolation<TraineeProfileDto>> violations = validator
        .validateProperty(dto, "traineeTisId", Create.class);

    assertThat("Unexpected number of violations.", violations.size(), is(0));
  }
}
