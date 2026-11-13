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

import com.fasterxml.jackson.core.JsonPointer;
import java.util.Comparator;
import java.util.List;
import org.springframework.validation.FieldError;

/**
 * A detailed request body validation error.
 *
 * @param pointer The JSON pointer for the body's field.
 * @param detail  The validation failure detail.
 */
public record BodyValidationError(String pointer, String detail) {

  /**
   * Convert field errors to body validation errors, sorted by pointer and then detail.
   *
   * @param fieldErrors The field errors to convert.
   * @return The equivalent body validation errors.
   */
  public static List<BodyValidationError> fromFieldErrors(List<FieldError> fieldErrors) {
    return fieldErrors.stream()
        .map(fe -> {
          String pointer = "#/" + fe.getField().replace('.', JsonPointer.SEPARATOR);
          return new BodyValidationError(pointer, fe.getDefaultMessage());
        })
        .sorted(
            Comparator.comparing(BodyValidationError::pointer)
                .thenComparing(BodyValidationError::detail)
        )
        .toList();
  }
}
