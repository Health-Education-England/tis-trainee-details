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

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.nhs.hee.trainee.details.dto.CctChangeDto;

class CctChangesConverterTest {

  private final CctChangesConverter converter = new CctChangesConverter();

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 200})
  void shouldKeepAllChangesWhenWithinLimit(int count) {
    List<CctChangeDto> changes = changes(count);

    assertThat("Unexpected changes.", converter.convert(changes), is(changes));
  }

  @ParameterizedTest
  @ValueSource(ints = {201, 202, 1000})
  void shouldKeepOneChangePastLimitWhenOverLimit(int count) {
    List<CctChangeDto> changes = changes(count);

    assertThat("Unexpected changes.", converter.convert(changes), is(changes.subList(0, 201)));
  }

  private static List<CctChangeDto> changes(int count) {
    return IntStream.range(0, count)
        .mapToObj(index -> CctChangeDto.builder().notes("Change " + index).build())
        .toList();
  }
}
