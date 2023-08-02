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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsDto;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsUpdateEvent;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsUpdateEvent.Update;
import uk.nhs.hee.trainee.details.mapper.PersonalDetailsMapperImpl;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.service.PersonalDetailsService;

class BasicDetailsListenerTest {

  private static final String TIS_ID = "40";
  private static final String PUBLIC_HEALTH_NUMBER = "140";

  private BasicDetailsListener listener;

  private PersonalDetailsService service;

  @BeforeEach
  void setUp() {
    service = mock(PersonalDetailsService.class);
    listener = new BasicDetailsListener(service, new PersonalDetailsMapperImpl());
  }

  @Test
  void shouldUpsertBasicDetails() {
    PersonalDetailsDto dto = new PersonalDetailsDto();
    dto.setPublicHealthNumber(PUBLIC_HEALTH_NUMBER);

    Update update = new Update(dto);
    PersonalDetailsUpdateEvent event = new PersonalDetailsUpdateEvent(TIS_ID, update);

    listener.updateBasicDetails(event);

    ArgumentCaptor<PersonalDetails> entityCaptor = ArgumentCaptor.forClass(PersonalDetails.class);
    verify(service).createProfileOrUpdateBasicDetailsByTisId(eq(TIS_ID), entityCaptor.capture());

    PersonalDetails entity = entityCaptor.getValue();
    assertThat("Unexpected Public Health Number.", entity.getPublicHealthNumber(),
        is(PUBLIC_HEALTH_NUMBER));
  }
}
