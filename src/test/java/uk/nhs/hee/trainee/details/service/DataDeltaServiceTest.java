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

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.nhs.hee.trainee.details.dto.DataDeltaDto;
import uk.nhs.hee.trainee.details.dto.FieldDeltaDto;
import uk.nhs.hee.trainee.details.model.Placement;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DataDeltaServiceTest {

  private static final String QUEUE_URL = "queue.url";

  private DataDeltaService service;
  private QueueMessagingTemplate messagingTemplate;

  @BeforeEach
  void setUp() {
    messagingTemplate = mock(QueueMessagingTemplate.class);
    service = new DataDeltaService(messagingTemplate, QUEUE_URL);
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

    List<FieldDeltaDto> changedFields = delta.getChangedFields();
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

    List<FieldDeltaDto> changedFields = delta.getChangedFields();
    assertThat("Unexpected delta size.", changedFields.size(), is(1));

    FieldDeltaDto changedField = changedFields.get(0);
    assertThat("Unexpected delta field.", changedField.getField(), is("site"));
    assertThat("Unexpected delta original value.", changedField.getOriginal(), is("site1"));
    assertThat("Unexpected delta modified value.", changedField.getModified(), is("site2"));
  }

  @Test
  void shouldPublishObjectDelta() {
    DataDeltaDto delta = new DataDeltaDto();
    delta.setDataClass(Placement.class);
    delta.setTisId("40");

    service.publishObjectDelta(delta);

    ArgumentCaptor<DataDeltaDto> eventCaptor = ArgumentCaptor.forClass(DataDeltaDto.class);
    verify(messagingTemplate).convertAndSend(eq(QUEUE_URL), eventCaptor.capture());

    DataDeltaDto event = eventCaptor.getValue();
    assertThat("Unexpected data class.", event.getDataClass(), is(Placement.class));
    assertThat("Unexpected tis ID.", event.getTisId(), is("40"));
  }
}
