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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.nhs.hee.trainee.details.dto.enumeration.Status;
import uk.nhs.hee.trainee.details.mapper.PlacementMapper;
import uk.nhs.hee.trainee.details.model.Placement;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.Site;
import uk.nhs.hee.trainee.details.model.Specialty;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;

class PlacementServiceTest {

  private static final LocalDate START_DATE = LocalDate.now();
  private static final LocalDate END_DATE = START_DATE.plusYears(1);
  private static final String SITE = "site-";
  private static final String SITE_LOCATION = "siteLocation-";
  private static final String SITE_KNOWN_AS = "siteKnownAs-";
  private static final String GRADE = "grade-";
  private static final String SPECIALTY = "specialty-";
  private static final String SUB_SPECIALTY = "subSpecialty-";
  private static final Boolean POST_ALLOWS_SUBSPECIALTY = true;
  private static final String OTHER_SPECIALTY = "otherSpecialty-";
  private static final String PLACEMENT_TYPE = "placementType-";
  private static final String TRAINEE_TIS_ID = "40";
  private static final String MODIFIED_SUFFIX = "post";
  private static final String ORIGINAL_SUFFIX = "pre";
  private static final String NEW_PLACEMENT_ID = "1";
  private static final String EXISTING_PLACEMENT_ID = "2";
  private static final String NOT_EXISTING_PLACEMENT_ID = "3";
  private static final String PROGRAMME_MEMBERSHIP_ID = "pm-id";

  private PlacementService service;
  private TraineeProfileRepository repository;
  private ProgrammeMembershipService programmeMembershipService;

  @BeforeEach
  void setUp() {
    repository = mock(TraineeProfileRepository.class);
    programmeMembershipService = mock(ProgrammeMembershipService.class);
    service = new PlacementService(repository,
        Mappers.getMapper(PlacementMapper.class), programmeMembershipService);
  }

  @Test
  void shouldNotUpdatePlacementWhenTraineeIdNotFound() {
    Optional<Placement> placement = service
        .updatePlacementForTrainee("notFound", new Placement());

    assertThat("Unexpected optional isEmpty flag.", placement.isEmpty(), is(true));
    verify(repository).findByTraineeTisId("notFound");
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldAddPlacementWhenTraineeFoundAndNoPlacementsExists() {
    TraineeProfile traineeProfile = new TraineeProfile();

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<Placement> placement = service.updatePlacementForTrainee(TRAINEE_TIS_ID,
        createPlacement(NEW_PLACEMENT_ID, MODIFIED_SUFFIX, 100));

    assertThat("Unexpected optional isEmpty flag.", placement.isEmpty(), is(false));

    Placement expectedPlacement = new Placement();
    expectedPlacement.setTisId(NEW_PLACEMENT_ID);
    expectedPlacement.setStartDate(START_DATE.plusDays(100));
    expectedPlacement.setEndDate(END_DATE.plusDays(100));
    expectedPlacement.setGrade(GRADE);
    expectedPlacement.setSpecialty(SPECIALTY);
    expectedPlacement.setSubSpecialty(SUB_SPECIALTY);
    expectedPlacement.setPostAllowsSubspecialty(POST_ALLOWS_SUBSPECIALTY);
    expectedPlacement.setPlacementType(PLACEMENT_TYPE);
    expectedPlacement.setStatus(Status.CURRENT);

    Specialty otherSpecialty = new Specialty();
    otherSpecialty.setName(OTHER_SPECIALTY);
    expectedPlacement.setOtherSpecialties(Set.of(otherSpecialty));

    Site site = new Site();
    site.setName(SITE + MODIFIED_SUFFIX);
    site.setKnownAs(SITE_KNOWN_AS + MODIFIED_SUFFIX);
    site.setLocation(SITE_LOCATION + MODIFIED_SUFFIX);
    expectedPlacement.setSite(site);

    assertThat("Unexpected placement.", placement.get(), is(expectedPlacement));
  }

  @Test
  void shouldAddPlacementWhenTraineeFoundAndPlacementNotExists() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getPlacements()
        .add(createPlacement(EXISTING_PLACEMENT_ID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<Placement> placement = service.updatePlacementForTrainee(TRAINEE_TIS_ID,
        createPlacement(NEW_PLACEMENT_ID, MODIFIED_SUFFIX, 100));

    assertThat("Unexpected optional isEmpty flag.", placement.isEmpty(), is(false));

    Placement expectedPlacement = new Placement();
    expectedPlacement.setTisId(NEW_PLACEMENT_ID);
    expectedPlacement.setStartDate(START_DATE.plusDays(100));
    expectedPlacement.setEndDate(END_DATE.plusDays(100));
    expectedPlacement.setGrade(GRADE);
    expectedPlacement.setSpecialty(SPECIALTY);
    expectedPlacement.setSubSpecialty(SUB_SPECIALTY);
    expectedPlacement.setPostAllowsSubspecialty(POST_ALLOWS_SUBSPECIALTY);
    expectedPlacement.setPlacementType(PLACEMENT_TYPE);
    expectedPlacement.setStatus(Status.CURRENT);

    Specialty otherSpecialty = new Specialty();
    otherSpecialty.setName(OTHER_SPECIALTY);
    expectedPlacement.setOtherSpecialties(Set.of(otherSpecialty));

    Site site = new Site();
    site.setName(SITE + MODIFIED_SUFFIX);
    site.setKnownAs(SITE_KNOWN_AS + MODIFIED_SUFFIX);
    site.setLocation(SITE_LOCATION + MODIFIED_SUFFIX);
    expectedPlacement.setSite(site);

    assertThat("Unexpected placement.", placement.get(), is(expectedPlacement));
  }

  @Test
  void shouldUpdatePlacementWhenTraineeFoundAndPlacementExists() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getPlacements()
        .add(createPlacement(EXISTING_PLACEMENT_ID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<Placement> placement = service.updatePlacementForTrainee(TRAINEE_TIS_ID,
        createPlacement(EXISTING_PLACEMENT_ID, MODIFIED_SUFFIX, 100));

    assertThat("Unexpected optional isEmpty flag.", placement.isEmpty(), is(false));

    Placement expectedPlacement = createPlacement(EXISTING_PLACEMENT_ID,
        ORIGINAL_SUFFIX, 0);
    expectedPlacement.setTisId(EXISTING_PLACEMENT_ID);
    expectedPlacement.setStartDate(START_DATE.plusDays(100));
    expectedPlacement.setEndDate(END_DATE.plusDays(100));
    expectedPlacement.setGrade(GRADE);
    expectedPlacement.setSpecialty(SPECIALTY);
    expectedPlacement.setSubSpecialty(SUB_SPECIALTY);
    expectedPlacement.setPostAllowsSubspecialty(POST_ALLOWS_SUBSPECIALTY);
    expectedPlacement.setPlacementType(PLACEMENT_TYPE);
    expectedPlacement.setStatus(Status.CURRENT);

    Site site = new Site();
    site.setName(SITE + MODIFIED_SUFFIX);
    site.setKnownAs(SITE_KNOWN_AS + MODIFIED_SUFFIX);
    site.setLocation(SITE_LOCATION + MODIFIED_SUFFIX);
    expectedPlacement.setSite(site);

    assertThat("Unexpected placement.", placement.get(), is(expectedPlacement));
  }

  @Test
  void shouldDeletePlacementWhenTraineeFoundAndPlacementExists() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getPlacements()
        .add(createPlacement(EXISTING_PLACEMENT_ID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean result = service.deletePlacementForTrainee(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected result.", result, is(true));
  }

  @Test
  void shouldNotDeletePlacementWhenTraineeNotFound() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getPlacements()
        .add(createPlacement(EXISTING_PLACEMENT_ID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(null);

    boolean result = service.deletePlacementForTrainee(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected result.", result, is(false));
  }

  @Test
  void shouldNotDeletePlacementWhenPlacementNotFound() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getPlacements()
        .add(createPlacement(EXISTING_PLACEMENT_ID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean result = service.deletePlacementForTrainee(TRAINEE_TIS_ID, NOT_EXISTING_PLACEMENT_ID);

    assertThat("Unexpected result.", result, is(false));
  }

  @Test
  void pilot2024ShouldBeFalseIfTraineeNotFound() {
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(null);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected pilot2024 value.", isPilot2024, is(false));
  }

  @Test
  void pilot2024ShouldBeFalseIfPlacementNotFound() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPlacements(
        List.of(createPlacement("unknown id", "", 0)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @Test
  void pilot2024ShouldBeFalseIfTraineeHasNoProgrammeMemberships() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPlacements(
        List.of(createPlacement(EXISTING_PLACEMENT_ID, "", 0)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @Test
  void pilot2024ShouldBeFalseIfTraineeProgrammeMembershipsFinishedBeforePlacement() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPlacements(
        List.of(createPlacement(EXISTING_PLACEMENT_ID, "", 0)));
    LocalDate dateFinished = START_DATE.minusDays(1);
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembership(null, LocalDate.MIN, dateFinished)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @Test
  void pilot2024ShouldBeFalseIfTraineeProgrammeMembershipsStartMonthAfterPlacement() {
    TraineeProfile traineeProfile = new TraineeProfile();
    LocalDate placementStart = LocalDate.of(2024, 8, 1);
    LocalDate nextMonth = LocalDate.of(2024, 9, 2);
    traineeProfile.setPlacements(
        List.of(createPlacement(EXISTING_PLACEMENT_ID, "", 0)));
    traineeProfile.getPlacements().get(0).setStartDate(placementStart);
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembership(null, nextMonth, LocalDate.MAX)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @Test
  void pilot2024ShouldBeTrueIfTraineeProgrammeMembershipIsInPilot() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPlacements(
        List.of(createPlacement(EXISTING_PLACEMENT_ID, "", 0)));
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembership(PROGRAMME_MEMBERSHIP_ID, LocalDate.MIN, LocalDate.MAX)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(programmeMembershipService.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_MEMBERSHIP_ID))
        .thenReturn(true);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(true));
  }

  @Test
  void pilot2024ShouldBeFalseIfTraineeProgrammeMembershipIsNotInPilot() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPlacements(
        List.of(createPlacement(EXISTING_PLACEMENT_ID, "", 0)));
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembership(PROGRAMME_MEMBERSHIP_ID, LocalDate.MIN, LocalDate.MAX)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(programmeMembershipService.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_MEMBERSHIP_ID))
        .thenReturn(false);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @Test
  void pilot2024ShouldBeTrueIfAnyTraineeProgrammeMembershipIsInPilot() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPlacements(
        List.of(createPlacement(EXISTING_PLACEMENT_ID, "", 0)));
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembership(PROGRAMME_MEMBERSHIP_ID, LocalDate.MIN, LocalDate.MAX),
            getProgrammeMembership("not pilot", LocalDate.MIN, LocalDate.MAX)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(programmeMembershipService.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_MEMBERSHIP_ID))
        .thenReturn(true);
    when(programmeMembershipService.isPilot2024(TRAINEE_TIS_ID, "not pilot"))
        .thenReturn(false);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(true));
  }

  @Test
  void pilot2024ShouldBeTrueIfAnyTraineeProgrammeMembershipStartingLaterInMonthIsInPilot() {
    TraineeProfile traineeProfile = new TraineeProfile();
    LocalDate placementStart = LocalDate.of(2024, 8, 1);
    LocalDate laterInSameMonth = LocalDate.of(2024, 8, 31);
    traineeProfile.setPlacements(
        List.of(createPlacement(EXISTING_PLACEMENT_ID, "", 0)));
    traineeProfile.getPlacements().get(0).setStartDate(placementStart);
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembership(PROGRAMME_MEMBERSHIP_ID, laterInSameMonth, LocalDate.MAX),
            getProgrammeMembership("not pilot", LocalDate.MIN, LocalDate.MAX)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(programmeMembershipService.isPilot2024(TRAINEE_TIS_ID, PROGRAMME_MEMBERSHIP_ID))
        .thenReturn(true);
    when(programmeMembershipService.isPilot2024(TRAINEE_TIS_ID, "not pilot"))
        .thenReturn(false);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(true));
  }

  @Test
  void pilot2024ShouldBeFalseIfAnyTraineeProgrammeMembershipIsInDateButNotPilot() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPlacements(
        List.of(createPlacement(EXISTING_PLACEMENT_ID, "", 0)));
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembership("pilot", LocalDate.MIN, LocalDate.MIN),
            getProgrammeMembership("not pilot", LocalDate.MIN, LocalDate.MAX)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(programmeMembershipService.isPilot2024(TRAINEE_TIS_ID, "not pilot"))
        .thenReturn(false);
    when(programmeMembershipService.isPilot2024(TRAINEE_TIS_ID, "pilot"))
        .thenReturn(true);

    boolean isPilot2024 = service.isPilot2024(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected isPilot2024 value.", isPilot2024, is(false));
  }

  @Test
  void rollout2024ShouldBeFalseIfTraineeNotFound() {
    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(null);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected pilotRollout2024 value.", isPilotRollout2024, is(false));
  }

  @Test
  void rollout2024ShouldBeFalseIfPlacementNotFound() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPlacements(
        List.of(createPlacement("unknown id", "", 0)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected isPilotRollout2024 value.", isPilotRollout2024, is(false));
  }

  @Test
  void rollout2024ShouldBeFalseIfTraineeHasNoProgrammeMemberships() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPlacements(
        List.of(createPlacement(EXISTING_PLACEMENT_ID, "", 0)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected isPilotRollout2024 value.", isPilotRollout2024, is(false));
  }

  @Test
  void rollout2024ShouldBeFalseIfTraineeProgrammeMembershipsFinishedBeforePlacement() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPlacements(
        List.of(createPlacement(EXISTING_PLACEMENT_ID, "", 0)));
    LocalDate dateFinished = START_DATE.minusDays(1);
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembership(null, LocalDate.MIN, dateFinished)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected isPilotRollout2024 value.", isPilotRollout2024, is(false));
  }

  @Test
  void rollout2024ShouldBeFalseIfTraineeProgrammeMembershipsStartMonthAfterPlacement() {
    TraineeProfile traineeProfile = new TraineeProfile();
    LocalDate placementStart = LocalDate.of(2024, 8, 1);
    LocalDate nextMonth = LocalDate.of(2024, 9, 2);
    traineeProfile.setPlacements(
        List.of(createPlacement(EXISTING_PLACEMENT_ID, "", 0)));
    traineeProfile.getPlacements().get(0).setStartDate(placementStart);
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembership(null, nextMonth, LocalDate.MAX)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected isPilotRollout2024 value.", isPilotRollout2024, is(false));
  }

  @Test
  void rollout2024ShouldBeTrueIfTraineeProgrammeMembershipIsInPilotRollout() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPlacements(
        List.of(createPlacement(EXISTING_PLACEMENT_ID, "", 0)));
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembership(PROGRAMME_MEMBERSHIP_ID, LocalDate.MIN, LocalDate.MAX)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(programmeMembershipService.isPilotRollout2024(TRAINEE_TIS_ID, PROGRAMME_MEMBERSHIP_ID))
        .thenReturn(true);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected isPilotRollout2024 value.", isPilotRollout2024, is(true));
  }

  @Test
  void rollout2024ShouldBeFalseIfTraineeProgrammeMembershipIsNotInPilotRollout() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPlacements(
        List.of(createPlacement(EXISTING_PLACEMENT_ID, "", 0)));
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembership(PROGRAMME_MEMBERSHIP_ID, LocalDate.MIN, LocalDate.MAX)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(programmeMembershipService.isPilotRollout2024(TRAINEE_TIS_ID, PROGRAMME_MEMBERSHIP_ID))
        .thenReturn(false);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected isPilotRollout2024 value.", isPilotRollout2024, is(false));
  }

  @Test
  void rollout2024ShouldBeTrueIfAnyTraineeProgrammeMembershipIsInPilotRollout() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPlacements(
        List.of(createPlacement(EXISTING_PLACEMENT_ID, "", 0)));
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembership(PROGRAMME_MEMBERSHIP_ID, LocalDate.MIN, LocalDate.MAX),
            getProgrammeMembership("not rollout", LocalDate.MIN, LocalDate.MAX)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(programmeMembershipService.isPilotRollout2024(TRAINEE_TIS_ID, PROGRAMME_MEMBERSHIP_ID))
        .thenReturn(true);
    when(programmeMembershipService.isPilotRollout2024(TRAINEE_TIS_ID, "not rollout"))
        .thenReturn(false);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected isPilotRollout2024 value.", isPilotRollout2024, is(true));
  }

  @Test
  void rollout2024ShouldBeTrueIfAnyTraineeProgrammeMembershipStartingLaterInMonthInPilotRollout() {
    TraineeProfile traineeProfile = new TraineeProfile();
    LocalDate placementStart = LocalDate.of(2024, 8, 1);
    LocalDate laterInSameMonth = LocalDate.of(2024, 8, 31);
    traineeProfile.setPlacements(
        List.of(createPlacement(EXISTING_PLACEMENT_ID, "", 0)));
    traineeProfile.getPlacements().get(0).setStartDate(placementStart);
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembership(PROGRAMME_MEMBERSHIP_ID, laterInSameMonth, LocalDate.MAX),
            getProgrammeMembership("not rollout", LocalDate.MIN, LocalDate.MAX)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(programmeMembershipService.isPilotRollout2024(TRAINEE_TIS_ID, PROGRAMME_MEMBERSHIP_ID))
        .thenReturn(true);
    when(programmeMembershipService.isPilotRollout2024(TRAINEE_TIS_ID, "not rollout"))
        .thenReturn(false);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected isPilotRollout2024 value.", isPilotRollout2024, is(true));
  }

  @Test
  void rollout2024ShouldBeFalseIfAnyTraineeProgrammeMembershipIsInDateButNotPilotRollout() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.setPlacements(
        List.of(createPlacement(EXISTING_PLACEMENT_ID, "", 0)));
    traineeProfile.setProgrammeMemberships(
        List.of(getProgrammeMembership("rollout", LocalDate.MIN, LocalDate.MIN),
            getProgrammeMembership("not rollout", LocalDate.MIN, LocalDate.MAX)));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(programmeMembershipService.isPilotRollout2024(TRAINEE_TIS_ID, "not rollout"))
        .thenReturn(false);
    when(programmeMembershipService.isPilotRollout2024(TRAINEE_TIS_ID, "rollout"))
        .thenReturn(true);

    boolean isPilotRollout2024 = service.isPilotRollout2024(TRAINEE_TIS_ID, EXISTING_PLACEMENT_ID);

    assertThat("Unexpected isPilotRollout2024 value.", isPilotRollout2024, is(false));
  }

  /**
   * Create an instance of Placement with default dummy values.
   *
   * @param tisId              The TIS ID to set on the placement.
   * @param stringSuffix       The suffix to use for string values.
   * @param dateAdjustmentDays The number of days to add to dates.
   * @return The dummy entity.
   */
  private Placement createPlacement(String tisId, String stringSuffix,
                                    int dateAdjustmentDays) {
    Placement placement = new Placement();
    placement.setTisId(tisId);
    placement.setStartDate(START_DATE.plusDays(dateAdjustmentDays));
    placement.setEndDate(END_DATE.plusDays(dateAdjustmentDays));
    placement.setGrade(GRADE);
    placement.setSpecialty(SPECIALTY);
    placement.setSubSpecialty(SUB_SPECIALTY);
    placement.setPostAllowsSubspecialty(POST_ALLOWS_SUBSPECIALTY);
    placement.setPlacementType(PLACEMENT_TYPE);
    placement.setStatus(Status.CURRENT);

    Specialty otherSpecialty = new Specialty();
    otherSpecialty.setName(OTHER_SPECIALTY);
    placement.setOtherSpecialties(Set.of(otherSpecialty));

    Site site = new Site();
    site.setName(SITE + stringSuffix);
    site.setKnownAs(SITE_KNOWN_AS + stringSuffix);
    site.setLocation(SITE_LOCATION + stringSuffix);
    placement.setSite(site);

    return placement;
  }

  /**
   * Create a programme membership for testing pilot2024 conditions.
   *
   * @param programmeMembershipTisId The TIS ID to set on the programmeMembership.
   * @param startDate                The start date.
   * @param endDate                  The end date.
   * @return The programme membership.
   */
  private ProgrammeMembership getProgrammeMembership(
      String programmeMembershipTisId, LocalDate startDate,
      LocalDate endDate) {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId(programmeMembershipTisId);
    programmeMembership.setStartDate(startDate);
    programmeMembership.setProgrammeCompletionDate(endDate);

    return programmeMembership;
  }
}
