/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
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

import com.amazonaws.xray.spring.aop.XRayEnabled;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.mapper.PlacementMapper;
import uk.nhs.hee.trainee.details.model.Placement;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;

@Service
@XRayEnabled
@Slf4j
public class PlacementService {

  private static final Set<String> NON_ONBOARDED_GRADES = Set.of(
      "DCT1", // Dental Core Training Year 1
      "DCT2", // Dental Core Training Year 2
      "DCT3", // Dental Core Training Year 3
      "DFT", // Dental Foundation Training
      "F1", // Foundation Year 1
      "F2" // Foundation Year 2
  );

  private final TraineeProfileRepository repository;
  private final PlacementMapper mapper;
  private final ProgrammeMembershipService programmeMembershipService;

  PlacementService(TraineeProfileRepository repository, PlacementMapper mapper,
      ProgrammeMembershipService programmeMembershipService) {
    this.repository = repository;
    this.mapper = mapper;
    this.programmeMembershipService = programmeMembershipService;
  }

  /**
   * Update the placement for the trainee with the given TIS ID.
   *
   * @param traineeTisId The TIS id of the trainee.
   * @param placement    The placement to update for the trainee.
   * @return The updated placement or empty if a trainee with the ID was not found.
   */
  public Optional<Placement> updatePlacementForTrainee(String traineeTisId, Placement placement) {

    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);

    if (traineeProfile == null) {
      return Optional.empty();
    }

    List<Placement> existingPlacements = traineeProfile.getPlacements();

    for (Placement existingPlacement : existingPlacements) {

      if (existingPlacement.getTisId().equals(placement.getTisId())) {
        mapper.updatePlacement(existingPlacement, placement);
        repository.save(traineeProfile);
        return Optional.of(existingPlacement);
      }
    }

    existingPlacements.add(placement);
    repository.save(traineeProfile);
    return Optional.of(placement);
  }

  /**
   * Delete the programme memberships for the trainee with the given TIS ID.
   *
   * @param traineeTisId   The TIS id of the trainee.
   * @param placementTisId The TIS id of the placement
   * @return True, or False if a trainee with the ID was not found or the placement was not found.
   */
  public boolean deletePlacementForTrainee(String traineeTisId, String placementTisId) {
    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);

    if (traineeProfile == null) {
      return false;
    }
    List<Placement> placements = traineeProfile.getPlacements();

    boolean hasDeleted = placements.removeIf(p -> p.getTisId().equals(placementTisId));

    if (hasDeleted) {
      repository.save(traineeProfile);
      return true;
    }
    return false;
  }

  /**
   * Check whether a trainee can be onboarded based on the given placement.
   *
   * @param traineeTisId The TIS id of the trainee.
   * @param placementId  The ID of the placement to assess.
   * @return Whether the trainee can be onboarded based on this placement.
   */
  public boolean canBeOnboarded(String traineeTisId, String placementId) {
    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);

    if (traineeProfile == null) {
      log.info("Placement not valid for onboarding as no profile found for ID '{}'.", traineeTisId);
      return false;
    }

    return traineeProfile.getPlacements().stream()
        .filter(p -> p.getTisId().equals(placementId))
        .anyMatch(p -> canBeOnboarded(traineeProfile, p));
  }

  /**
   * Check whether a trainee can be onboarded based on the given placement.
   *
   * @param traineeProfile The profile of the trainee.
   * @param placement      The placement to assess.
   * @return Whether the trainee can be onboarded based on this placement.
   */
  private boolean canBeOnboarded(TraineeProfile traineeProfile, Placement placement) {
    String grade = placement.getGrade();

    if (NON_ONBOARDED_GRADES.contains(grade)) {
      log.info("Placement not valid for onboarding with grade '{}'.", grade);
      return false;
    }

    return getPossiblePlacementProgrammes(traineeProfile, placement).stream()
        .anyMatch(programmeMembershipService::canBeOnboarded);
  }

  private Optional<Placement> pilotPreflightChecks(String traineeTisId, String placementId) {
    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);

    if (traineeProfile == null) {
      log.info("2024 pilot / rollout: [false] trainee profile {} not found", traineeTisId);
      return Optional.empty();
    }

    Optional<Placement> optionalPlacement
        = traineeProfile.getPlacements().stream()
        .filter(p -> p.getTisId().equals(placementId)).findAny();

    if (optionalPlacement.isEmpty()) {
      log.info("2024 pilot / rollout: [false] placement {} does not exist", placementId);
    }
    return optionalPlacement;
  }

  /**
   * Assess if the placement for a trainee is in the 2024 pilot. Hopefully a temporary kludge.
   *
   * @param traineeTisId The TIS id of the trainee.
   * @param placementId  The ID of the placement to assess.
   * @return True, or False if the placement is not in the 2024 pilot.
   */
  public boolean isPilot2024(String traineeTisId, String placementId) {
    Optional<Placement> optionalPlacement = pilotPreflightChecks(traineeTisId, placementId);

    if (optionalPlacement.isEmpty()) {
      return false;
    }

    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);
    return getPossiblePlacementProgrammes(traineeProfile, optionalPlacement.get()).stream()
        .anyMatch(pmInPeriod -> programmeMembershipService.isPilot2024(traineeTisId,
            pmInPeriod.getTisId()));
  }

  /**
   * Assess if the placement for a trainee is in the 2024 pilot rollout. Hopefully a temporary
   * kludge.
   *
   * @param traineeTisId The TIS id of the trainee.
   * @param placementId  The ID of the placement to assess.
   * @return True, or False if the placement is not in the 2024 pilot rollout.
   */
  public boolean isPilotRollout2024(String traineeTisId, String placementId) {
    Optional<Placement> optionalPlacement = pilotPreflightChecks(traineeTisId, placementId);

    if (optionalPlacement.isEmpty()) {
      return false;
    }
    Placement placement = optionalPlacement.get();

    //short-circuit
    if (placement.getStartDate().isBefore(LocalDate.of(2024, 11, 1))) {
      return false;
    }

    LocalDate dayAfterPlacementStart = placement.getStartDate().plusDays(1);
    LocalDate dayBeforePlacementStart = placement.getStartDate().minusDays(1);

    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);
    List<ProgrammeMembership> pmsInPeriod = traineeProfile.getProgrammeMemberships().stream()
        .filter(pm -> pm.getStartDate().withDayOfMonth(1).isBefore(dayAfterPlacementStart)
            && pm.getProgrammeCompletionDate().isAfter(dayBeforePlacementStart))
        .toList();
    return pmsInPeriod.stream()
        .filter(pm -> !pm.getManagingDeanery().equalsIgnoreCase("North East"))
        .anyMatch(pmInRollout -> {
          LocalDate notificationEpoch = pmInRollout.getManagingDeanery()
              .equalsIgnoreCase("Thames Valley")
              ? LocalDate.of(2025, 1, 31)
              : LocalDate.of(2024, 10, 31);
          return (placement.getStartDate().isAfter(notificationEpoch));
        });
  }

  /**
   * Get the possible programme memberships associated with the given placement.
   *
   * @param traineeProfile The trainee profile to check PMs of.
   * @param placement      The placement to assess.
   * @return A list of possible programme memberships.
   */
  private List<ProgrammeMembership> getPossiblePlacementProgrammes(TraineeProfile traineeProfile,
      Placement placement) {
    LocalDate dayAfterPlacementStart = placement.getStartDate().plusDays(1);
    LocalDate dayBeforePlacementStart = placement.getStartDate().minusDays(1);

    return traineeProfile.getProgrammeMemberships().stream().filter(pm ->
            pm.getStartDate().withDayOfMonth(1).isBefore(dayAfterPlacementStart)
                && pm.getProgrammeCompletionDate().isAfter(dayBeforePlacementStart))
        .toList();
  }
}
