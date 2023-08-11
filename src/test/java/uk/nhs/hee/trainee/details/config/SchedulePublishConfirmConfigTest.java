/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
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

package uk.nhs.hee.trainee.details.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class SchedulePublishConfirmConfigTest {

  @Mock
  RabbitTemplate rabbitTemplateMock;

  private SchedulePublishConfirmConfig schedulePublishConfirmConfig;

  @BeforeEach
  void setUp() {
    rabbitTemplateMock = mock(RabbitTemplate.class);
    schedulePublishConfirmConfig = new SchedulePublishConfirmConfig(rabbitTemplateMock, 100L);
  }

  @Test
  void shouldRaiseAlertForUnconfirmedMessage() {
    int unconfirmedMessageCount;
    CorrelationData unconfirmedCorrelationData
        = new CorrelationData("123");
    Collection<CorrelationData> unconfirmed = Collections.singletonList(unconfirmedCorrelationData);

    when(rabbitTemplateMock.getUnconfirmed(anyLong())).thenReturn(unconfirmed);

    unconfirmedMessageCount = schedulePublishConfirmConfig.scheduleUnconfirmedRepublishTask();

    assertThat("Unexpected unconfirmed message count", unconfirmedMessageCount, is(1));
  }

  @Test
  void shouldNotRaiseAlertIfNoUnconfirmedMessage() {
    int unconfirmedMessageCount;

    when(rabbitTemplateMock.getUnconfirmed(anyLong())).thenReturn(null);

    unconfirmedMessageCount = schedulePublishConfirmConfig.scheduleUnconfirmedRepublishTask();

    assertThat("Unexpected unconfirmed message count", unconfirmedMessageCount, is(0));
  }
}
