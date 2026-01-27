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

import com.amazonaws.xray.spring.aop.XRayEnabled;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.dto.ContactDetailsUpdateDto;
import uk.nhs.hee.trainee.details.dto.GmcDetailsDto;
import uk.nhs.hee.trainee.details.event.CojSignedEvent;
import uk.nhs.hee.trainee.details.event.EmailDetailsProvidedEvent;
import uk.nhs.hee.trainee.details.event.GmcDetailsProvidedEvent;
import uk.nhs.hee.trainee.details.event.ProfileCreateEvent;
import uk.nhs.hee.trainee.details.model.ConditionsOfJoining;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;

/**
 * A service to publish events to SQS.
 */
@Slf4j
@Service
@XRayEnabled
public class EventPublishService {

  private final SnsTemplate snsTemplate;
  private final String cojSignedTopic;
  private final String contactDetailsProvidedTopic;
  private final String gmcDetailsProvidedTopic;

  private final SqsTemplate sqsTemplate;
  private final String eventQueueUrl;

  EventPublishService(SnsTemplate snsTemplate, SqsTemplate sqsTemplate,
      @Value("${application.aws.sns.coj-signed}") String cojSignedTopic,
      @Value("${application.aws.sns.contact-details-provided}") String contactDetailsProvidedTopic,
      @Value("${application.aws.sns.gmc-details-provided}") String gmcDetailsProvidedTopic,
      @Value("${application.aws.sqs.event}") String eventQueueUrl) {
    this.snsTemplate = snsTemplate;
    this.sqsTemplate = sqsTemplate;
    this.cojSignedTopic = cojSignedTopic;
    this.contactDetailsProvidedTopic = contactDetailsProvidedTopic;
    this.gmcDetailsProvidedTopic = gmcDetailsProvidedTopic;
    this.eventQueueUrl = eventQueueUrl;
  }

  /**
   * Publish a CoJ signed event.
   *
   * @param programmeMembership The signed {@link ProgrammeMembership}.
   */
  public void publishCojSignedEvent(ProgrammeMembership programmeMembership) {
    String programmeMembershipId = programmeMembership.getTisId();
    log.info("Sending CoJ signed event for programme membership id '{}'", programmeMembershipId);

    ConditionsOfJoining conditionsOfJoining = programmeMembership.getConditionsOfJoining();
    CojSignedEvent event = new CojSignedEvent(programmeMembershipId, conditionsOfJoining);

    SnsNotification<CojSignedEvent> notification = SnsNotification.builder(event)
        .groupId(programmeMembershipId)
        .build();
    snsTemplate.sendNotification(cojSignedTopic, notification);
  }

  /**
   * Publish a GMC Details provided event.
   *
   * @param traineeId  The ID of the trainee being updated.
   * @param gmcDetails The provided GMC details.
   */
  public void publishGmcDetailsProvidedEvent(String traineeId, GmcDetailsDto gmcDetails) {
    log.info("Sending GMC Details update event for trainee id '{}'", traineeId);

    GmcDetailsProvidedEvent event = new GmcDetailsProvidedEvent(traineeId, gmcDetails);
    SnsNotification<GmcDetailsProvidedEvent> notification = SnsNotification.builder(event)
        .groupId(traineeId)
        .build();

    snsTemplate.sendNotification(gmcDetailsProvidedTopic, notification);
  }

  /**
   * Publish a Email update request event.
   *
   * @param traineeId    The ID of the trainee being updated.
   * @param emailDetails The provided updated email details.
   */
  public void publishEmailDetailsProvidedEvent(String traineeId,
      ContactDetailsUpdateDto emailDetails) {
    log.info("Sending email update event for trainee id '{}'", traineeId);

    EmailDetailsProvidedEvent event = new EmailDetailsProvidedEvent(traineeId, emailDetails);
    SnsNotification<EmailDetailsProvidedEvent> notification = SnsNotification.builder(event)
        .groupId(traineeId)
        .build();

    snsTemplate.sendNotification(contactDetailsProvidedTopic, notification);
  }

  /**
   * Publish a Profile Create event.
   *
   * @param profile The created {@link TraineeProfile}.
   */
  public void publishProfileCreateEvent(TraineeProfile profile) {
    log.info("Sending profile creation event for trainee id '{}'", profile.getTraineeTisId());

    ProfileCreateEvent event = new ProfileCreateEvent(profile.getTraineeTisId());
    sqsTemplate.send(eventQueueUrl, event);
  }
}
