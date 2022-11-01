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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.nhs.hee.trainee.details.dto.DataDeltaDto;
import uk.nhs.hee.trainee.details.dto.FieldDeltaDto;
import uk.nhs.hee.trainee.details.dto.enumeration.Status;
import uk.nhs.hee.trainee.details.mapper.PlacementMapper;
import uk.nhs.hee.trainee.details.model.Placement;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PlacementServiceTest {

  private static final LocalDate START_DATE = LocalDate.now();
  private static final LocalDate END_DATE = START_DATE.plusYears(1);
  private static final String SITE = "site-";
  private static final String SITE_LOCATION = "siteLocation-";
  private static final String GRADE = "grade-";
  private static final String SPECIALTY = "specialty-";
  private static final String PLACEMENT_TYPE = "placementType-";
  private static final String TRAINEE_TIS_ID = "40";
  private static final String MODIFIED_SUFFIX = "post";
  private static final String ORIGINAL_SUFFIX = "pre";
  private static final String NEW_PLACEMENT_ID = "1";
  private static final String EXISTING_PLACEMENT_ID = "2";
  private static final String NOT_EXISTING_PLACEMENT_ID = "3";

  private PlacementService service;
  private TraineeProfileRepository repository;
  private DataDeltaService dataDeltaService;

  @BeforeEach
  void setUp() {
    repository = mock(TraineeProfileRepository.class);
    dataDeltaService = mock(DataDeltaService.class);
    service = new PlacementService(repository,
        Mappers.getMapper(PlacementMapper.class), dataDeltaService);
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
    expectedPlacement.setSite(SITE + MODIFIED_SUFFIX);
    expectedPlacement.setSiteLocation(SITE_LOCATION + MODIFIED_SUFFIX);
    expectedPlacement.setGrade(GRADE);
    expectedPlacement.setSpecialty(SPECIALTY);
    expectedPlacement.setPlacementType(PLACEMENT_TYPE);
    expectedPlacement.setStatus(Status.CURRENT);

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
    expectedPlacement.setSite(SITE + MODIFIED_SUFFIX);
    expectedPlacement.setSiteLocation(SITE_LOCATION + MODIFIED_SUFFIX);
    expectedPlacement.setGrade(GRADE);
    expectedPlacement.setSpecialty(SPECIALTY);
    expectedPlacement.setPlacementType(PLACEMENT_TYPE);
    expectedPlacement.setStatus(Status.CURRENT);

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
    expectedPlacement.setSite(SITE + MODIFIED_SUFFIX);
    expectedPlacement.setSiteLocation(SITE_LOCATION + MODIFIED_SUFFIX);
    expectedPlacement.setGrade(GRADE);
    expectedPlacement.setSpecialty(SPECIALTY);
    expectedPlacement.setPlacementType(PLACEMENT_TYPE);
    expectedPlacement.setStatus(Status.CURRENT);

    assertThat("Unexpected placement.", placement.get(), is(expectedPlacement));
  }

  @Test
  void shouldSendDeltaMessage() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getPlacements()
        .add(createPlacement(EXISTING_PLACEMENT_ID, ORIGINAL_SUFFIX, 0));

    DataDeltaDto delta = new DataDeltaDto();
    FieldDeltaDto fieldDelta = new FieldDeltaDto("test", "test", "test");
    delta.getChangedFields().add(fieldDelta);
    when(dataDeltaService.getObjectDelta(any(Placement.class), any(Placement.class), eq(Placement.class))).thenReturn(delta);

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    service.updatePlacementForTrainee(TRAINEE_TIS_ID, createPlacement(EXISTING_PLACEMENT_ID, MODIFIED_SUFFIX, 100));

    verify(dataDeltaService).publishObjectDelta(any());
  }

  @Test
  void shouldNotSendEmptyDeltaMessage() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getPlacements()
        .add(createPlacement(EXISTING_PLACEMENT_ID, ORIGINAL_SUFFIX, 0));

    DataDeltaDto delta = new DataDeltaDto();
    when(dataDeltaService.getObjectDelta(any(Placement.class), any(Placement.class), eq(Placement.class))).thenReturn(delta);

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);

    service.updatePlacementForTrainee(TRAINEE_TIS_ID, createPlacement(EXISTING_PLACEMENT_ID, MODIFIED_SUFFIX, 100));

    verify(dataDeltaService).getObjectDelta(any(Placement.class), any(Placement.class), eq(Placement.class));
    verifyNoMoreInteractions(dataDeltaService);
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
    placement.setSite(SITE + stringSuffix);
    placement.setSiteLocation(SITE_LOCATION + stringSuffix);
    placement.setGrade(GRADE);
    placement.setSpecialty(SPECIALTY);
    placement.setPlacementType(PLACEMENT_TYPE);
    placement.setStatus(Status.CURRENT);

    return placement;
  }
}
