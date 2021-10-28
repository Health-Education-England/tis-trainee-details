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

package uk.nhs.hee.trainee.details.repository;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import uk.nhs.hee.trainee.details.TestConfig;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.model.TraineeProfile;

@Disabled("Current requires a local DB instance, ignore until in-memory test DB is set up")
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfig.class)
class TraineeProfileRepositoryTest {

  private static final String EMAIL = "email@email.com";

  @Autowired
  private TraineeProfileRepository repository;

  private TraineeProfile traineeProfile;

  /**
   * Set up before each test.
   */
  @BeforeEach
  @Transactional
  public void setUp() {
    traineeProfile = new TraineeProfile();
    traineeProfile.setId("1");
    traineeProfile.setTraineeTisId("1111");

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setEmail(EMAIL);
    traineeProfile.setPersonalDetails(personalDetails);
    repository.save(traineeProfile);
  }

  @AfterEach
  @Transactional
  public void tearDown() {
    repository.delete(traineeProfile);
  }

  @Test
  @Transactional
  void findTraineeProfileById() {
    Optional<TraineeProfile> result = repository.findById("1");
    assertThat(result.isPresent(), is(true));
    TraineeProfile traineeProfile = result.get();
    assertThat(traineeProfile.getId(), is("1s"));
  }

  @Test
  @Transactional
  void shouldReturnTraineeProfileByTraineeTisId() {
    TraineeProfile traineeProfile = repository.findByTraineeTisId("1111");
    assertThat(traineeProfile.getId(), is("1"));
  }

  @Test
  @Transactional
  void shouldReturnTraineeProfileWhenEmailFound() {
    Optional<TraineeProfile> traineeProfile = repository.findByTraineeEmail(EMAIL);
    assertThat("Unexpected trainee profile ID.", traineeProfile.get().getId(), is("1"));
  }

  @Test
  @Transactional
  void shouldReturnEmptyWhenEmailNotFound() {
    Optional<TraineeProfile> traineeProfile = repository.findByTraineeEmail("1");
    assertThat("Unexpected trainee profile.", traineeProfile.isPresent(), is(false));
  }
}
