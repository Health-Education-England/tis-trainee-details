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
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST2;

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
import uk.nhs.hee.trainee.details.dto.CctTrainingTimeDto;

class CctTrainingTimeDeserializerTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 20})
  void shouldKeepAllEntriesWhenWithinLimit(int count) throws IOException {
    Request request = mapper.readValue(requestJson(count), Request.class);

    assertThat("Unexpected entries.", request.entries(), is(entries(count)));
  }

  @ParameterizedTest
  @ValueSource(ints = {21, 22, 1000})
  void shouldKeepOneEntryPastLimitWhenOverLimit(int count) throws IOException {
    Request request = mapper.readValue(requestJson(count), Request.class);

    assertThat("Unexpected entries.", request.entries(), is(entries(21)));
  }

  @Test
  void shouldRejectUnreadableEntryWithinLimit() {
    String content = "{\"entries\":[{\"grade\":\"ST99\"}]}";

    assertThrows(MismatchedInputException.class, () -> mapper.readValue(content, Request.class));
  }

  private static String requestJson(int count) {
    return IntStream.range(0, count)
        .mapToObj(index -> "{\"grade\":\"ST2\",\"months\":" + (index + 1) + ",\"notes\":\"why\"}")
        .collect(Collectors.joining(",", "{\"entries\":[", "]}"));
  }

  private static List<CctTrainingTimeDto> entries(int count) {
    return IntStream.range(0, count)
        .mapToObj(index -> CctTrainingTimeDto.builder()
            .grade(ST2)
            .months(index + 1)
            .notes("why")
            .build())
        .toList();
  }

  record Request(
      @JsonDeserialize(using = CctTrainingTimeDeserializer.class)
      List<CctTrainingTimeDto> entries) {

  }
}
