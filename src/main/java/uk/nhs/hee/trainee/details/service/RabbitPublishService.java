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

import com.amazonaws.xray.spring.aop.XRayEnabled;
import io.sentry.Sentry;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.event.CojSignedEvent;
import uk.nhs.hee.trainee.details.model.ConditionsOfJoining;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;

/**
 * A service to publish events to Rabbit MQ.
 */
@Slf4j
@Service
@XRayEnabled
public class RabbitPublishService {

  private final String rabbitExchange;
  private final String routingKey;
  private final RabbitTemplate rabbitTemplate;
  protected ConcurrentHashMap<String, ProgrammeMembership> outstandingConfirms
      = new ConcurrentHashMap<>();

  RabbitPublishService(@Value("${application.rabbit.coj-signed.exchange}") String rabbitExchange,
      @Value("${application.rabbit.coj-signed.routing-key}") String routingKey,
      RabbitTemplate rabbitTemplate) {
    this.rabbitExchange = rabbitExchange;
    this.routingKey = routingKey;
    this.rabbitTemplate = rabbitTemplate;
  }

  /**
   * Set up the Rabbit confirmation callback handler.
   */
  @PostConstruct
  public void postConstruct() {
    rabbitTemplate.setConfirmCallback((correlation, ack, reason) -> {
      if (correlation == null) {
        return;
      }
      handleRabbitAcknowledgement(ack, correlation);
    });
  }

  public static void raiseRabbitSentryException(AmqpException e) {
    Sentry.captureException(e);
  }

  /**
   * Log successfully acked messages and raise sentry alerts for nacked messages.
   *
   * @param isAck           was the message acknowledged (success) or not (failure).
   * @param correlationData message correlation details.
   */
  void handleRabbitAcknowledgement(boolean isAck, CorrelationData correlationData) {
    final String id = correlationData.getId();
    outstandingConfirms.remove(id);
    if (!isAck) {
      log.info("Rabbit message for programme membership id '{}' got nack-ed", id);
      AmqpException e = new AmqpException("Rabbit message for programme membership id '"
          + id + "' got nack-ed");
      raiseRabbitSentryException(e);
    } else {
      //acked, message reached the broker successfully
      log.info("Rabbit message for programme membership id '{}' got acked", id);
    }
  }

  /**
   * Publish a CoJ signed event.
   *
   * @param programmeMembership The signed {@link ProgrammeMembership}.
   */
  public void publishCojSignedEvent(ProgrammeMembership programmeMembership) {
    log.info("Sending CoJ signed event for programme membership id '{}'",
        programmeMembership.getTisId());
    CorrelationData correlationData = new CorrelationData(programmeMembership.getTisId());

    ConditionsOfJoining conditionsOfJoining = programmeMembership.getConditionsOfJoining();

    CojSignedEvent event;
    UUID uuid = UUID.fromString(programmeMembership.getTisId());
    event = new CojSignedEvent(uuid.toString(), conditionsOfJoining);

    outstandingConfirms.putIfAbsent(programmeMembership.getTisId(), programmeMembership);
    try {
      rabbitTemplate.convertAndSend(rabbitExchange, routingKey, event, correlationData);
    } catch (AmqpException e) {
      log.info("Rabbit has gone away!");
      raiseRabbitSentryException(e);
    }
  }
}
