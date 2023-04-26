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
import com.rabbitmq.client.AMQP;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.event.CojSignedEvent;
import uk.nhs.hee.trainee.details.event.ProfileCreateEvent;
import uk.nhs.hee.trainee.details.model.ConditionsOfJoining;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;

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

  RabbitPublishService(@Value("${application.rabbit.coj-signed-exchange}") String rabbitExchange,
                       @Value("${application.rabbit.coj-signed-routing-key}") String routingKey,
                       RabbitTemplate rabbitTemplate) {
    this.rabbitExchange = rabbitExchange;
    this.routingKey = routingKey;
    this.rabbitTemplate = rabbitTemplate;
  }

  /**
   * Publish a CoJ signed event.
   *
   * @param programmeMembership The signed {@link ProgrammeMembership}.
   */
  public void publishCojSignedEvent(ProgrammeMembership programmeMembership) {
    log.info("Sending CoJ signed event for programme membership id '{}'",
        programmeMembership.getTisId());

    ConditionsOfJoining conditionsOfJoining = programmeMembership.getConditionsOfJoining();

    CojSignedEvent event = new CojSignedEvent(programmeMembership.getTisId(), conditionsOfJoining);
    rabbitTemplate.convertAndSend(rabbitExchange, routingKey, event);
  }
}