/*
 * The MIT License (MIT)
 *
 * Copyright 2022 Crown Copyright (Health Education England)
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

package uk.nhs.hee.trainee.details.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.trainee.details.dto.DataDeltaDto;
import uk.nhs.hee.trainee.details.model.Placement;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class DataDeltaServiceTest {

  private DataDeltaService service;

  @BeforeEach
  void setUp() {
    service = new DataDeltaService();
  }

  @Test
  void shouldNotFindTisIdDelta() {
    Placement original = new Placement();
    original.setTisId("40");

    Placement latest = new Placement();
    latest.setTisId("140");

    DataDeltaDto delta = service.getObjectDelta(original, latest, Placement.class);

    assertThat("Unexpected delta class.", delta.getDataClass(), is(Placement.class));
    assertThat("Unexpected delta id.", delta.getTisId(), is("40"));

    Map<String, SimpleEntry<Object, Object>> changedFields = delta.getChangedFields();
    assertThat("Unexpected delta size.", changedFields.size(), is(0));
  }

  @Test
  void shouldFindNonTisIdDelta() {
    Placement original = new Placement();
    original.setTisId("40");
    original.setSite("site1");

    Placement latest = new Placement();
    latest.setTisId("40");
    latest.setSite("site2");

    DataDeltaDto delta = service.getObjectDelta(original, latest, Placement.class);

    assertThat("Unexpected delta class.", delta.getDataClass(), is(Placement.class));
    assertThat("Unexpected delta id.", delta.getTisId(), is("40"));

    Map<String, SimpleEntry<Object, Object>> changedFields = delta.getChangedFields();
    assertThat("Unexpected delta size.", changedFields.size(), is(1));
    assertThat("Unexpected delta fields.", changedFields.keySet(), hasItem("site"));

    SimpleEntry<Object, Object> fieldChange = changedFields.get("site");
    assertThat("Unexpected delta original value.", fieldChange.getKey(), is("site1"));
    assertThat("Unexpected delta latest value.", fieldChange.getValue(), is("site2"));
  }
}
