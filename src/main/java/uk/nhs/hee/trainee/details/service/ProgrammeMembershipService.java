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
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.mapper.ProgrammeMembershipMapper;
import uk.nhs.hee.trainee.details.model.CurriculumMembership;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;

@Service
public class ProgrammeMembershipService {

  private final TraineeProfileRepository repository;
  private final ProgrammeMembershipMapper mapper;

  ProgrammeMembershipService(TraineeProfileRepository repository,
      ProgrammeMembershipMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
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

  public Optional<ProgrammeMembership> updateCurriculumMembershipForTrainee(String traineeTisId,
      ProgrammeMembership programmeMembership) {

    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);
    if (traineeProfile == null) {
      return Optional.empty();
    }
    ProgrammeMembership updatedProgrammeMembership;
    String curriculumMembershipTisId = programmeMembership.getCurriculumMemberships().get(0)
        .getCurriculumMembershipTisId();
    final Predicate<CurriculumMembership> filterByCurriculumMembershipId =
        curriculumMembership -> curriculumMembership.getCurriculumMembershipTisId()
            .equals(curriculumMembershipTisId);
    final Predicate<? super ProgrammeMembership> filterByCurriculumMembership = pm ->
      pm.getCurriculumMemberships().parallelStream().anyMatch(filterByCurriculumMembershipId);
    // Find Programme Membership with existing Curriculum Membership
    List<ProgrammeMembership> existingProgrammeMemberships = traineeProfile
        .getProgrammeMemberships();
    Optional<ProgrammeMembership> existingProgrammeOpt = existingProgrammeMemberships
        .parallelStream()
        .filter(filterByCurriculumMembership).findFirst();

    // Find Programme Membership
    if (existingProgrammeOpt.isEmpty()) {
      var programmeMembershipFilter = createProgrammeMembershipFilter(programmeMembership);
      existingProgrammeOpt = existingProgrammeMemberships.parallelStream()
          .filter(programmeMembershipFilter).findFirst();
    }

    // Create or update the Programme Membership
    if (existingProgrammeOpt.isEmpty()) {
      existingProgrammeMemberships.add(programmeMembership);
      updatedProgrammeMembership = programmeMembership;
    } else {
      updatedProgrammeMembership = existingProgrammeOpt.get();
      updatedProgrammeMembership.getCurriculumMemberships()
          .removeIf(filterByCurriculumMembershipId);
      programmeMembership.getCurriculumMemberships()
          .addAll(updatedProgrammeMembership.getCurriculumMemberships());
      mapper.updateProgrammeMembership(updatedProgrammeMembership, programmeMembership);
    }

    //Save the updated Programme
    repository.save(traineeProfile);
    return Optional.of(updatedProgrammeMembership);
  }

  private Predicate<? super ProgrammeMembership> createProgrammeMembershipFilter(
      ProgrammeMembership programmeMembership) {
    return pm -> programmeMembership.getTisId().equals(pm.getTisId());
    /*  We should currently be doing something more like this:
    return pm -> programmeMembership.getProgrammeTisId().equals(pm.getProgrammeTisId())
        && programmeMembership.getStartDate().equals(pm.getStartDate())
        && programmeMembership.getEndDate().equals(pm.getEndDate());*/
  }
}
