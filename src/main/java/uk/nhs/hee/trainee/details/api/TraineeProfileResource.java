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

package uk.nhs.hee.trainee.details.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsDto;
import uk.nhs.hee.trainee.details.dto.TraineeProfileDto;
import uk.nhs.hee.trainee.details.mapper.TraineeProfileMapper;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.service.TraineeProfileService;

@RestController
@RequestMapping("/api")
public class TraineeProfileResource {

  private static final Logger log = LoggerFactory.getLogger(TraineeProfileResource.class);

  private TraineeProfileService traineeProfileService;
  private TraineeProfileMapper traineeProfileMapper;

  public TraineeProfileResource(TraineeProfileService traineeProfileService,
      TraineeProfileMapper traineeProfileMapper) {
    this.traineeProfileService = traineeProfileService;
    this.traineeProfileMapper = traineeProfileMapper;
  }


  /**
   * Get a trainee profile record with a given ID.
   *
   * @param traineeProfileId The ID of the trainee profile to get.
   * @return The {@link PersonalDetailsDto} representing the trainee profile.
   */
  @GetMapping("/trainee-profile/{id}")
  public TraineeProfileDto getTraineeProfileById(
      @PathVariable(name = "id") String traineeProfileId) {
    log.trace("Trainee Profile of a trainee by traineeProfileId {}", traineeProfileId);
    TraineeProfile traineeProfile = traineeProfileService.getTraineeProfile(traineeProfileId);
    traineeProfile = traineeProfileService.hidePastProgrammes(traineeProfile);
    traineeProfile = traineeProfileService.hidePastPlacements(traineeProfile);
    return traineeProfileMapper.toDto(traineeProfile);
  }

  /**
   * Get a trainee's trainee profile based on the trainee's ID.
   *
   * @param traineeId The trainee's TIS ID.
   * @return The {@link PersonalDetailsDto} representing the trainee profile.
   */
  @GetMapping("/trainee-profile/trainee/{traineeId}")
  public TraineeProfileDto getTraineeProfileByTraineeId(
      @PathVariable(name = "traineeId") String traineeId) {
    log.trace("Trainee Profile of a trainee by traineeId {}", traineeId);
    TraineeProfile traineeProfile = traineeProfileService
        .getTraineeProfileByTraineeTisId(traineeId);
    traineeProfile = traineeProfileService.hidePastProgrammes(traineeProfile);
    traineeProfile = traineeProfileService.hidePastPlacements(traineeProfile);
    return traineeProfileMapper.toDto(traineeProfile);
  }
}
