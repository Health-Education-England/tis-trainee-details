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

package uk.nhs.hee.trainee.details.event;

import static io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsDto;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsEvent;
import uk.nhs.hee.trainee.details.mapper.PersonalDetailsMapper;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.service.PersonalDetailsService;

@Slf4j
@Component
public class GdcDetailsListener {

  private final PersonalDetailsService service;
  private final PersonalDetailsMapper mapper;

  public GdcDetailsListener(PersonalDetailsService service, PersonalDetailsMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  /**
   * Update the GDC details for the trainee.
   *
   * @param event The sync event containing the GDC details.
   */
  @SqsListener(value = "${application.aws.sqs.gdc-details-update", deletionPolicy = ON_SUCCESS)
  void updateGdcDetails(PersonalDetailsEvent event) {
    String tisId = event.metadata().tisId();
    log.info("Update GDC details of trainee with TIS ID {}", tisId);

    PersonalDetailsDto dto = event.data();
    PersonalDetails entity = mapper.toEntity(dto);
    Optional<PersonalDetails> optionalEntity = service.updateGdcDetailsByTisId(tisId, entity);

    if (optionalEntity.isEmpty()) {
      throw new IllegalArgumentException("Trainee not found.");
    }
  }
}
