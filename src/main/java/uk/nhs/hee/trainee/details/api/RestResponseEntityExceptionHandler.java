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

package uk.nhs.hee.trainee.details.api;

import com.fasterxml.jackson.core.JsonPointer;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.nhs.hee.trainee.details.exception.EmailAlreadyInUseException;

/**
 * Exception handler for REST requests.
 */
@RestControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

  private static final String TITLE_VALIDATION_FAILURE = "Validation failure";

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
      HttpHeaders headers, HttpStatusCode status, WebRequest request) {

    BindingResult result = ex.getBindingResult();
    List<BodyValidationError> errors = result.getFieldErrors().stream()
        .map(fe -> {
          String pointer = "#/" + fe.getField().replace('.', JsonPointer.SEPARATOR);
          return new BodyValidationError(pointer, fe.getDefaultMessage());
        })
        .sorted(
            Comparator.comparing(BodyValidationError::pointer)
                .thenComparing(BodyValidationError::detail)
        )
        .toList();

    ProblemDetail problemDetail = ProblemDetail.forStatus(status);
    problemDetail.setTitle(TITLE_VALIDATION_FAILURE);
    problemDetail.setProperty("errors", errors);
    return handleExceptionInternal(ex, problemDetail, headers, status, request);
  }

  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status,
      WebRequest request) {

    List<ParameterValidationError> errors = ex.getAllValidationResults().stream()
        .flatMap(result -> {
          String parameterName = result.getMethodParameter().getParameterName();

          return result.getResolvableErrors().stream()
              .map(err -> new ParameterValidationError(parameterName, err.getDefaultMessage()));
        })
        .sorted(
            Comparator.comparing(ParameterValidationError::parameter)
                .thenComparing(ParameterValidationError::detail)
        )
        .toList();

    ProblemDetail problemDetail = ProblemDetail.forStatus(status);
    problemDetail.setTitle(ex.getReason());
    problemDetail.setProperty("errors", errors);
    return handleExceptionInternal(ex, problemDetail, headers, status, request);
  }

  /**
   * Handle email already in use exceptions.
   *
   * @param ex The exception.
   * @return A response entity with problem details.
   */
  @ExceptionHandler(EmailAlreadyInUseException.class)
  public ResponseEntity<Object> handleEmailAlreadyInUse(EmailAlreadyInUseException ex) {
    List<ParameterValidationError> errors = List.of(
        new ParameterValidationError("email", ex.getMessage())
    );
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problemDetail.setTitle("Email already in use");
    problemDetail.setProperty("errors", errors);
    return ResponseEntity.badRequest().body(problemDetail);
  }

  /**
   * A detailed parameter validation error.
   *
   * @param parameter The name of the parameter.
   * @param detail    The validation failure detail.
   */
  record ParameterValidationError(String parameter, String detail) {

  }

  /**
   * A detailed request body validation error.
   *
   * @param pointer The JSON pointer for the body's field.
   * @param detail  The validation failure detail.
   */
  record BodyValidationError(String pointer, String detail) {

  }
}
