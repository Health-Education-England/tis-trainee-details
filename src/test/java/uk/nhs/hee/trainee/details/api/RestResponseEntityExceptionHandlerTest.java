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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import uk.nhs.hee.trainee.details.api.RestResponseEntityExceptionHandler.BodyValidationError;
import uk.nhs.hee.trainee.details.api.RestResponseEntityExceptionHandler.ParameterValidationError;

class RestResponseEntityExceptionHandlerTest {

  private static final HttpStatusCode STATUS_CODE = HttpStatusCode.valueOf(
      new Random().nextInt(400, 500));

  private RestResponseEntityExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new RestResponseEntityExceptionHandler();
  }

  @Test
  void shouldHandleMethodArgumentNotValid() {
    BeanPropertyBindingResult result = new BeanPropertyBindingResult(null, "dto");
    result.addError(new FieldError("dto", "field2", "detail3"));
    result.addError(new FieldError("dto", "field1", "detail2"));
    result.addError(new FieldError("dto", "field1", "detail1"));
    result.addError(new FieldError("dto", "field1.subField", "detail4"));
    MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
        mock(MethodParameter.class), result);

    HttpHeaders headers = HttpHeaders.EMPTY;
    WebRequest request = new ServletWebRequest(new MockHttpServletRequest());

    ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(exception, headers,
        STATUS_CODE, request);

    assertThat("Unexpected response.", response, notNullValue());
    assertThat("Unexpected headers.", response.getHeaders(), is(headers));
    assertThat("Unexpected response code.", response.getStatusCode(), is(STATUS_CODE));
    assertThat("Unexpected response type.", response.getBody(), instanceOf(ProblemDetail.class));

    ProblemDetail problem = (ProblemDetail) response.getBody();
    assertThat("Unexpected problem.", problem, notNullValue());
    assertThat("Unexpected problem title.", problem.getTitle(), is("Validation failure"));
    assertThat("Unexpected problem status.", problem.getStatus(), is(STATUS_CODE.value()));
    assertThat("Unexpected problem status.", problem.getInstance(), nullValue());
    assertThat("Unexpected problem status.", problem.getType(), is(URI.create("about:blank")));
    assertThat("Unexpected problem status.", problem.getDetail(), nullValue());

    Map<String, Object> problemProperties = problem.getProperties();
    assertThat("Unexpected problem properties.", problemProperties, notNullValue());
    assertThat("Unexpected property count.", problemProperties.size(), is(1));

    Object errors = problemProperties.get("errors");
    assertThat("Unexpected errors type.", errors, instanceOf(List.class));

    List<BodyValidationError> errorsList = (List<BodyValidationError>) errors;
    assertThat("Unexpected errors count.", errorsList.size(), is(4));

    BodyValidationError error = errorsList.get(0);
    assertThat("Unexpected error pointer.", error.pointer(), is("#/field1"));
    assertThat("Unexpected error detail.", error.detail(), is("detail1"));

    error = errorsList.get(1);
    assertThat("Unexpected error pointer.", error.pointer(), is("#/field1"));
    assertThat("Unexpected error detail.", error.detail(), is("detail2"));

    error = errorsList.get(2);
    assertThat("Unexpected error pointer.", error.pointer(), is("#/field1/subField"));
    assertThat("Unexpected error detail.", error.detail(), is("detail4"));

    error = errorsList.get(3);
    assertThat("Unexpected error pointer.", error.pointer(), is("#/field2"));
    assertThat("Unexpected error detail.", error.detail(), is("detail3"));
  }

  @Test
  void shouldHandleMethodValidationException() {
    // Use a linked hash map to allow verification of sorting.
    Map<String, List<String>> parametersToMessages = new LinkedHashMap<>();
    parametersToMessages.put("parameter2", List.of("detail3"));
    parametersToMessages.put("parameter1", List.of("detail2", "detail1"));
    MethodValidationResultStub validationResult = new MethodValidationResultStub(
        parametersToMessages);
    HandlerMethodValidationException exception = new HandlerMethodValidationException(
        validationResult);

    HttpHeaders headers = HttpHeaders.EMPTY;
    WebRequest request = new ServletWebRequest(new MockHttpServletRequest());

    ResponseEntity<Object> response = handler.handleHandlerMethodValidationException(exception,
        headers, STATUS_CODE, request);

    assertThat("Unexpected response.", response, notNullValue());
    assertThat("Unexpected headers.", response.getHeaders(), is(headers));
    assertThat("Unexpected response code.", response.getStatusCode(), is(STATUS_CODE));
    assertThat("Unexpected response type.", response.getBody(), instanceOf(ProblemDetail.class));

    ProblemDetail problem = (ProblemDetail) response.getBody();
    assertThat("Unexpected problem.", problem, notNullValue());
    assertThat("Unexpected problem title.", problem.getTitle(), is("Validation failure"));
    assertThat("Unexpected problem status.", problem.getStatus(), is(STATUS_CODE.value()));
    assertThat("Unexpected problem status.", problem.getInstance(), nullValue());
    assertThat("Unexpected problem status.", problem.getType(), is(URI.create("about:blank")));
    assertThat("Unexpected problem status.", problem.getDetail(), nullValue());

    Map<String, Object> problemProperties = problem.getProperties();
    assertThat("Unexpected problem properties.", problemProperties, notNullValue());
    assertThat("Unexpected property count.", problemProperties.size(), is(1));

    Object errors = problemProperties.get("errors");
    assertThat("Unexpected errors type.", errors, instanceOf(List.class));

    List<ParameterValidationError> errorsList = (List<ParameterValidationError>) errors;
    assertThat("Unexpected errors count.", errorsList.size(), is(3));

    ParameterValidationError error = errorsList.get(0);
    assertThat("Unexpected error parameter.", error.parameter(), is("parameter1"));
    assertThat("Unexpected error detail.", error.detail(), is("detail1"));

    error = errorsList.get(1);
    assertThat("Unexpected error parameter.", error.parameter(), is("parameter1"));
    assertThat("Unexpected error detail.", error.detail(), is("detail2"));

    error = errorsList.get(2);
    assertThat("Unexpected error parameter.", error.parameter(), is("parameter2"));
    assertThat("Unexpected error detail.", error.detail(), is("detail3"));
  }

  /**
   * A test stub for {@link MethodValidationResult}.
   *
   * @param parametersToMessages A map where the key is parameter names and the value is a list of
   *                             error messages.
   */
  private record MethodValidationResultStub(
      Map<String, List<String>> parametersToMessages) implements MethodValidationResult {

    @Override
    public @NotNull Object getTarget() {
      return "testTarget";
    }

    @Override
    public Method getMethod() {
      return null;
    }

    @Override
    public boolean isForReturnValue() {
      return false;
    }

    @Override
    public @NotNull List<ParameterValidationResult> getAllValidationResults() {
      return parametersToMessages.entrySet().stream()
          .map(entry -> {
            String parameterName = entry.getKey();
            MethodParameter parameter = mock(MethodParameter.class);
            when(parameter.getParameterName()).thenReturn(parameterName);

            List<DefaultMessageSourceResolvable> messages = entry.getValue().stream()
                .map(msg -> new DefaultMessageSourceResolvable(null, null, msg))
                .toList();

            return new ParameterValidationResult(parameter, null, messages, null, null, null);
          })
          .toList();
    }
  }
}
