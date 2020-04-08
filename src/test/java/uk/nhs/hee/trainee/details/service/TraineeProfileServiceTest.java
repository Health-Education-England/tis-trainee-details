/*
 * The MIT License (MIT)
 *
 * Copyright 2020 Crown Copyright (Health Education England)
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

import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.trainee.details.dto.TraineeProfileDto;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;
import uk.nhs.hee.trainee.details.service.impl.TraineeProfileServiceImpl;

@ExtendWith(MockitoExtension.class)
public class TraineeProfileServiceTest {

  private static final String DEFAULT_ID_1 = "5e00c7942749a84794644f83";
  private static final String DEFAULT_TIS_ID_1 = "123";

  @InjectMocks
  private TraineeProfileServiceImpl traineeProfileServiceImpl;

  @Mock
  private TraineeProfileRepository traineeProfileRepositoryMock;

  private TraineeProfile traineeProfile;
  private TraineeProfileDto traineeProfileDto;

  @BeforeEach
  public void initData() {
    traineeProfile = new TraineeProfile();
    traineeProfile.setId(DEFAULT_ID_1);
    traineeProfile.setTraineeTisId(DEFAULT_TIS_ID_1);
  }

  @Test
  public void getTraineeProfileShouldReturnTraineeProfile() {
    when(traineeProfileRepositoryMock.findById(DEFAULT_ID_1))
        .thenReturn(Optional.of(traineeProfile));
    TraineeProfile returnedTraineeProfile = traineeProfileServiceImpl.getTraineeProfile(DEFAULT_ID_1);
    Assert.assertEquals(returnedTraineeProfile, traineeProfile);
  }

  @Test
  public void getTraineeProfileByTraineeTisIdShouldReturnTraineeProfile() {
    when(traineeProfileRepositoryMock.findByTraineeTisId(DEFAULT_TIS_ID_1))
        .thenReturn(traineeProfile);
    TraineeProfile returnedTraineeProfile = traineeProfileServiceImpl.getTraineeProfileByTraineeTisId(DEFAULT_TIS_ID_1);
    Assert.assertEquals(returnedTraineeProfile, traineeProfile);
  }
}
