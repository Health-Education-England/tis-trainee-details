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

  public static final String FOUNDATION_CURRICULUM_SUBTYPE = "AFT";

  private static final Set<String> NON_FOUNDATION_TRAINEE_CURRICULUM_SUB_TYPES = Set.of(
      "MEDICAL_CURRICULUM", "MEDICAL_SPR");

  public static final String FOUNDATION_SPECIALTY = "FOUNDATION";
  private static final String PUBLIC_HEALTH_MEDICINE_SPECIALTY = "PUBLIC HEALTH MEDICINE";
  private static final Set<String> NON_SPECIALTY_TRAINEE_CURRICULUM_SPECIALTIES = Set.of(
      PUBLIC_HEALTH_MEDICINE_SPECIALTY, FOUNDATION_SPECIALTY);

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
    log.debug("Getting enabled features for trainee {}.", traineeId);

    TraineeProfile profile = profileService.getTraineeProfileByTraineeTisId(traineeId);

    if (profile == null) {
      log.debug("Profile is missing, disabling all features.");
      return FeaturesDto.disable();
    }

    // Specialty
    if (isSpecialtyTrainee(profile)) {
      log.debug("{} is a specialty trainee.", profile.getTraineeTisId());
      List<String> ltftProgrammes = getLtftEnabledSpecialtyProgrammes(profile);

      if (isLtftEnabled(profile, ltftProgrammes)) {
        log.debug("Trainee is in LTFT pilot, enabling all features including LTFT pilot access.");
        return FeaturesDto.enableWithLtftPilot(ltftProgrammes);
      }

      log.debug("Enabling all released features.");
      return FeaturesDto.enable();
    }

    // Public Health
    if (isPublicHealthTrainee(profile)) {
      log.debug("{} is a Public Health trainee.", profile.getTraineeTisId());
      // Public Health trainees have their own set of LTFT-enabled deaneries.
      List<String> ltftProgrammes = getLtftEnabledPublicHealthProgrammes(profile);

      if (isLtftEnabled(profile, ltftProgrammes)) {
        log.debug("Trainee is in LTFT pilot, enabling all features including LTFT pilot access.");
        return FeaturesDto.enableWithLtftPilot(ltftProgrammes);
      }

      log.debug("Enabling all released features.");
      return FeaturesDto.enable();
    }

    // Foundation
    if (isFoundationTrainee(profile)) {
      List<String> ltftProgrammes = getLtftEnabledFoundationProgrammes(profile);

      if (!ltftProgrammes.isEmpty()) {
        log.debug("{} is a Foundation trainee in enabled Deanery, enabling all features except "
                + "Form Rs, CoJ.",
            profile.getTraineeTisId());
      } else {
        log.debug("{} is a Foundation trainee, enabling all features except Form Rs, LTFT, CoJ.",
            profile.getTraineeTisId());
      }

      return FeaturesDto.enableForFoundation(ltftProgrammes);
    }

    // None of the above
    log.debug("Not a specialty, public health or foundation trainee, setting read-only features.");
    return FeaturesDto.readOnly();
  }

  /**
   * Check whether the given profile contains a specialty programme. Indicating that the trainee is,
   * or will be, a specialty trainee.
   *
   * @param profile The trainee profile to check.
   * @return Whether the trainee has a specialty programme in their profile.
   */
  private boolean isSpecialtyTrainee(TraineeProfile profile) {
    return profile.getProgrammeMemberships().stream()
        .flatMap(pm -> pm.getCurricula().stream())
        .anyMatch(curriculum -> {
          String subType = curriculum.getCurriculumSubType();
          String specialty = curriculum.getCurriculumSpecialty();

          return subType != null && specialty != null
              && NON_FOUNDATION_TRAINEE_CURRICULUM_SUB_TYPES.contains(subType.toUpperCase())
              && !NON_SPECIALTY_TRAINEE_CURRICULUM_SPECIALTIES.contains(specialty.toUpperCase());
        });
  }

  /**
   * Check whether the given profile contains a public health programme. Indicating that the trainee
   * is, or will be, a Public Health trainee.
   *
   * @param profile The trainee profile to check.
   * @return Whether the trainee has a public health programme in their profile.
   */
  private boolean isPublicHealthTrainee(TraineeProfile profile) {
    return profile.getProgrammeMemberships().stream()
        .anyMatch(ProgrammeMembershipService::isPublicHealthProgramme);
  }

  /**
   * Check whether the given profile contains a foundation programme. Indicating that the trainee
   * is, or will be, a Foundation trainee.
   *
   * @param profile The trainee profile to check.
   * @return Whether the trainee has a foundation programme in their profile.
   */
  private boolean isFoundationTrainee(TraineeProfile profile) {
    return profile.getProgrammeMemberships().stream()
        .anyMatch(ProgrammeMembershipService::isFoundationProgramme);
  }

  /**
   * Whether the given profile is allowed access to Less Than Full Time functionality.
   *
   * @param profile        The trainee profile to check.
   * @param ltftProgrammes The LTFT-enabled programmes for the trainee.
   * @return Whether the trainee should have access to LTFT.
   */
  private boolean isLtftEnabled(TraineeProfile profile, List<String> ltftProgrammes) {
    Set<String> groups = identity.getGroups();
    boolean isBetaUser = groups != null && groups.contains("beta-participant");

    boolean enabled = isBetaUser || !ltftProgrammes.isEmpty();

    log.info("LTFT enabled for trainee {}: {}", profile.getTraineeTisId(), enabled);

    return enabled;
  }

  /**
   * Get the list of LTFT-enabled programmes for the given Specialty trainee profile. Only current
   * (not ended) programmes managed by an enabled Specialty deanery qualify.
   *
   * @param profile The trainee profile to check.
   * @return The list of LTFT-enabled programmes, or an empty list if none qualify.
   */
  private List<String> getLtftEnabledSpecialtyProgrammes(TraineeProfile profile) {
    List<String> pmIds = getCurrentProgrammesInDeaneries(profile,
        featuresProperties.specialtyDeaneries());

    log.info("LTFT enabled Specialty programme memberships for trainee {}: {}",
        profile.getTraineeTisId(), pmIds);

    return pmIds;
  }

  /**
   * Get the list of LTFT-enabled programmes for the given Public Health trainee profile. Only
   * current (not ended) programmes managed by an enabled Public Health deanery qualify.
   *
   * @param profile The trainee profile to check.
   * @return The list of LTFT-enabled programmes, or an empty list if none qualify.
   */
  private List<String> getLtftEnabledPublicHealthProgrammes(TraineeProfile profile) {
    List<String> pmIds = getCurrentProgrammesInDeaneries(profile,
        featuresProperties.publicHealthDeaneries());

    log.info("LTFT enabled Public Health programme memberships for trainee {}: {}",
        profile.getTraineeTisId(), pmIds);

    return pmIds;
  }

  /**
   * Get the IDs of current (not ended) programmes managed by any of the given deaneries.
   *
   * @param profile   The trainee profile to check.
   * @param deaneries The managing deaneries to match, may be null or empty.
   * @return The list of matching programme IDs, or an empty list if no deaneries are given.
   */
  private List<String> getCurrentProgrammesInDeaneries(TraineeProfile profile,
      Set<String> deaneries) {
    if (deaneries == null || deaneries.isEmpty()) {
      return List.of();
    }

    LocalDate now = LocalDate.now(timezone);

    return profile.getProgrammeMemberships().stream()
        // Past programmes are not valid for LTFT.
        .filter(pm -> pm.getEndDate() != null && pm.getEndDate().isAfter(now))
        .filter(pm -> pm.getManagingDeanery() != null
            && deaneries.contains(pm.getManagingDeanery()))
        .map(ProgrammeMembership::getTisId)
        .toList();
  }

  /**
   * Get the list of LTFT-enabled Foundation programmes for the given profile. Only current (not
   * ended) Foundation programmes managed by an enabled Foundation deanery qualify.
   *
   * @param profile The trainee profile to check.
   * @return The list of LTFT-enabled Foundation programmes, or an empty list if none qualify.
   */
  private List<String> getLtftEnabledFoundationProgrammes(TraineeProfile profile) {
    LocalDate now = LocalDate.now(timezone);
    Set<String> foundationDeaneries = featuresProperties.foundationDeaneries();

    if (foundationDeaneries == null || foundationDeaneries.isEmpty()) {
      return List.of();
    }

    List<String> pmIds = profile.getProgrammeMemberships().stream()
        .filter(ProgrammeMembershipService::isFoundationProgramme)
        .filter(pm -> pm.getManagingDeanery() != null
            && foundationDeaneries.contains(pm.getManagingDeanery()))
        // Past programmes are not valid for LTFT.
        .filter(pm -> pm.getEndDate() != null && pm.getEndDate().isAfter(now))
        .map(ProgrammeMembership::getTisId)
        .toList();

    log.info("LTFT enabled Foundation programme memberships for trainee {}: {}",
        profile.getTraineeTisId(), pmIds);

    return pmIds;
  }
}
