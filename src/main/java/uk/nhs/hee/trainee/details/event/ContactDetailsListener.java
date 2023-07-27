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

import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsDto;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsEvent;
import uk.nhs.hee.trainee.details.mapper.PersonalDetailsMapper;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.service.PersonalDetailsService;

@Slf4j
@Component
public class ContactDetailsListener {

  private final PersonalDetailsService service;
  private final PersonalDetailsMapper mapper;

  public ContactDetailsListener(PersonalDetailsService service, PersonalDetailsMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  /**
   * Update the contact details for the trainee.
   *
   * @param event The sync event containing the contact details.
   */
  @SqsListener(value = "${application.aws.sqs.contact-details-update", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
  void updateContactDetails(PersonalDetailsEvent event) {
    String tisId = event.metadata().tisId();
    log.info("Update contact details of trainee with TIS ID {}", tisId);

    PersonalDetailsDto dto = event.data();
    PersonalDetails entity = mapper.toEntity(dto);
    Optional<PersonalDetails> optionalEntity = service.updateContactDetailsByTisId(tisId, entity);
    optionalEntity.orElseThrow(() -> new IllegalArgumentException("Trainee not found."));
  }
}
