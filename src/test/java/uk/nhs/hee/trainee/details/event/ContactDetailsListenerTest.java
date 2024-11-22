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
import uk.nhs.hee.trainee.details.dto.PersonalDetailsUpdateEvent;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsUpdateEvent.Update;
import uk.nhs.hee.trainee.details.mapper.PersonalDetailsMapperImpl;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.service.PersonalDetailsService;

class ContactDetailsListenerTest {

  private static final String TIS_ID = "40";
  private static final String FORENAMES = "Anthony";
  private static final String SURNAME = "Gilliam";
  private static final String EMAIL = "anthony.gilliam@tis.nhs.uk";

  private ContactDetailsListener listener;

  private PersonalDetailsService service;

  @BeforeEach
  void setUp() {
    service = mock(PersonalDetailsService.class);
    listener = new ContactDetailsListener(service, new PersonalDetailsMapperImpl());
  }

  @Test
  void shouldThrowExceptionWhenTraineeNotFound() {
    PersonalDetailsDto dto = new PersonalDetailsDto();
    Update update = new Update(dto);
    PersonalDetailsUpdateEvent event = new PersonalDetailsUpdateEvent(TIS_ID, update);

    when(service.updateContactDetailsByTisId(eq(TIS_ID), any())).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> listener.updateContactDetails(event));
  }

  @Test
  void shouldUpdateContactDetailsWhenTraineeFound() {
    PersonalDetailsDto dto = new PersonalDetailsDto();
    dto.setForenames(FORENAMES);
    dto.setSurname(SURNAME);
    dto.setEmail(EMAIL);

    Update update = new Update(dto);
    PersonalDetailsUpdateEvent event = new PersonalDetailsUpdateEvent(TIS_ID, update);

    ArgumentCaptor<PersonalDetails> entityCaptor = ArgumentCaptor.forClass(PersonalDetails.class);
    when(service.updateContactDetailsByTisId(eq(TIS_ID), entityCaptor.capture())).then(
        inv -> Optional.of(inv.getArgument(1)));

    listener.updateContactDetails(event);

    PersonalDetails entity = entityCaptor.getValue();
    assertThat("Unexpected forenames.", entity.getForenames(), is(FORENAMES));
    assertThat("Unexpected surname.", entity.getSurname(), is(SURNAME));
    assertThat("Unexpected contact.", entity.getEmail(), is(EMAIL));
  }
}
