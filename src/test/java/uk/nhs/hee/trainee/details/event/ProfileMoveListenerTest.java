/*
 * The MIT License (MIT)
 *
 * Copyright 2025 Crown Copyright (Health Education England)
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.trainee.details.dto.ProfileMoveEvent;
import uk.nhs.hee.trainee.details.service.CctService;

/**
 * Test for {@link ProfileMoveListener}.
 */
class ProfileMoveListenerTest {
  private static final String FROM_TRAINEE_ID = UUID.randomUUID().toString();
  private static final String TO_TRAINEE_ID = UUID.randomUUID().toString();

  private ProfileMoveListener listener;
  private CctService service;

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    service = mock(CctService.class);
    listener = new ProfileMoveListener(service);
    mapper = JsonMapper.builder()
        .findAndAddModules()
        .build();
  }

  @Test
  void shouldMoveProfileCctCalculations() throws JsonProcessingException {
    String eventJson = """
        {
          "fromTraineeId": "%s",
          "toTraineeId": "%s"
        }""".formatted(FROM_TRAINEE_ID, TO_TRAINEE_ID);
    ProfileMoveEvent event = mapper.readValue(eventJson, ProfileMoveEvent.class);

    listener.handleProfileMove(event);

    verify(service).moveCalculations(FROM_TRAINEE_ID, TO_TRAINEE_ID);
  }
}