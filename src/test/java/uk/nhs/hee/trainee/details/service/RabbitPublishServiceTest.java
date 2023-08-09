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

package uk.nhs.hee.trainee.details.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.nhs.hee.trainee.details.dto.enumeration.GoldGuideVersion;
import uk.nhs.hee.trainee.details.event.CojSignedEvent;
import uk.nhs.hee.trainee.details.model.ConditionsOfJoining;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;

@ExtendWith(MockitoExtension.class)
class RabbitPublishServiceTest {

  private static final String RABBIT_EXCHANGE = "rabbit.exchange";
  private static final String RABBIT_ROUTING_KEY = "routing.key";

  @InjectMocks
  RabbitPublishService rabbitPublishService;

  @Mock
  RabbitTemplate rabbitTemplate;

  @BeforeEach
  void setUp() {
    rabbitPublishService
        = new RabbitPublishService(RABBIT_EXCHANGE, RABBIT_ROUTING_KEY, rabbitTemplate);
  }

  @Test
  void shouldPublishCojSignedEvent() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId("123");
    Instant signedAt = Instant.now();
    ConditionsOfJoining conditionsOfJoining
        = new ConditionsOfJoining(signedAt, GoldGuideVersion.GG9);
    programmeMembership.setConditionsOfJoining(conditionsOfJoining);

    rabbitPublishService.publishCojSignedEvent(programmeMembership);

    ArgumentCaptor<CojSignedEvent> eventCaptor = ArgumentCaptor.forClass(
        CojSignedEvent.class);

    verify(rabbitTemplate).convertAndSend(any(), any(), eventCaptor.capture(),
        any(CorrelationData.class));

    CojSignedEvent event = eventCaptor.getValue();
    assertThat("Unexpected programme membership ID.",
        event.getProgrammeMembershipTisId(), is("123"));
    ConditionsOfJoining conditionsOfJoiningSent = event.getConditionsOfJoining();
    assertThat("Unexpected CoJ Signed At",
        conditionsOfJoiningSent.signedAt(), is(signedAt));
    assertThat("Unexpected CoJ Version",
        conditionsOfJoiningSent.version(), is(GoldGuideVersion.GG9));
  }

  @Test
  void shouldPublishCojSignedEventWithSimplifiedId() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId("123,456,7890");
    Instant signedAt = Instant.now();
    ConditionsOfJoining conditionsOfJoining
        = new ConditionsOfJoining(signedAt, GoldGuideVersion.GG9);
    programmeMembership.setConditionsOfJoining(conditionsOfJoining);

    rabbitPublishService.publishCojSignedEvent(programmeMembership);

    ArgumentCaptor<CojSignedEvent> eventCaptor = ArgumentCaptor.forClass(
        CojSignedEvent.class);

    verify(rabbitTemplate).convertAndSend(any(), any(), eventCaptor.capture(),
        any(CorrelationData.class));

    CojSignedEvent event = eventCaptor.getValue();
    assertThat("Unexpected programme membership ID.",
        event.getProgrammeMembershipTisId(), is("123"));
  }

  @Test
  void shouldPublishCojSignedEventWithUuid() {
    UUID uuid = UUID.randomUUID();
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId(uuid.toString());
    Instant signedAt = Instant.now();
    ConditionsOfJoining conditionsOfJoining
        = new ConditionsOfJoining(signedAt, GoldGuideVersion.GG9);
    programmeMembership.setConditionsOfJoining(conditionsOfJoining);

    rabbitPublishService.publishCojSignedEvent(programmeMembership);

    ArgumentCaptor<CojSignedEvent> eventCaptor = ArgumentCaptor.forClass(
        CojSignedEvent.class);

    verify(rabbitTemplate).convertAndSend(any(), any(), eventCaptor.capture(),
        any(CorrelationData.class));

    CojSignedEvent event = eventCaptor.getValue();
    assertThat("Unexpected programme membership ID.",
        event.getProgrammeMembershipTisId(), is(uuid.toString()));
  }

  @Test
  void shouldPublishCojSignedEventWithPmCorrelationId() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId("123,456,7890");
    Instant signedAt = Instant.now();
    ConditionsOfJoining conditionsOfJoining
        = new ConditionsOfJoining(signedAt, GoldGuideVersion.GG9);
    programmeMembership.setConditionsOfJoining(conditionsOfJoining);

    rabbitPublishService.publishCojSignedEvent(programmeMembership);

    ArgumentCaptor<CorrelationData> correlationCaptor = ArgumentCaptor.forClass(
        CorrelationData.class);

    verify(rabbitTemplate).convertAndSend(any(), any(), any(CojSignedEvent.class),
        correlationCaptor.capture());

    CorrelationData correlation = correlationCaptor.getValue();
    assertThat("Unexpected correlation ID.", correlation.getId(), is("123,456,7890"));
  }

  @Test
  void shouldRemoveAckedMessageFromOutstandingConfirms() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId("123");
    rabbitPublishService.outstandingConfirms.clear();
    rabbitPublishService.outstandingConfirms.put(programmeMembership.getTisId(),
        programmeMembership);
    CorrelationData correlationData = new CorrelationData("123");

    rabbitPublishService.handleRabbitAcknowledgement(true, correlationData);

    assertThat("Unexpected outstanding confirms",
        rabbitPublishService.outstandingConfirms.size(), is(0));
  }

  @Test
  void shouldNotRemoveNackedMessageFromOutstandingConfirms() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId("123");
    rabbitPublishService.outstandingConfirms.clear();
    rabbitPublishService.outstandingConfirms.put(programmeMembership.getTisId(),
        programmeMembership);
    CorrelationData correlationData = new CorrelationData("123");

    rabbitPublishService.handleRabbitAcknowledgement(false, correlationData);

    assertThat("Unexpected outstanding confirms",
        rabbitPublishService.outstandingConfirms.size(), is(1));
  }

  @Test
  void shouldSendNackedMessageToSentry() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId("123");
    CorrelationData correlationData = new CorrelationData("123");

    rabbitPublishService.handleRabbitAcknowledgement(false, correlationData);

    //TODO: verify Sentry sent
  }

  @Test
  void shouldSetupRabitConfirmCallback() {

    rabbitPublishService.postConstruct();

    verify(rabbitTemplate).setConfirmCallback(any());
  }
}
