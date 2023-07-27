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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsDto;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsEvent;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsEvent.PersonalDetailsMetadata;
import uk.nhs.hee.trainee.details.mapper.PersonalDetailsMapperImpl;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.service.PersonalDetailsService;

class GmcDetailsListenerTest {

  private static final String TIS_ID = "40";
  private static final String GMC_NUMBER = "140";
  private static final String GMC_STATUS = "Expired";

  private GmcDetailsListener listener;

  private PersonalDetailsService service;

  @BeforeEach
  void setUp() {
    service = mock(PersonalDetailsService.class);
    listener = new GmcDetailsListener(service, new PersonalDetailsMapperImpl());
  }

  @Test
  void shouldThrowExceptionWhenTraineeNotFound() {
    PersonalDetailsDto dto = new PersonalDetailsDto();
    PersonalDetailsMetadata metadata = new PersonalDetailsMetadata(TIS_ID);
    PersonalDetailsEvent event = new PersonalDetailsEvent(dto, metadata);

    when(service.updateGmcDetailsByTisId(eq(TIS_ID), any())).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> listener.updateGmcDetails(event));
  }

  @Test
  void shouldUpdateGmcDetailsWhenTraineeFound() {
    PersonalDetailsDto dto = new PersonalDetailsDto();
    dto.setGmcNumber(GMC_NUMBER);
    dto.setGmcStatus(GMC_STATUS);

    PersonalDetailsMetadata metadata = new PersonalDetailsMetadata(TIS_ID);
    PersonalDetailsEvent event = new PersonalDetailsEvent(dto, metadata);

    ArgumentCaptor<PersonalDetails> entityCaptor = ArgumentCaptor.forClass(PersonalDetails.class);
    when(service.updateGmcDetailsByTisId(eq(TIS_ID), entityCaptor.capture())).then(
        inv -> Optional.of(inv.getArgument(1)));

    listener.updateGmcDetails(event);

    PersonalDetails entity = entityCaptor.getValue();
    assertThat("Unexpected GMC number.", entity.getGmcNumber(), is(GMC_NUMBER));
    assertThat("Unexpected GMC status.", entity.getGmcStatus(), is(GMC_STATUS));
  }
}
