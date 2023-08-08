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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.RetryCorrelationData;
import uk.nhs.hee.trainee.details.service.RabbitPublishService;

class SchedulePublishRetryConfigTest {
  private static final int MAX_RETRIES = 3;

  @Mock
  RabbitPublishService rabbitPublishServiceMock;
  @Mock
  RabbitTemplate rabbitTemplateMock;

  private SchedulePublishRetryConfig schedulePublishRetryConfig;

  @BeforeEach
  void setUp() {
    rabbitTemplateMock = Mockito.mock(RabbitTemplate.class);
    rabbitPublishServiceMock = Mockito.mock(RabbitPublishService.class);
    schedulePublishRetryConfig = new SchedulePublishRetryConfig(rabbitTemplateMock,
        rabbitPublishServiceMock, MAX_RETRIES);
  }

  @Test
  void shouldRepublishNackedMessage() {
    int retryCount = 1;
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId("123");

    ConcurrentLinkedQueue<RetryCorrelationData> negativeAckedMessages
        = new ConcurrentLinkedQueue<>();
    RetryCorrelationData retryCorrelationData
        = new RetryCorrelationData(programmeMembership.getTisId(), retryCount);
    negativeAckedMessages.add(retryCorrelationData);
    when(rabbitPublishServiceMock.getNegativeAckedMessages())
        .thenReturn(negativeAckedMessages);
    when(rabbitPublishServiceMock.cleanOutstandingConfirm("123"))
        .thenReturn(programmeMembership);

    schedulePublishRetryConfig.scheduleNackedRepublishTask();

    verify(rabbitPublishServiceMock)
        .publishCojSignedEvent(programmeMembership, retryCount + 1);
  }

  @Test
  void shouldNotRepublishMessageWithTooManyRetries() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId("123");

    ConcurrentLinkedQueue<RetryCorrelationData> negativeAckedMessages
        = new ConcurrentLinkedQueue<>();
    RetryCorrelationData retryCorrelationData
        = new RetryCorrelationData(programmeMembership.getTisId(), MAX_RETRIES);
    negativeAckedMessages.add(retryCorrelationData);
    when(rabbitPublishServiceMock.getNegativeAckedMessages())
        .thenReturn(negativeAckedMessages);
    when(rabbitPublishServiceMock.cleanOutstandingConfirm("123"))
        .thenReturn(programmeMembership);

    schedulePublishRetryConfig.scheduleNackedRepublishTask();

    verify(rabbitPublishServiceMock, never()).publishCojSignedEvent(any(), anyInt());
  }

  @Test
  void shouldNotRepublishNackedMessageIfNotInQueue() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId("123");

    ConcurrentLinkedQueue<RetryCorrelationData> negativeAckedMessages
        = new ConcurrentLinkedQueue<>();
    RetryCorrelationData retryCorrelationData
        = new RetryCorrelationData(programmeMembership.getTisId(), MAX_RETRIES);
    negativeAckedMessages.add(retryCorrelationData);
    when(rabbitPublishServiceMock.getNegativeAckedMessages())
        .thenReturn(negativeAckedMessages);
    when(rabbitPublishServiceMock.cleanOutstandingConfirm("123"))
        .thenReturn(null);

    schedulePublishRetryConfig.scheduleNackedRepublishTask();

    verify(rabbitPublishServiceMock, never()).publishCojSignedEvent(any(), anyInt());
  }

  @Test
  void shouldRepublishUnconfirmedMessage() {
    int retryCount = 1;
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId("123");
    RetryCorrelationData unconfirmedCorrelationData
        = new RetryCorrelationData(programmeMembership.getTisId(), retryCount);
    Collection<CorrelationData> unconfirmed = Collections.singletonList(unconfirmedCorrelationData);

    when(rabbitTemplateMock.getUnconfirmed(anyLong())).thenReturn(unconfirmed);
    when(rabbitPublishServiceMock.cleanOutstandingConfirm("123"))
        .thenReturn(programmeMembership);

    schedulePublishRetryConfig.scheduleUnconfirmedRepublishTask();

    verify(rabbitPublishServiceMock)
        .publishCojSignedEvent(programmeMembership, retryCount + 1);
  }
}
