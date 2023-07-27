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

import java.time.LocalDate;
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

class PersonalInfoListenerTest {

  private static final String TIS_ID = "40";
  private static final LocalDate DOB = LocalDate.now();
  private static final String GENDER = "Male";

  private PersonalInfoListener listener;

  private PersonalDetailsService service;

  @BeforeEach
  void setUp() {
    service = mock(PersonalDetailsService.class);
    listener = new PersonalInfoListener(service, new PersonalDetailsMapperImpl());
  }

  @Test
  void shouldThrowExceptionWhenTraineeNotFound() {
    PersonalDetailsDto dto = new PersonalDetailsDto();
    PersonalDetailsMetadata metadata = new PersonalDetailsMetadata(TIS_ID);
    PersonalDetailsEvent event = new PersonalDetailsEvent(dto, metadata);

    when(service.updatePersonalInfoByTisId(eq(TIS_ID), any())).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> listener.updatePersonalInfo(event));
  }

  @Test
  void shouldUpdatePersonalInfoWhenTraineeFound() {
    PersonalDetailsDto dto = new PersonalDetailsDto();
    dto.setDateOfBirth(DOB);
    dto.setGender(GENDER);

    PersonalDetailsMetadata metadata = new PersonalDetailsMetadata(TIS_ID);
    PersonalDetailsEvent event = new PersonalDetailsEvent(dto, metadata);

    ArgumentCaptor<PersonalDetails> entityCaptor = ArgumentCaptor.forClass(PersonalDetails.class);
    when(service.updatePersonalInfoByTisId(eq(TIS_ID), entityCaptor.capture())).then(
        inv -> Optional.of(inv.getArgument(1)));

    listener.updatePersonalInfo(event);

    PersonalDetails entity = entityCaptor.getValue();
    assertThat("Unexpected date of birth.", entity.getDateOfBirth(), is(DOB));
    assertThat("Unexpected gender.", entity.getGender(), is(GENDER));
  }
}
