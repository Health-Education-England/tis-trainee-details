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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class WholeNumberDeserializerTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      84    | 84
      84.0  | 84
      8.4e1 | 84
      "84"  | 84
      -3    | -3
      """)
  void shouldDeserializeWholeNumber(String json, int expected) throws IOException {
    Months months = mapper.readValue("{\"months\":" + json + "}", Months.class);

    assertThat("Unexpected months.", months.months(), is(expected));
  }

  @ParameterizedTest
  @ValueSource(strings = {"84.5", "0.1", "-2.5", "1e-1"})
  void shouldRejectNumberWithFractionalPart(String json) {
    String content = "{\"months\":" + json + "}";

    assertThrows(InvalidFormatException.class, () -> mapper.readValue(content, Months.class));
  }

  @Test
  void shouldDeserializeNull() throws IOException {
    Months months = mapper.readValue("{\"months\":null}", Months.class);

    assertThat("Unexpected months.", months.months(), nullValue());
  }

  record Months(@JsonDeserialize(using = WholeNumberDeserializer.class) Integer months) {

  }
}
