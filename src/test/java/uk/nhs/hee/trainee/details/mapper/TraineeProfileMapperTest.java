/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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

package uk.nhs.hee.trainee.details.mapper;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.trainee.details.dto.TraineeProfileDto;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.service.TrainingNumberGenerator;

@ExtendWith(MockitoExtension.class)
class TraineeProfileMapperTest {

  @InjectMocks
  private TraineeProfileMapperImpl mapper;

  @Spy
  PersonalDetailsMapper personalDetailsMapper = new PersonalDetailsMapperImpl();
  @Spy
  PlacementMapper placementMapper = new PlacementMapperImpl();
  @Spy
  ProgrammeMembershipMapper programmeMembershipMapper = new ProgrammeMembershipMapperImpl();

  @Mock
  private TrainingNumberGenerator trainingNumberGenerator;

  @Test
  void shouldGenerateTrainingNumbersWhenGettingTraineeProfile() {
    TraineeProfile entity = new TraineeProfile();

    TraineeProfileDto dto = mapper.toDto(entity);

    ArgumentCaptor<TraineeProfileDto> dtoCaptor = ArgumentCaptor.captor();
    verify(trainingNumberGenerator).populateTrainingNumbers(dtoCaptor.capture());

    assertThat("Unexpected dto.", dtoCaptor.getValue(), sameInstance(dto));
  }

  @Test
  void shouldIgnoreErrorsWhenGenerateTrainingNumbersForTraineeProfile() {
    doThrow(RuntimeException.class).when(trainingNumberGenerator).populateTrainingNumbers(any());

    TraineeProfile entity = new TraineeProfile();

    TraineeProfileDto dto = assertDoesNotThrow(() -> mapper.toDto(entity));

    ArgumentCaptor<TraineeProfileDto> dtoCaptor = ArgumentCaptor.captor();
    verify(trainingNumberGenerator).populateTrainingNumbers(dtoCaptor.capture());

    assertThat("Unexpected dto.", dtoCaptor.getValue(), sameInstance(dto));
  }
}
