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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.nhs.hee.trainee.details.event.ProfileCreateEvent;
import uk.nhs.hee.trainee.details.model.TraineeProfile;

class EventPublishServiceTest {

  private static final String QUEUE_URL = "queue.url";
  private EventPublishService eventPublishService;
  private SqsTemplate messagingTemplate;

  @BeforeEach
  void setUp() {
    messagingTemplate = mock(SqsTemplate.class);
    eventPublishService = new EventPublishService(messagingTemplate, QUEUE_URL);
  }

  @Test
  void shouldPublishProfileCreateEvent() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setTraineeTisId("10");

    eventPublishService.publishProfileCreateEvent(traineeProfile);

    ArgumentCaptor<ProfileCreateEvent> eventCaptor = ArgumentCaptor.forClass(
        ProfileCreateEvent.class);
    verify(messagingTemplate).send(eq(QUEUE_URL), eventCaptor.capture());

    ProfileCreateEvent event = eventCaptor.getValue();
    assertThat("Unexpected trainee ID.", event.getTraineeTisId(), is("10"));
  }
}
