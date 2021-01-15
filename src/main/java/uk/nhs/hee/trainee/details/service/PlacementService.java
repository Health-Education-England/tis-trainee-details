package uk.nhs.hee.trainee.details.service;

import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.mapper.PlacementMapper;
import uk.nhs.hee.trainee.details.model.Placement;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;

import java.util.List;
import java.util.Optional;

@Service
public class PlacementService {

  private final TraineeProfileRepository repository;
  private final PlacementMapper mapper;

  PlacementService(TraineeProfileRepository repository, PlacementMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  /**
   * Update the qualification for the trainee with the given TIS ID.
   *
   * @param tisId         The TIS id of the trainee.
   * @param placement The placement to update for the trainee.
   * @return The updated placement or empty if a trainee with the ID was not found.
   */
  public Optional<Placement> updatePlacementByTisId(String tisId,
                                                        Placement placement) {

    TraineeProfile traineeProfile = repository.findByTraineeTisId(tisId);

    if (traineeProfile == null) {
      return Optional.empty();
    }

    List<Placement> existingPlacements = traineeProfile.getPlacements();

    for (Placement existingPlacement : existingPlacements) {

      if (existingPlacement.getPlacementTisId().equals(placement.getPlacementTisId())) {
        mapper.updatePlacement(existingPlacement, placement);
        repository.save(traineeProfile);
        return Optional.of(existingPlacement);
      }
    }

    existingPlacements.add(placement);
    repository.save(traineeProfile);
    return Optional.of(placement);
  }
}
