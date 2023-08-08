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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.event.CojSignedEvent;
import uk.nhs.hee.trainee.details.model.ConditionsOfJoining;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.RetryCorrelationData;

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
  protected ConcurrentHashMap<String, ProgrammeMembership> outstandingConfirms = new ConcurrentHashMap<>();
  protected ConcurrentLinkedQueue<RetryCorrelationData> negativeAckedMessages = new ConcurrentLinkedQueue<>();

  RabbitPublishService(@Value("${application.rabbit.coj-signed.exchange}") String rabbitExchange,
                       @Value("${application.rabbit.coj-signed.routing-key}") String routingKey,
                       RabbitTemplate rabbitTemplate) {
    this.rabbitExchange = rabbitExchange;
    this.routingKey = routingKey;
    this.rabbitTemplate = rabbitTemplate;
  }

  @PostConstruct
  public void postConstruct() {
    rabbitTemplate.setConfirmCallback((correlation, ack, reason) -> {

      if (correlation == null) {
        return;
      }

      RetryCorrelationData retryCorrelationData = (RetryCorrelationData) correlation;
      handleRabbitAcknowledgement(ack, retryCorrelationData);
    });
  }

  /**
   * Manage the collections of nacked and unconfirmed messages based on Rabbit message
   * acknowledgements.
   *
   * @param isAck                was the message acknowledged (success) or not (failure).
   * @param retryCorrelationData message correlation details to match to cached entries.
   */
  void handleRabbitAcknowledgement(boolean isAck, RetryCorrelationData retryCorrelationData) {
    final String id = retryCorrelationData.getId();
    if (!isAck) {
      log.info("Rabbit message for programme membership id '{}' got nack-ed, " +
          "saving to nack-ed message queue for retry", id);
      negativeAckedMessages.add(new RetryCorrelationData(id,
          retryCorrelationData.getRetryCount() + 1));
    } else {
      //acked, message reached the broker successfully
      cleanOutstandingConfirm(id);
      log.info("Rabbit message for programme membership id '{}' got acked", id);
    }
  }


  /**
   * Publish a CoJ signed event.
   *
   * @param programmeMembership The signed {@link ProgrammeMembership}.
   */
  public void publishCojSignedEvent(ProgrammeMembership programmeMembership, int retryCount) {
    log.info("Sending CoJ signed event for programme membership id '{}' [retry {}]",
        programmeMembership.getTisId(), retryCount);
    RetryCorrelationData correlationData = new RetryCorrelationData(programmeMembership.getTisId(), retryCount);

    ConditionsOfJoining conditionsOfJoining = programmeMembership.getConditionsOfJoining();

    // FIXME: remove deprecated (non-uuid) tisId
    CojSignedEvent event;
    try {
      UUID uuid = UUID.fromString(programmeMembership.getTisId());
      event = new CojSignedEvent(uuid.toString(), conditionsOfJoining);
    } catch (IllegalArgumentException e) {
      String firstPmId = programmeMembership.getTisId().split(",")[0];
      event = new CojSignedEvent(firstPmId, conditionsOfJoining);
    }

    outstandingConfirms.putIfAbsent(programmeMembership.getTisId(), programmeMembership);
    rabbitTemplate.convertAndSend(rabbitExchange, routingKey, event, correlationData);
  }

  /**
   * Remove a cached outstanding confirm message (if it has been successfully processed).
   *
   * @param id the message id.
   * @return the queued programme membership.
   */
  public ProgrammeMembership cleanOutstandingConfirm(String id) {
    // remove the data from outstandingConfirms
    // created as public method for usage in other retry schedulers as well.
    return outstandingConfirms.remove(id);
  }

  /**
   * Get the queue of nacked messages.
   *
   * @return the queue of nacked messages.
   */
  public Queue<RetryCorrelationData> getNegativeAckedMessages() {
    return negativeAckedMessages;
  }
}
