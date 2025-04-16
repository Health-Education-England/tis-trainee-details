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

package uk.nhs.hee.trainee.details.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.nhs.hee.trainee.details.config.FeaturesProperties;
import uk.nhs.hee.trainee.details.config.FeaturesProperties.Ltft;
import uk.nhs.hee.trainee.details.dto.FeaturesDto;
import uk.nhs.hee.trainee.details.dto.TraineeIdentity;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;

class FeatureServiceTest {

  private static final String TRAINEE_ID = UUID.randomUUID().toString();

  private FeatureService service;
  private TraineeProfileService profileService;

  @BeforeEach
  void setUp() {
    TraineeIdentity traineeIdentity = new TraineeIdentity();
    traineeIdentity.setTraineeId(TRAINEE_ID);

    profileService = mock(TraineeProfileService.class);
    FeaturesProperties featuresProperties = FeaturesProperties.builder()
        .ltft(Ltft.builder()
            .deaneries(Set.of("test 1", "test 2", "test 3"))
            .build())
        .build();

    service = new FeatureService(traineeIdentity, profileService, featuresProperties,
        ZoneId.of("UTC"));
  }

  @Test
  void shouldDisableLtftWhenNoProfileFound() {
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(null);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.ltft(), is(false));
  }

  @Test
  void shouldDisableLtftWhenNoProgrammes() {
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    profile.setProgrammeMemberships(List.of());
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.ltft(), is(false));
  }

  @Test
  void shouldDisableLtftWhenNoProgrammesInQualifyingDeanery() {
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery("None Qualifying");
    pm.setEndDate(LocalDate.now().plusDays(1));
    profile.setProgrammeMemberships(List.of(pm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.ltft(), is(false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"test 1", "test 2", "test 3"})
  void shouldDisableLtftWhenProgrammeInQualifyingDeaneryEnded(String deanery) {
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(deanery);
    pm.setEndDate(LocalDate.now());
    profile.setProgrammeMemberships(List.of(pm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.ltft(), is(false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"test 1", "test 2", "test 3"})
  void shouldEnableLtftWhenProgrammeInQualifyingDeaneryNotEnded(String deanery) {
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setManagingDeanery(deanery);
    pm.setEndDate(LocalDate.now().plusDays(1));
    profile.setProgrammeMemberships(List.of(pm));
    when(profileService.getTraineeProfileByTraineeTisId(TRAINEE_ID)).thenReturn(profile);

    FeaturesDto features = service.getFeatures();

    assertThat("Unexpected LTFT flag.", features.ltft(), is(true));
  }
}
