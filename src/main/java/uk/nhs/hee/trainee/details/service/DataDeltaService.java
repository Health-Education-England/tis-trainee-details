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

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import uk.nhs.hee.trainee.details.dto.DataDeltaDto;
import uk.nhs.hee.trainee.details.dto.FieldDeltaDto;
import uk.nhs.hee.trainee.details.model.TraineeProfile;

import java.util.List;
import java.util.Objects;

/**
 * A service to find the delta between two data objects of the same type.
 */
@Slf4j
@Service
public class DataDeltaService {

  private final QueueMessagingTemplate messagingTemplate;
  private final String queueUrl;

  DataDeltaService(QueueMessagingTemplate messagingTemplate,
                   @Value("${application.aws.sqs.delta}") String queueUrl) {
    this.messagingTemplate = messagingTemplate;
    this.queueUrl = queueUrl;
  }

  public <T> DataDeltaDto getObjectDelta(TraineeProfile traineeProfile, T original, T latest, Class<T> objectClass) {
    DataDeltaDto delta = new DataDeltaDto();
    delta.setDataClass(objectClass.getSimpleName());
    delta.setNotificationEmail(traineeProfile.getPersonalDetails().getEmail());
    List<FieldDeltaDto> changedFields = delta.getChangedFields();

    ReflectionUtils.doWithFields(objectClass, field -> {
      field.setAccessible(true);
      Object originalField = field.get(original);
      Object latestField = field.get(latest);

      if (field.getName().equals("tisId")) {
        delta.setTisId(originalField.toString());
      } else if (!Objects.equals(originalField, latestField)) {
        FieldDeltaDto changedField = new FieldDeltaDto(field.getName(), originalField, latestField);
        changedFields.add(changedField);
      }
    });

    return delta;
  }

  public void publishObjectDelta(DataDeltaDto objectDelta) {
    log.info("Sending object delta for {} id '{}'", objectDelta.getDataClass(), objectDelta.getTisId());
    messagingTemplate.convertAndSend(queueUrl, objectDelta);
  }
}
