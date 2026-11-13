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

package uk.nhs.hee.trainee.details.dto.deserializer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.nhs.hee.trainee.details.dto.CctChangeDto;

class CctChangesDeserializerTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 200})
  void shouldKeepAllChangesWhenWithinLimit(int count) throws IOException {
    Request request = mapper.readValue(requestJson(changesJson(count)), Request.class);

    assertThat("Unexpected changes.", request.changes(), is(changes(count)));
    assertThat("Unexpected name.", request.name(), is("After"));
  }

  @ParameterizedTest
  @ValueSource(ints = {201, 202, 1000})
  void shouldKeepOneChangePastLimitWhenOverLimit(int count) throws IOException {
    Request request = mapper.readValue(requestJson(changesJson(count)), Request.class);

    assertThat("Unexpected changes.", request.changes(), is(changes(201)));
    assertThat("Unexpected name.", request.name(), is("After"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"{\"type\":\"NOPE\"}", "{\"a\":{\"b\":[1]}}", "[1,[2]]", "\"x\"", "5",
      "true", "null"})
  void shouldSkipItemsPastLimitWithoutReadingThem(String item) throws IOException {
    String json = changesJson(201) + "," + item + "," + item;

    Request request = mapper.readValue(requestJson(json), Request.class);

    assertThat("Unexpected changes.", request.changes(), is(changes(201)));
    assertThat("Unexpected name.", request.name(), is("After"));
  }

  @Test
  void shouldKeepNullChanges() throws IOException {
    String json = "null,{\"notes\":\"Change 0\"},null";

    Request request = mapper.readValue(requestJson(json), Request.class);

    assertThat("Unexpected changes.", request.changes(),
        contains(nullValue(), is(changes(1).get(0)), nullValue()));
  }

  @Test
  void shouldDeserializeNull() throws IOException {
    Request request = mapper.readValue("{\"changes\":null}", Request.class);

    assertThat("Unexpected changes.", request.changes(), nullValue());
  }

  @ParameterizedTest
  @ValueSource(strings = {"{\"type\":\"NOPE\"}", "[]", "\"x\"", "5", "true"})
  void shouldRejectUnreadableItemWithinLimit(String item) {
    String content = requestJson(changesJson(200) + "," + item);

    assertThrows(MismatchedInputException.class, () -> mapper.readValue(content, Request.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "\"x\"", "\"\"", "5", "true"})
  void shouldRejectValueThatIsNotList(String value) {
    String content = "{\"changes\":" + value + "}";

    assertThrows(MismatchedInputException.class, () -> mapper.readValue(content, Request.class));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 300})
  void shouldRejectUnclosedList(int count) {
    String content = "{\"changes\":[" + changesJson(count) + ",";

    JsonMappingException exception = assertThrows(JsonMappingException.class,
        () -> mapper.readValue(content, Request.class));

    assertThat("Unexpected cause.", exception.getCause(), instanceOf(JsonParseException.class));
  }

  private static String requestJson(String changesJson) {
    return "{\"changes\":[" + changesJson + "],\"name\":\"After\"}";
  }

  private static String changesJson(int count) {
    return IntStream.range(0, count)
        .mapToObj(index -> "{\"notes\":\"Change " + index + "\"}")
        .collect(Collectors.joining(","));
  }

  private static List<CctChangeDto> changes(int count) {
    return IntStream.range(0, count)
        .mapToObj(index -> CctChangeDto.builder().notes("Change " + index).build())
        .toList();
  }

  record Request(
      @JsonDeserialize(using = CctChangesDeserializer.class) List<CctChangeDto> changes,
      String name) {

  }
}
