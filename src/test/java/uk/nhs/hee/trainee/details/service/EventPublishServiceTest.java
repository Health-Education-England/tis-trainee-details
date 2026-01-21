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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import uk.nhs.hee.trainee.details.dto.EmailUpdateDto;
import uk.nhs.hee.trainee.details.dto.GmcDetailsDto;
import uk.nhs.hee.trainee.details.dto.enumeration.GoldGuideVersion;
import uk.nhs.hee.trainee.details.event.CojSignedEvent;
import uk.nhs.hee.trainee.details.event.EmailDetailsProvidedEvent;
import uk.nhs.hee.trainee.details.event.GmcDetailsProvidedEvent;
import uk.nhs.hee.trainee.details.event.ProfileCreateEvent;
import uk.nhs.hee.trainee.details.model.ConditionsOfJoining;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;

class EventPublishServiceTest {

  private static final String COJ_SIGNED_TOPIC = "coj-signed.topic.arn";
  private static final String EMAIL_DETAILS_PROVIDED_TOPIC = "email-details-provided.topic.arn";
  private static final String GMC_DETAILS_PROVIDED_TOPIC = "gmc-details-provided.topic.arn";
  private static final String QUEUE_URL = "queue.url";

  private EventPublishService eventPublishService;
  private SnsTemplate snsTemplate;
  private SqsTemplate sqsTemplate;

  @BeforeEach
  void setUp() {
    snsTemplate = mock(SnsTemplate.class);
    sqsTemplate = mock(SqsTemplate.class);
    eventPublishService = new EventPublishService(snsTemplate, sqsTemplate, COJ_SIGNED_TOPIC,
        EMAIL_DETAILS_PROVIDED_TOPIC, GMC_DETAILS_PROVIDED_TOPIC, QUEUE_URL);
  }

  @ParameterizedTest
  @EnumSource(GoldGuideVersion.class)
  void shouldPublishCojSignedEvent(GoldGuideVersion version) {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    String pmId = UUID.randomUUID().toString();
    programmeMembership.setTisId(pmId);

    Instant signed = Instant.now();
    ConditionsOfJoining coj = new ConditionsOfJoining(signed, version, null);
    programmeMembership.setConditionsOfJoining(coj);

    eventPublishService.publishCojSignedEvent(programmeMembership);

    ArgumentCaptor<SnsNotification<CojSignedEvent>> notificationCaptor = ArgumentCaptor.captor();
    verify(snsTemplate).sendNotification(eq(COJ_SIGNED_TOPIC), notificationCaptor.capture());

    SnsNotification<CojSignedEvent> notification = notificationCaptor.getValue();
    assertThat("Unexpected group ID.", notification.getGroupId(), is(pmId));
    assertThat("Unexpected dedupe ID.", notification.getDeduplicationId(), nullValue());

    CojSignedEvent event = notification.getPayload();
    assertThat("Unexpected PM ID.", event.getProgrammeMembershipTisId(), is(pmId));

    ConditionsOfJoining eventCoj = event.getConditionsOfJoining();
    assertThat("Unexpected signed timestamp.", eventCoj.signedAt(), is(signed));
    assertThat("Unexpected Gold Guide version.", eventCoj.version(), is(version));
    assertThat("Unexpected synced timestamp.", eventCoj.syncedAt(), nullValue());
  }

  @Test
  void shouldPublishGmcDetailsProvidedEvent() {
    String traineeId = "40";
    GmcDetailsDto gmcDetails = GmcDetailsDto.builder()
        .gmcNumber("1234567")
        .gmcStatus("Registered with Licence")
        .build();
    eventPublishService.publishGmcDetailsProvidedEvent(traineeId, gmcDetails);

    ArgumentCaptor<SnsNotification<GmcDetailsProvidedEvent>> notificationCaptor = ArgumentCaptor
        .captor();
    verify(snsTemplate).sendNotification(eq(GMC_DETAILS_PROVIDED_TOPIC),
        notificationCaptor.capture());

    SnsNotification<GmcDetailsProvidedEvent> notification = notificationCaptor.getValue();
    assertThat("Unexpected group ID.", notification.getGroupId(), is(traineeId));
    assertThat("Unexpected dedupe ID.", notification.getDeduplicationId(), nullValue());

    GmcDetailsProvidedEvent event = notification.getPayload();
    assertThat("Unexpected trainee ID.", event.traineeId(), is(traineeId));

    GmcDetailsDto eventGmcDetails = event.gmcDetails();
    assertThat("Unexpected GMC number.", eventGmcDetails.gmcNumber(), is("1234567"));
    assertThat("Unexpected GMC status.", eventGmcDetails.gmcStatus(),
        is("Registered with Licence"));
  }

  @Test
  void shouldPublishEmailDetailsProvidedEvent() {
    String traineeId = "40";
    String newEmail = "email@test.tis.nhs.net";
    EmailUpdateDto emailDetails = new EmailUpdateDto();
    emailDetails.setEmail(newEmail);
    eventPublishService.publishEmailDetailsProvidedEvent(traineeId, emailDetails);

    ArgumentCaptor<SnsNotification<EmailDetailsProvidedEvent>> notificationCaptor = ArgumentCaptor
        .captor();
    verify(snsTemplate).sendNotification(eq(EMAIL_DETAILS_PROVIDED_TOPIC),
        notificationCaptor.capture());

    SnsNotification<EmailDetailsProvidedEvent> notification = notificationCaptor.getValue();
    assertThat("Unexpected group ID.", notification.getGroupId(), is(traineeId));
    assertThat("Unexpected dedupe ID.", notification.getDeduplicationId(), nullValue());

    EmailDetailsProvidedEvent event = notification.getPayload();
    assertThat("Unexpected trainee ID.", event.traineeId(), is(traineeId));

    EmailUpdateDto eventEmailDetails = event.emailDetails();
    assertThat("Unexpected email address.", eventEmailDetails.getEmail(), is(newEmail));
  }

  @Test
  void shouldPublishProfileCreateEvent() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setTraineeTisId("10");

    eventPublishService.publishProfileCreateEvent(traineeProfile);

    ArgumentCaptor<ProfileCreateEvent> eventCaptor = ArgumentCaptor.captor();
    verify(sqsTemplate).send(eq(QUEUE_URL), eventCaptor.capture());

    ProfileCreateEvent event = eventCaptor.getValue();
    assertThat("Unexpected trainee ID.", event.getTraineeTisId(), is("10"));
  }
}
