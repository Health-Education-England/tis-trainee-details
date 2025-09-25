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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.config.FeaturesProperties;
import uk.nhs.hee.trainee.details.dto.FeaturesDto;
import uk.nhs.hee.trainee.details.dto.FeaturesDto.DetailsFeatures;
import uk.nhs.hee.trainee.details.dto.FeaturesDto.DetailsFeatures.ProfileFeatures;
import uk.nhs.hee.trainee.details.dto.FeaturesDto.DetailsFeatures.ProgrammeFeatures;
import uk.nhs.hee.trainee.details.dto.FeaturesDto.Feature;
import uk.nhs.hee.trainee.details.dto.FeaturesDto.FormFeatures;
import uk.nhs.hee.trainee.details.dto.FeaturesDto.FormFeatures.LtftFeatures;
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

    List<String> ltftProgrammes = getLtftEnabledProgrammes(profile);

    boolean featuresEnabled = profile != null;
    if (!featuresEnabled) {
      log.info("Features disabled due to missing profile.");
    }

    return FeaturesDto.builder()
        .actions(new Feature(featuresEnabled))
        .cct(new Feature(featuresEnabled))
        .details(DetailsFeatures.builder()
            .enabled(featuresEnabled)
            .placements(new Feature(featuresEnabled))
            .profile(ProfileFeatures.builder()
                .enabled(featuresEnabled)
                .gmcUpdate(new Feature(featuresEnabled))
                .build())
            .programmes(ProgrammeFeatures.builder()
                .enabled(featuresEnabled)
                .conditionsOfJoining(new Feature(featuresEnabled))
                .confirmation(new Feature(featuresEnabled))
                .build())
            .build())
        .forms(FormFeatures.builder()
            .enabled(featuresEnabled)
            .formr(new Feature(featuresEnabled))
            .ltft(LtftFeatures.builder()
                .enabled(isLtftEnabled(profile, ltftProgrammes))
                .qualifyingProgrammes(Set.copyOf(ltftProgrammes))
                .build())
            .build())
        .notifications(new Feature(featuresEnabled))
        .ltft(isLtftEnabled(profile, ltftProgrammes))
        .ltftProgrammes(ltftProgrammes)
        .build();
  }

  /**
   * Whether the given profile is allowed access to Less Than Full Time functionality.
   *
   * @param profile The trainee profile to check.
   * @param ltftProgrammes The LTFT-enabled programmes for the trainee.
   * @return Whether the trainee should have access to LTFT.
   */
  private boolean isLtftEnabled(TraineeProfile profile, List<String> ltftProgrammes) {
    if (profile == null) {
      log.info("LTFT disabled due to missing profile.");
      return false;
    }

    Set<String> groups = identity.getGroups();
    boolean isBetaUser = groups != null && groups.contains("beta-participant");

    boolean enabled = isBetaUser || !ltftProgrammes.isEmpty();

    log.info("LTFT enabled for trainee {}: {}", profile.getTraineeTisId(), enabled);

    return enabled;
  }

  /**
   * Get the list of LTFT-enabled programmes for the given profile.
   *
   * @param profile The trainee profile to check.
   * @return The list of LTFT-enabled programmes, or an empty list if LTFT is not enabled.
   */
  private List<String> getLtftEnabledProgrammes(TraineeProfile profile) {
    if (profile == null) {
      return List.of();
    }

    LocalDate now = LocalDate.now(timezone);

    Set<String> enabledDeaneries = featuresProperties.ltft().entrySet().stream()
        .filter(tranche -> !tranche.getValue().startDate().isAfter(now))
        .peek(tranche -> log.debug("LTFT tranche enabled: {}", tranche.getKey()))
        .flatMap(tranche -> tranche.getValue().deaneries().stream())
        .collect(Collectors.toSet());

    List<String> ltftPmIds = profile.getProgrammeMemberships().stream()
        // Past programmes are not valid for LTFT.
        .filter(pm -> pm.getEndDate().isAfter(now)
            && enabledDeaneries.contains(pm.getManagingDeanery()))
        .map(ProgrammeMembership::getTisId)
        .toList();

    log.info("LTFT enabled programme memberships for trainee {}: {}",
        profile.getTraineeTisId(), ltftPmIds);

    return ltftPmIds;
  }
}
