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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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

  protected static final List<String> MEDICAL_CURRICULA
      = List.of("DENTAL_CURRICULUM", "DENTAL_POST_CCST", "MEDICAL_CURRICULUM");
  protected static final List<String> NON_NEW_START_PROGRAMME_MEMBERSHIP_TYPES
      = List.of("VISITOR", "LAT");
  protected static final Long PROGRAMME_BREAK_DAYS = 355L;

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

      // Restore the Conditions of Joining if it exists. This covers the (generally short-term)
      // case when a CoJ has just been signed, but the data has not yet made the round-trip to TIS
      // and tis-trainee-sync, enriching the incoming programme membership with this information.

      UUID uuid = UUID.fromString(programmeMembership.getTisId());
      ProgrammeMembership savedProgrammeMembership
          = existingProgrammeMemberships.stream()
          .filter(i -> i.getTisId().equals(uuid.toString()))
          .findAny()
          .orElse(null);
      if (savedProgrammeMembership != null
          && savedProgrammeMembership.getConditionsOfJoining() != null) {
        ConditionsOfJoining savedCoj = savedProgrammeMembership.getConditionsOfJoining();
        programmeMembership.setConditionsOfJoining(savedCoj);
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
        .forEach(pm -> {
          UUID uuid = UUID.fromString(pm.getTisId());
          cachingDelegate.cacheConditionsOfJoining(uuid.toString(),
              pm.getConditionsOfJoining());
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
   * not found.
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
              new ConditionsOfJoining(Instant.now(), GoldGuideVersion.getLatest(), null);
          existingProgrammeMembership.setConditionsOfJoining(conditionsOfJoining);
          repository.save(traineeProfile);
          return Optional.of(existingProgrammeMembership);
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Assess if the programme membership for a trainee is a new starter.
   *
   * @param traineeTisId          The TIS id of the trainee.
   * @param programmeMembershipId The ID of the programme membership to assess.
   * @return True, or False if the programme membership is not a new starter.
   */
  public boolean isNewStarter(String traineeTisId, String programmeMembershipId) {
    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);

    if (traineeProfile == null) {
      return false;
    }

    //get the list of programme memberships with only medical curricula attached
    List<ProgrammeMembership> pmsToConsider
        = getPmsMedicalCurricula(traineeProfile.getProgrammeMemberships());

    //get the programme membership that must be assessed
    Optional<ProgrammeMembership> optionalProgrammeMembership
        = pmsToConsider.stream().filter(p -> p.getTisId().equals(programmeMembershipId)).findAny();

    //it cannot be a new starter if it does not exist, or if it is not a medical one
    if (optionalProgrammeMembership.isEmpty()) {
      return false;
    }
    ProgrammeMembership programmeMembership = optionalProgrammeMembership.get();

    //it cannot be a new starter if it has a non-applicable programme membership type
    if (programmeMembership.getProgrammeMembershipType() == null
        || NON_NEW_START_PROGRAMME_MEMBERSHIP_TYPES.stream()
        .anyMatch(programmeMembership.getProgrammeMembershipType()::equalsIgnoreCase)) {
      return false;
    }

    //it cannot be a new starter if it has already finished
    if (programmeMembership.getEndDate() == null
        || programmeMembership.getEndDate().isBefore(LocalDate.now())) {
      return false;
    }

    List<ProgrammeMembership> otherPms
        = pmsToConsider.stream()
        .filter(pm -> !pm.getTisId().equals(programmeMembershipId)).toList();

    List<ProgrammeMembership> intraOrRotaPms = getIntraOrRotaPms(programmeMembership, otherPms);
    List<ProgrammeMembership> precedingPms = getRecentPrecedingPms(programmeMembership, otherPms);

    //if there are no intra-deanery transfer or rota PMs, or no preceding PM, it is a new starter
    if (intraOrRotaPms.isEmpty() || precedingPms.isEmpty()) {
      return true;
    }
    //if the preceding PM is one of the intra-deanery or rota PMs, it is not a new starter
    return !intraOrRotaPms.contains(precedingPms.get(0));
  }

  /**
   * Remove non-medical curricula from a list of programme memberships. Returned programme
   * memberships will each contain at least one medical curriculum.
   *
   * @param pms The list of programme memberships.
   * @return The filtered list of medical curricula containing programme memberships.
   */
  private List<ProgrammeMembership> getPmsMedicalCurricula(List<ProgrammeMembership> pms) {
    List<ProgrammeMembership> filteredPms = new ArrayList<>();
    for (ProgrammeMembership programmeMembership : pms) {
      List<Curriculum> filteredCms = programmeMembership.getCurricula().stream()
          .filter(c -> {
            String subtype = c.getCurriculumSubType();
            if (subtype == null) {
              return false;
            } else {
              return MEDICAL_CURRICULA.stream().anyMatch(subtype::equalsIgnoreCase);
            }
          })
          .toList();
      if (!filteredCms.isEmpty()) {
        programmeMembership.setCurricula(filteredCms);
        filteredPms.add(programmeMembership);
      }
    }
    return filteredPms;
  }

  /**
   * Get programme memberships that comprise intra-deanery transfers or rotas for another programme
   * membership.
   *
   * @param anchorPm     The programme membership against which to assess the others.
   * @param candidatePms The list of candidate programme memberships.
   * @return The programme memberships that comprise intra-deanery transfers or rotas.
   */
  private List<ProgrammeMembership> getIntraOrRotaPms(ProgrammeMembership anchorPm,
                                                      List<ProgrammeMembership> candidatePms) {
    List<ProgrammeMembership> newStarterPmsWithSameDeaneryStartedBeforeAnchor =
        candidatePms.stream()
            .filter(pm -> {
              if (pm.getProgrammeMembershipType() == null) {
                return false;
              } else {
                return (NON_NEW_START_PROGRAMME_MEMBERSHIP_TYPES.stream()
                    .noneMatch(pm.getProgrammeMembershipType()::equalsIgnoreCase));
              }
            })
            .filter(pm -> {
              if (pm.getManagingDeanery() == null || anchorPm.getManagingDeanery() == null) {
                return false;
              } else {
                return pm.getManagingDeanery().equalsIgnoreCase(anchorPm.getManagingDeanery());
              }
            })
            .filter(pm -> {
              if (pm.getStartDate() == null || anchorPm.getStartDate() == null) {
                return false;
              } else {
                return pm.getStartDate().isBefore(anchorPm.getStartDate());
              }
            }).toList();

    List<String> anchorPmCurriculumSpecialties = anchorPm.getCurricula().stream()
        .map(Curriculum::getCurriculumSpecialtyCode)
        .filter(Objects::nonNull).toList();

    return new ArrayList<>(newStarterPmsWithSameDeaneryStartedBeforeAnchor.stream()
        .filter(pm -> {
          List<String> sharedCurriculumSpecialties = new ArrayList<>(pm.getCurricula().stream()
              .map(Curriculum::getCurriculumSpecialtyCode)
              .filter(Objects::nonNull).toList());
          sharedCurriculumSpecialties.retainAll(anchorPmCurriculumSpecialties);

          return !sharedCurriculumSpecialties.isEmpty();
        }).toList());
  }

  /**
   * Get the list of programme memberships that started before another programme membership and
   * finished within PROGRAMME_BREAK_DAYS of it, sorted in descending start-date order.
   *
   * @param anchorPm     The programme membership against which to assess the others.
   * @param candidatePms The possible programme memberships.
   * @return The filtered list.
   */
  List<ProgrammeMembership> getRecentPrecedingPms(ProgrammeMembership anchorPm,
                                                  List<ProgrammeMembership> candidatePms) {
    List<ProgrammeMembership> precedingPms
        = new ArrayList<>(candidatePms.stream()
        .filter(pm -> {
          if (pm.getStartDate() == null || anchorPm.getStartDate() == null) {
            return false;
          } else {
            return pm.getStartDate().isBefore(anchorPm.getStartDate());
          }
        })
        .filter(pm -> {
          if (pm.getEndDate() == null) {
            return false;
          } else {
            return pm.getEndDate().isAfter(anchorPm.getStartDate().minusDays(PROGRAMME_BREAK_DAYS));
          }
        }).toList());
    precedingPms.sort(Comparator.comparing(ProgrammeMembership::getStartDate).reversed());
    return precedingPms;
  }
}
