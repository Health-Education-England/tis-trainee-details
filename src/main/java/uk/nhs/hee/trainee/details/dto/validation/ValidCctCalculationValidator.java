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

package uk.nhs.hee.trainee.details.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import uk.nhs.hee.trainee.details.dto.CctCalculationDetailDto;
import uk.nhs.hee.trainee.details.dto.CctCalculationDetailDto.CctChangeDto;

/**
 * Validate a CCT calculation's cross-field constraints.
 */
public class ValidCctCalculationValidator
    implements ConstraintValidator<ValidCctCalculation, CctCalculationDetailDto> {

  @Override
  public boolean isValid(CctCalculationDetailDto value, ConstraintValidatorContext context) {
    if (value == null || value.programmeMembership() == null || value.changes() == null) {
      return true;
    }

    if (value.programmeMembership().endDate() == null) {
      return true;
    }

    boolean valid = true;
    List<CctChangeDto> changes = value.changes();
    context.disableDefaultConstraintViolation();

    for (int i = 0; i < changes.size(); i++) {
      CctChangeDto change = changes.get(i);
      if (change == null || change.endDate() == null) {
        continue;
      }

      if (change.endDate().isAfter(value.programmeMembership().endDate())) {
        context
            .buildConstraintViolationWithTemplate("must not be after programmeMembership.endDate")
            .addPropertyNode("changes")
            .addPropertyNode("endDate")
            .inIterable().atIndex(i)
            .addConstraintViolation();
        valid = false;
      }
    }

    return valid;
  }
}
