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

import java.util.Collection;
import java.util.Queue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.RetryCorrelationData;
import uk.nhs.hee.trainee.details.service.RabbitPublishService;

/**
 * Configure Rabbit message republishing for not-acknowledged and unconfirmed errors.
 */
@Slf4j
@Configuration
@EnableScheduling
public class SchedulePublishRetryConfig {

  private final RabbitTemplate rabbitTemplate;
  private final RabbitPublishService rabbitPublishService;
  private final int maxRetries;

  /**
   * Initialise the SchedulePublishRetryConfig object.
   *
   * @param rabbitTemplate       the Rabbit template.
   * @param rabbitPublishService the Rabbit publishing service.
   * @param maxRetries           the maximum number of message retries.
   */
  public SchedulePublishRetryConfig(RabbitTemplate rabbitTemplate,
                                    RabbitPublishService rabbitPublishService,
                                    @Value("${spring.rabbitmq.max-retries}") int maxRetries) {
    this.rabbitTemplate = rabbitTemplate;
    this.rabbitPublishService = rabbitPublishService;
    this.maxRetries = maxRetries;
  }

  /**
   * Republish not-acknowledged messages (up to a maximum number of retries).
   */
  @Scheduled(fixedDelay = 10000)
  public void scheduleNackedRepublishTask() {
    Queue<RetryCorrelationData> queue = rabbitPublishService.getNegativeAckedMessages();

    // try re-publish with batch size of 100 each time
    for (int i = 0; i < 100; i++) {
      if (queue.isEmpty()) {
        break;
      }
      RetryCorrelationData retryCorrelationData = queue.remove();

      log.info("Retry nack-ed Rabbit message id : '{}'", retryCorrelationData.getId());
      checkRetryCountAndRepublish(retryCorrelationData.getRetryCount(),
          retryCorrelationData.getId());
    }
  }

  /**
   * Republish unconfirmed messages (up to a maximum number of retries).
   */
  @Scheduled(fixedDelay = 5000)
  public void scheduleUnconfirmedRepublishTask() {
    Collection<CorrelationData> unconfirmed = rabbitTemplate.getUnconfirmed(10000);

    if (unconfirmed != null) {
      for (CorrelationData correlationData : unconfirmed) {
        RetryCorrelationData retryCorrelationData = (RetryCorrelationData) correlationData;

        log.info("Retry unconfirmed Rabbit message id : '{}'", retryCorrelationData.getId());
        checkRetryCountAndRepublish(retryCorrelationData.getRetryCount(), correlationData.getId());
      }
    }
  }

  /**
   * Perform the message republishing.
   *
   * @param retryCount the current retry count for the message.
   * @param id         the message id.
   */
  void checkRetryCountAndRepublish(int retryCount, String id) {
    ProgrammeMembership pm = rabbitPublishService.cleanOutstandingConfirm(id);

    if (pm == null) {
      log.warn("Failed to retrieve programme membership from outstandingConfirms queue "
          + "with id : '{}'", id);
      return;
    }

    // have limit of retry count of 100, to prevent infinite retry
    if (retryCount < maxRetries) {
      rabbitPublishService.publishCojSignedEvent(pm, retryCount + 1);
    } else {
      log.warn("Retry limit reached, failed to send message for programme membership id : '{}'",
          pm.getTisId());
    }
  }
}
