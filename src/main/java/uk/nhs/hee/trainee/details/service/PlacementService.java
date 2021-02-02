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

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.mapper.PlacementMapper;
import uk.nhs.hee.trainee.details.model.Placement;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;

@Service
public class PlacementService {

  private final TraineeProfileRepository repository;
  private final PlacementMapper mapper;

  PlacementService(TraineeProfileRepository repository, PlacementMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
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
}
