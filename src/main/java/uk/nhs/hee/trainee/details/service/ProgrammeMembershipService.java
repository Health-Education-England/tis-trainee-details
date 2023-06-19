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
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.dto.enumeration.GoldGuideVersion;
import uk.nhs.hee.trainee.details.mapper.ProgrammeMembershipMapper;
import uk.nhs.hee.trainee.details.model.ConditionsOfJoining;
import uk.nhs.hee.trainee.details.model.Curriculum;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;

@Service
@XRayEnabled
public class ProgrammeMembershipService {

  private final TraineeProfileRepository repository;
  private final ProgrammeMembershipMapper mapper;
  private final CachingDelegate cachingDelegate;

  ProgrammeMembershipService(TraineeProfileRepository repository,
      ProgrammeMembershipMapper mapper, CachingDelegate cachingDelegate) {
    this.repository = repository;
    this.mapper = mapper;
    this.cachingDelegate = cachingDelegate;
  }

  /**
   * Update the programme membership for the trainee with the given TIS ID.
   *
   * @param traineeTisId        The TIS id of the trainee.
   * @param programmeMembership The programme membership to update for the trainee.
   * @return The updated programme membership or empty if a trainee with the ID was not found.
   */
  public Optional<ProgrammeMembership> updateProgrammeMembershipForTrainee(String traineeTisId,
      ProgrammeMembership programmeMembership) {
    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);

    if (traineeProfile == null) {
      return Optional.empty();
    }

    List<ProgrammeMembership> existingProgrammeMemberships = traineeProfile
        .getProgrammeMemberships();

    if (programmeMembership.getConditionsOfJoining() == null
        || programmeMembership.getConditionsOfJoining().signedAt() == null) {

      // Restore the Conditions of Joining if it exists
      // FIXME: push all PMs through tis-trainee-sync to flush 2 and 3 so they can be removed?
      // 3 scenarios:
      try {
        //1. new uuid PM, with CoJ also saved against this PM *THE FUTURE*
        UUID uuid = UUID.fromString(programmeMembership.getTisId());
        ProgrammeMembership savedProgrammeMembership
            = existingProgrammeMemberships.stream()
            .filter(i -> i.getTisId().equals(uuid.toString()))
            .findAny()
            .orElse(null);
        if (savedProgrammeMembership == null) {
          //2. new uuid PM, but with CoJ saved against old PM with delimited cm ids *THE PRESENT*
          for (Curriculum curriculum : programmeMembership.getCurricula()) {
            ProgrammeMembership oldProgrammeMembership
                = existingProgrammeMemberships.stream()
                .filter(i -> Arrays.stream(i.getTisId().split(","))
                    .anyMatch(id -> id.equals(curriculum.getTisId())))
                .findAny()
                .orElse(null);
            if (oldProgrammeMembership != null) {
              ConditionsOfJoining savedCoj = oldProgrammeMembership.getConditionsOfJoining();
              programmeMembership.setConditionsOfJoining(savedCoj);
            }
          }

        }
      } catch (IllegalArgumentException e) {
        //3. old cm-ids PM, with CoJ cached against old delimited cm ids *THE PAST*
        for (String id : programmeMembership.getTisId().split(",")) {
          Optional<ConditionsOfJoining> conditionsOfJoiningId
              = cachingDelegate.getConditionsOfJoining(id);
          conditionsOfJoiningId.ifPresent(programmeMembership::setConditionsOfJoining);
          // All results should be the same, but iterating through all IDs ensures a clean cache.
        }
      }
    }

    for (ProgrammeMembership existingProgrammeMembership : existingProgrammeMemberships) {

      if (existingProgrammeMembership.getTisId().equals(programmeMembership.getTisId())) {
        mapper.updateProgrammeMembership(existingProgrammeMembership, programmeMembership);
        repository.save(traineeProfile);
        return Optional.of(existingProgrammeMembership);
      }
    }

    existingProgrammeMemberships.add(programmeMembership);
    repository.save(traineeProfile);
    return Optional.of(programmeMembership);
  }

  /**
   * Delete the programme memberships for the trainee with the given TIS ID.
   *
   * @param traineeTisId The TIS id of the trainee.
   * @return True, or False if a trainee with the ID was not found.
   */
  public boolean deleteProgrammeMembershipsForTrainee(String traineeTisId) {
    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);

    if (traineeProfile == null) {
      return false;
    }
    List<ProgrammeMembership> existingProgrammeMemberships = traineeProfile
        .getProgrammeMemberships();

    // Cache any signed Conditions of Joining so that it can be restored later.
    existingProgrammeMemberships.stream()
        .filter(pm -> pm.getConditionsOfJoining() != null
            && pm.getConditionsOfJoining().signedAt() != null)
        .flatMap(pm ->
            Stream.of(pm.getTisId().split(",")).map(id -> {
              ProgrammeMembership newPm = new ProgrammeMembership();
              newPm.setTisId(id);
              newPm.setConditionsOfJoining(pm.getConditionsOfJoining());
              return newPm;
            })
        )
        .forEach(pm -> {
          try {
            //preferentially cache against new uuid
            UUID uuid = UUID.fromString(pm.getTisId());
            cachingDelegate.cacheConditionsOfJoining(uuid.toString(),
                pm.getConditionsOfJoining());
          } catch (IllegalArgumentException e) {
            //fallback: cache against delimited ids
            cachingDelegate.cacheConditionsOfJoining(pm.getTisId(),
                pm.getConditionsOfJoining());
          }
        });

    existingProgrammeMemberships.clear();
    repository.save(traineeProfile);

    return true;
  }

  /**
   * Delete the matching programme membership for the trainee with the given TIS ID.
   *
   * @param traineeTisId          The TIS id of the trainee.
   * @param programmeMembershipId The ID of the programme membership to delete.
   * @return True, or False if a trainee with the ID was not found.
   */
  public boolean deleteProgrammeMembershipForTrainee(String traineeTisId,
      String programmeMembershipId) {
    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);

    if (traineeProfile == null) {
      return false;
    }

    for (var iter = traineeProfile.getProgrammeMemberships().iterator(); iter.hasNext(); ) {
      ProgrammeMembership programmeMembership = iter.next();

      if (programmeMembership.getTisId().equals(programmeMembershipId)) {
        iter.remove();
        repository.save(traineeProfile);
        return true;
      }
    }

    return false;
  }

  /**
   * Sign Condition of Joining with the given programme membership ID.
   *
   * @param programmeMembershipId The ID of the programme membership for signing COJ.
   * @return The updated programme membership or empty if the programme membership with the ID was
   *     not found.
   */
  public Optional<ProgrammeMembership> signProgrammeMembershipCoj(
      String traineeTisId, String programmeMembershipId) {

    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);

    if (traineeProfile != null) {
      List<ProgrammeMembership> existingProgrammeMemberships = traineeProfile
          .getProgrammeMemberships();

      for (ProgrammeMembership existingProgrammeMembership : existingProgrammeMemberships) {
        if (existingProgrammeMembership.getTisId().equals(programmeMembershipId)) {
          ConditionsOfJoining conditionsOfJoining =
              new ConditionsOfJoining(Instant.now(), GoldGuideVersion.getLatest());
          existingProgrammeMembership.setConditionsOfJoining(conditionsOfJoining);
          repository.save(traineeProfile);
          return Optional.of(existingProgrammeMembership);
        }
      }
    }
    return Optional.empty();
  }
}
