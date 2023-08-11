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

import io.sentry.Sentry;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configure handling of Rabbit messages not confirmed in a reasonable time.
 */
@Slf4j
@Configuration
@EnableScheduling
public class SchedulePublishConfirmConfig {

  private final RabbitTemplate rabbitTemplate;
  private final long maxMessageAgeUnconfirmed;

  /**
   * Initialise the SchedulePublishConfirmConfig object.
   *
   * @param rabbitTemplate the Rabbit template.
   */
  public SchedulePublishConfirmConfig(RabbitTemplate rabbitTemplate,
                                      @Value("${spring.rabbitmq.max-message-age-unconfirmed}")
                                      Long maxMessageAgeUnconfirmed) {
    this.rabbitTemplate = rabbitTemplate;
    this.maxMessageAgeUnconfirmed = maxMessageAgeUnconfirmed;
  }

  /**
   * Post Sentry alert for unconfirmed messages older than [maxMessageAgeUnconfirmed] seconds.
   *
   * @return the number of unconfirmed messages dropped.
   */
  @Scheduled(fixedDelay = 5000)
  public int scheduleUnconfirmedRepublishTask() {
    int unconfirmedMessageCount = 0;
    Collection<CorrelationData> unconfirmed
        = rabbitTemplate.getUnconfirmed(maxMessageAgeUnconfirmed);

    if (unconfirmed != null) {
      unconfirmedMessageCount = unconfirmed.size();
      for (CorrelationData correlationData : unconfirmed) {
        log.info("Abandoning unconfirmed Rabbit message id : '{}'", correlationData.getId());
        AmqpException e = new AmqpException("Rabbit message for programme membership id '"
            + correlationData.getId() + "' was never confirmed");
        Sentry.captureException(e);
      }
    }
    return unconfirmedMessageCount;
  }
}
