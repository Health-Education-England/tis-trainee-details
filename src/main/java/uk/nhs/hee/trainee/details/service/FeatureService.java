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

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.config.FeaturesProperties;
import uk.nhs.hee.trainee.details.dto.FeaturesDto;
import uk.nhs.hee.trainee.details.dto.TraineeIdentity;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;

/**
 * A service providing access to feature flag logic.
 */
@Slf4j
@Service
public class FeatureService {

  private final TraineeIdentity identity;
  private final TraineeProfileService profileService;
  private final FeaturesProperties featuresProperties;
  private final ZoneId timezone;

  /**
   * Construct a feature service instance.
   *
   * @param identity           The identity of the caller.
   * @param profileService     The profile service to lookup trainee data.
   * @param featuresProperties The feature properties.
   * @param timezone           The timezone to use.
   */
  public FeatureService(TraineeIdentity identity, TraineeProfileService profileService,
      FeaturesProperties featuresProperties, @Value("${application.timezone}") ZoneId timezone) {
    this.identity = identity;
    this.profileService = profileService;
    this.featuresProperties = featuresProperties;
    this.timezone = timezone;
  }

  /**
   * Get the enabled features for the requesting trainee.
   *
   * @return The features that are enabled.
   */
  public FeaturesDto getFeatures() {
    String traineeId = identity.getTraineeId();
    log.info("Getting enabled features for trainee {}.", traineeId);

    TraineeProfile profile = profileService.getTraineeProfileByTraineeTisId(traineeId);

    return FeaturesDto.builder()
        .ltft(isLtftEnabled(profile))
        .build();
  }

  /**
   * Whether the given profile is allowed access to Less Than Full Time functionality.
   *
   * @param profile The trainee profile to check.
   * @return Whether the trainee should have access to LTFT.
   */
  private boolean isLtftEnabled(TraineeProfile profile) {
    if (profile == null) {
      log.info("LTFT disabled due to missing profile.");
      return false;
    }

    LocalDate now = LocalDate.now(timezone);

    boolean enabled = profile.getProgrammeMemberships().stream()
        .filter(pm -> pm.getEndDate().isAfter(now)) // Past programmes are not valid for LTFT.
        .map(ProgrammeMembership::getManagingDeanery)
        .anyMatch(md -> featuresProperties.ltft().deaneries().contains(md));

    log.info("LTFT enabled for trainee {}: {}", profile.getTraineeTisId(), enabled);

    return enabled;
  }
}
