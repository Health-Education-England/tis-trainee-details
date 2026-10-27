/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.LTFT;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST1;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import uk.nhs.hee.trainee.details.dto.CctCalculateResponseDto;
import uk.nhs.hee.trainee.details.dto.CctCalculatedChangeDto;
import uk.nhs.hee.trainee.details.dto.CctCalculationDetailDto;
import uk.nhs.hee.trainee.details.dto.CctCalculationSaveRequestDto;
import uk.nhs.hee.trainee.details.dto.CctChangeDto;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto;
import uk.nhs.hee.trainee.details.dto.CctResultDto;
import uk.nhs.hee.trainee.details.dto.TraineeIdentity;
import uk.nhs.hee.trainee.details.exception.NotRecordOwnerException;
import uk.nhs.hee.trainee.details.mapper.CctMapperImpl;
import uk.nhs.hee.trainee.details.model.CctCalculation;
import uk.nhs.hee.trainee.details.model.CctCalculation.CctAssumedFullTimePeriod;
import uk.nhs.hee.trainee.details.model.CctCalculation.CctChange;
import uk.nhs.hee.trainee.details.model.CctCalculation.CctProgrammeDetails;
import uk.nhs.hee.trainee.details.model.CctCalculation.CctProjection;
import uk.nhs.hee.trainee.details.model.CctCalculation.CctResult;
import uk.nhs.hee.trainee.details.repository.CctCalculationRepository;

class CctServiceTest {

  private static final String TRAINEE_ID = UUID.randomUUID().toString();

  private CctService service;
  private CctCalculationRepository calculationRepository;
  private CctEngine engine;

  @BeforeEach
  void setUp() {
    TraineeIdentity traineeIdentity = new TraineeIdentity();
    traineeIdentity.setTraineeId(TRAINEE_ID);

    calculationRepository = mock(CctCalculationRepository.class);
    engine = new CctEngine();

    service = new CctService(traineeIdentity, calculationRepository, new CctMapperImpl(),
        engine);
  }

  @Test
  void shouldReturnEmptyGettingCalculationsWhenNotFound() {
    when(calculationRepository.findByTraineeIdOrderByLastModified(TRAINEE_ID)).thenReturn(
        List.of());

    List<CctCalculationDetailDto> result = service.getCalculations();

    assertThat("Unexpected calculation details count.", result.size(), is(0));
  }

  @Test
  void shouldGetCalculationsAsSavedWhenFound() {
    UUID calculationId1 = UUID.randomUUID();
    Instant created1 = Instant.now().minus(Duration.ofDays(1));
    Instant lastModified1 = Instant.now().plus(Duration.ofDays(1));

    CctCalculation entity1 = CctCalculation.builder()
        .id(calculationId1)
        .traineeId(TRAINEE_ID)
        .name("Test Calculation 1")
        .result(CctResult.builder().cctDate(LocalDate.of(2099, 1, 1)).build())
        .created(created1)
        .lastModified(lastModified1)
        .build();

    UUID calculationId2 = UUID.randomUUID();
    Instant created2 = Instant.now().minus(Duration.ofDays(2));
    Instant lastModified2 = Instant.now().plus(Duration.ofDays(2));

    CctCalculation entity2 = CctCalculation.builder()
        .id(calculationId2)
        .traineeId(TRAINEE_ID)
        .name("Test Calculation 2")
        .result(CctResult.builder().cctDate(LocalDate.of(2099, 2, 1)).build())
        .created(created2)
        .lastModified(lastModified2)
        .build();

    when(calculationRepository.findByTraineeIdOrderByLastModified(TRAINEE_ID)).thenReturn(
        List.of(entity1, entity2));

    List<CctCalculationDetailDto> result = service.getCalculations();

    assertThat("Unexpected calculation summary count.", result.size(), is(2));

    CctCalculationDetailDto dto1 = result.get(0);
    assertThat("Unexpected calculation ID.", dto1.id(), is(calculationId1));
    assertThat("Unexpected calculation name.", dto1.name(), is("Test Calculation 1"));
    assertThat("Unexpected CCT date.", dto1.result().cctDate(), is(LocalDate.of(2099, 1, 1)));
    assertThat("Unexpected created timestamp.", dto1.created(), is(created1));
    assertThat("Unexpected last modified timestamp.", dto1.lastModified(), is(lastModified1));

    CctCalculationDetailDto dto2 = result.get(1);
    assertThat("Unexpected calculation ID.", dto2.id(), is(calculationId2));
    assertThat("Unexpected calculation name.", dto2.name(), is("Test Calculation 2"));
    assertThat("Unexpected CCT date.", dto2.result().cctDate(), is(LocalDate.of(2099, 2, 1)));
    assertThat("Unexpected created timestamp.", dto2.created(), is(created2));
    assertThat("Unexpected last modified timestamp.", dto2.lastModified(), is(lastModified2));
  }

  @Test
  void shouldReturnEmptyGettingCalculationWhenNotFound() {
    UUID id = UUID.randomUUID();

    when(calculationRepository.findById(id)).thenReturn(Optional.empty());

    Optional<CctCalculationDetailDto> result = service.getCalculation(id);

    assertThat("Unexpected calculation presence.", result.isPresent(), is(false));
  }

  @Test
  void shouldThrowExceptionGettingCalculationWhenNotBelongsToUser() {
    UUID id = UUID.randomUUID();
    CctCalculation entity = CctCalculation.builder()
        .id(id)
        .traineeId(UUID.randomUUID().toString())
        .build();

    when(calculationRepository.findById(id)).thenReturn(Optional.of(entity));

    assertThrows(NotRecordOwnerException.class, () -> service.getCalculation(id));
  }

  @Test
  void shouldGetCalculationAsSavedWhenBelongsToUser() {
    UUID calculationId = UUID.randomUUID();

    Instant created = Instant.now().minus(Duration.ofDays(1));
    Instant lastModified = Instant.now().plus(Duration.ofDays(1));

    CctCalculation entity = CctCalculation.builder()
        .id(calculationId)
        .traineeId(TRAINEE_ID)
        .name("Test Calculation")
        .programmeDetails(CctProgrammeDetails.builder()
            .specialty("Paediatrics")
            .startDate(LocalDate.of(2023, 8, 2))
            .lengthMonths(84)
            .startGrade(ST1)
            .build())
        .changes(List.of(CctChange.builder()
            .type(LTFT)
            .startDate(LocalDate.of(2024, 2, 1))
            .endDate(LocalDate.of(2024, 8, 1))
            .wte(0.6)
            .countsTowardsCompletion(true)
            .notes("Cardiology rotation")
            .calendarMonths(6.0)
            .wteMonths(3.6)
            .build()))
        .result(CctResult.builder()
            .cctDate(LocalDate.of(2099, 1, 1))
            .originalProgrammeEndDate(LocalDate.of(2030, 7, 29))
            .adjustedProgrammeEndDate(LocalDate.of(2030, 7, 29))
            .totalWteMonthsCompleted(9.6)
            .monthsRemaining(74.4)
            .assumedFullTimePeriods(List.of(CctAssumedFullTimePeriod.builder()
                .startDate(LocalDate.of(2023, 8, 2))
                .endDate(LocalDate.of(2024, 1, 31))
                .months(6.0)
                .build()))
            .projection(CctProjection.builder()
                .startDate(LocalDate.of(2024, 8, 2))
                .wte(1.0)
                .calendarMonths(74.4)
                .wteMonths(74.4)
                .build())
            .build())
        .created(created)
        .lastModified(lastModified)
        .build();

    when(calculationRepository.findById(calculationId)).thenReturn(Optional.of(entity));

    Optional<CctCalculationDetailDto> result = service.getCalculation(calculationId);

    assertThat("Unexpected calculation presence.", result.isPresent(), is(true));

    CctCalculationDetailDto dto = result.get();
    assertThat("Unexpected calculation ID.", dto.id(), is(calculationId));
    assertThat("Unexpected calculation name.", dto.name(), is("Test Calculation"));
    assertThat("Unexpected created timestamp.", dto.created(), is(created));
    assertThat("Unexpected last modified timestamp.", dto.lastModified(), is(lastModified));

    CctProgrammeDetailsDto programmeDetails = dto.programmeDetails();
    assertThat("Unexpected specialty.", programmeDetails.specialty(), is("Paediatrics"));
    assertThat("Unexpected start date.", programmeDetails.startDate(),
        is(LocalDate.of(2023, 8, 2)));
    assertThat("Unexpected length.", programmeDetails.lengthMonths(), is(84));
    assertThat("Unexpected start grade.", programmeDetails.startGrade(), is(ST1));

    assertThat("Unexpected changes.", dto.changes(), is(List.of(CctCalculatedChangeDto.builder()
        .change(datedLtft())
        .calendarMonths(6.0)
        .wteMonths(3.6)
        .build())));

    CctResultDto calculationResult = dto.result();
    assertThat("Unexpected CCT date.", calculationResult.cctDate(),
        is(LocalDate.of(2099, 1, 1)));
    assertThat("Unexpected original end date.", calculationResult.originalProgrammeEndDate(),
        is(LocalDate.of(2030, 7, 29)));
    assertThat("Unexpected adjusted end date.", calculationResult.adjustedProgrammeEndDate(),
        is(LocalDate.of(2030, 7, 29)));
    assertThat("Unexpected months completed.", calculationResult.totalWteMonthsCompleted(),
        is(9.6));
    assertThat("Unexpected months remaining.", calculationResult.monthsRemaining(), is(74.4));
    assertThat("Unexpected gap count.", calculationResult.assumedFullTimePeriods(), hasSize(1));
    assertThat("Unexpected gap end date.",
        calculationResult.assumedFullTimePeriods().get(0).endDate(),
        is(LocalDate.of(2024, 1, 31)));
    assertThat("Unexpected projection start date.", calculationResult.projection().startDate(),
        is(LocalDate.of(2024, 8, 2)));
    assertThat("Unexpected projection WTE months.", calculationResult.projection().wteMonths(),
        is(74.4));
  }

  @Test
  void shouldCreateCalculationWithCalculatedChangesAndResult()
      throws MethodArgumentNotValidException {
    CctCalculationSaveRequestDto dto = request(List.of(datedLtft()));

    UUID calculationId = UUID.randomUUID();
    when(calculationRepository.insert(any(CctCalculation.class))).thenAnswer(
        inv -> inv.<CctCalculation>getArgument(0).withId(calculationId));

    CctCalculationDetailDto savedDto = service.createCalculation(dto);
    assertThat("Unexpected calculation ID.", savedDto.id(), is(calculationId));
    assertThat("Unexpected calculation name.", savedDto.name(), is("Test Calculation"));
    assertThat("Unexpected programme details.", savedDto.programmeDetails(),
        is(dto.programmeDetails()));

    CctCalculateResponseDto calculation = engine.calculate(dto.programmeDetails(),
        dto.changes());
    assertThat("Unexpected changes.", savedDto.changes(), is(calculation.changes()));
    assertThat("Unexpected result.", savedDto.result(), is(calculation.result()));
    assertThat("Unexpected CCT date.", savedDto.result().cctDate(),
        is(LocalDate.of(2030, 10, 10)));

    ArgumentCaptor<CctCalculation> entityCaptor = ArgumentCaptor.captor();
    verify(calculationRepository).insert(entityCaptor.capture());

    CctCalculation entity = entityCaptor.getValue();
    assertThat("Unexpected trainee ID.", entity.traineeId(), is(TRAINEE_ID));
    assertThat("Unexpected created timestamp.", entity.created(), nullValue());
  }

  @Test
  void shouldNotCreateCalculationWhenChangeStartsBeforeProgramme() {
    CctCalculationSaveRequestDto dto = request(List.of(CctChangeDto.builder()
        .type(LTFT)
        .startDate(LocalDate.of(2023, 8, 1))
        .endDate(LocalDate.of(2024, 7, 31))
        .wte(0.5)
        .countsTowardsCompletion(true)
        .build()));

    MethodArgumentNotValidException exception = assertThrows(MethodArgumentNotValidException.class,
        () -> service.createCalculation(dto));

    List<FieldError> fieldErrors = exception.getFieldErrors();
    assertThat("Unexpected error count.", fieldErrors, hasSize(1));

    FieldError fieldError = fieldErrors.get(0);
    assertThat("Unexpected error field.", fieldError.getField(), is("changes[0].startDate"));
    assertThat("Unexpected error message.", fieldError.getDefaultMessage(),
        is("Start date cannot be before programme start (02/08/2023)."));

    verify(calculationRepository, never()).insert(any(CctCalculation.class));
  }

  @Test
  void shouldNotCreateCalculationWhenChangeEndsAfterCctDate() {
    CctCalculationSaveRequestDto dto = request(List.of(overrunningLtft()));

    MethodArgumentNotValidException exception = assertThrows(MethodArgumentNotValidException.class,
        () -> service.createCalculation(dto));

    List<FieldError> fieldErrors = exception.getFieldErrors();
    assertThat("Unexpected error count.", fieldErrors, hasSize(1));

    FieldError fieldError = fieldErrors.get(0);
    assertThat("Unexpected error field.", fieldError.getField(), is("changes[0].endDate"));
    assertThat("Unexpected error message.", fieldError.getDefaultMessage(),
        is("End date cannot be after the current projected Completion of Training Date "
            + "(26/02/2031)."));

    verify(calculationRepository, never()).insert(any(CctCalculation.class));
  }

  @Test
  void shouldNotCreateCalculationWhenChangeStartsAfterCctDate() {
    CctCalculationSaveRequestDto dto = request(List.of(
        overrunningLtft(),
        CctChangeDto.builder()
            .type(LTFT)
            .startDate(LocalDate.of(2033, 1, 1))
            .wte(0.5)
            .countsTowardsCompletion(true)
            .build()));

    MethodArgumentNotValidException exception = assertThrows(MethodArgumentNotValidException.class,
        () -> service.createCalculation(dto));

    List<FieldError> fieldErrors = exception.getFieldErrors();
    assertThat("Unexpected error count.", fieldErrors, hasSize(2));

    FieldError fieldError = fieldErrors.get(0);
    assertThat("Unexpected error field.", fieldError.getField(), is("changes[0].endDate"));
    assertThat("Unexpected error message.", fieldError.getDefaultMessage(),
        is("End date cannot be after the current projected Completion of Training Date "
            + "(26/02/2031)."));

    fieldError = fieldErrors.get(1);
    assertThat("Unexpected error field.", fieldError.getField(), is("changes[1].startDate"));
    assertThat("Unexpected error message.", fieldError.getDefaultMessage(),
        is("Start date cannot be after the current projected Completion of Training Date "
            + "(26/02/2031)."));

    verify(calculationRepository, never()).insert(any(CctCalculation.class));
  }

  @Test
  void shouldUpdateExistingCalculationKeepingCreatedWhenOwnedByUser()
      throws MethodArgumentNotValidException {
    UUID id = UUID.randomUUID();
    Instant created = Instant.now().minus(Duration.ofDays(1));

    CctCalculation existingCalc = CctCalculation.builder()
        .id(id)
        .traineeId(TRAINEE_ID)
        .name("Test Calculation")
        .created(created)
        .lastModified(created)
        .build();
    when(calculationRepository.findById(id)).thenReturn(Optional.of(existingCalc));

    Instant modified = Instant.now();
    when(calculationRepository.save(any(CctCalculation.class))).thenAnswer(
        inv -> inv.<CctCalculation>getArgument(0).withLastModified(modified));

    CctCalculationSaveRequestDto dto = CctCalculationSaveRequestDto.builder()
        .id(id)
        .name("Test Calculation updated")
        .programmeDetails(programmeDetails())
        .changes(List.of(datedLtft()))
        .build();

    Optional<CctCalculationDetailDto> updatedDtoOptional = service.updateCalculation(id, dto);
    assertThat("Unexpected saved calculation.", updatedDtoOptional.isPresent(), is(true));

    ArgumentCaptor<CctCalculation> entityCaptor = ArgumentCaptor.captor();
    verify(calculationRepository).save(entityCaptor.capture());

    CctCalculation entity = entityCaptor.getValue();
    assertThat("Unexpected entity ID.", entity.id(), is(id));
    assertThat("Unexpected trainee ID.", entity.traineeId(), is(TRAINEE_ID));
    assertThat("Unexpected created timestamp.", entity.created(), is(created));

    CctCalculationDetailDto updatedDto = updatedDtoOptional.get();
    assertThat("Unexpected calculation ID.", updatedDto.id(), is(id));
    assertThat("Unexpected calculation name.", updatedDto.name(),
        is("Test Calculation updated"));
    assertThat("Unexpected calculation created.", updatedDto.created(), is(created));
    assertThat("Unexpected calculation last modified.", updatedDto.lastModified(),
        is(modified));

    CctCalculateResponseDto calculation = engine.calculate(dto.programmeDetails(),
        dto.changes());
    assertThat("Unexpected changes.", updatedDto.changes(), is(calculation.changes()));
    assertThat("Unexpected result.", updatedDto.result(), is(calculation.result()));
  }

  @Test
  void shouldNotUpdateExistingCalculationIfNotExists() throws MethodArgumentNotValidException {
    UUID id = UUID.randomUUID();
    CctCalculationSaveRequestDto dto = request(List.of(datedLtft()));

    when(calculationRepository.findById(any())).thenReturn(Optional.empty());

    Optional<CctCalculationDetailDto> updatedDtoOptional = service.updateCalculation(id, dto);

    assertThat("Unexpected saved calculation.", updatedDtoOptional.isPresent(), is(false));
    verify(calculationRepository).findById(id);
    verifyNoMoreInteractions(calculationRepository);
  }

  @Test
  void shouldThrowExceptionUpdatingCalculationWhenNotBelongsToUser() {
    UUID id = UUID.randomUUID();
    CctCalculation existingCalc = CctCalculation.builder()
        .id(id)
        .traineeId(UUID.randomUUID().toString())
        .build();
    when(calculationRepository.findById(id)).thenReturn(Optional.of(existingCalc));

    CctCalculationSaveRequestDto dto = request(List.of(datedLtft()));

    assertThrows(NotRecordOwnerException.class, () -> service.updateCalculation(id, dto));
    verify(calculationRepository, never()).save(any());
  }

  @Test
  void shouldNotUpdateExistingCalculationWhenChangeDatesInvalid() {
    UUID id = UUID.randomUUID();
    CctCalculation existingCalc = CctCalculation.builder()
        .id(id)
        .traineeId(TRAINEE_ID)
        .build();
    when(calculationRepository.findById(id)).thenReturn(Optional.of(existingCalc));

    CctCalculationSaveRequestDto dto = request(List.of(overrunningLtft()));

    MethodArgumentNotValidException exception = assertThrows(MethodArgumentNotValidException.class,
        () -> service.updateCalculation(id, dto));

    List<FieldError> fieldErrors = exception.getFieldErrors();
    assertThat("Unexpected error count.", fieldErrors, hasSize(1));
    assertThat("Unexpected error field.", fieldErrors.get(0).getField(),
        is("changes[0].endDate"));

    verify(calculationRepository, never()).save(any());
  }

  @Test
  void shouldNotDeleteCalculationIfNotExists() {
    UUID id = UUID.randomUUID();

    when(calculationRepository.findById(any())).thenReturn(Optional.empty());

    boolean result = service.deleteCalculation(id);

    assertThat("Unexpected delete calculation result.", result, is(false));
    verify(calculationRepository).findById(id);
    verifyNoMoreInteractions(calculationRepository);
  }

  @Test
  void shouldDeleteCalculationIfExists() {
    UUID id = UUID.randomUUID();

    CctCalculation entity = CctCalculation.builder()
        .id(id)
        .traineeId(TRAINEE_ID)
        .build();
    when(calculationRepository.findById(any())).thenReturn(Optional.of(entity));

    boolean result = service.deleteCalculation(id);

    assertThat("Unexpected delete calculation result.", result, is(true));
    verify(calculationRepository).findById(id);
    verify(calculationRepository).deleteById(id);
    verifyNoMoreInteractions(calculationRepository);
  }

  @Test
  void shouldMoveCctCalculationsWhenFound() {
    String fromTraineeId = "40";
    String toTraineeId = "50";
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();

    CctCalculation calc1 = CctCalculation.builder()
        .id(id1)
        .traineeId(fromTraineeId)
        .name("Test Calculation 1")
        .build();

    CctCalculation calc2 = CctCalculation.builder()
        .id(id2)
        .traineeId(fromTraineeId)
        .name("Test Calculation 2")
        .build();

    when(calculationRepository.findByTraineeIdOrderByLastModified(fromTraineeId))
        .thenReturn(List.of(calc1, calc2));

    service.moveCalculations(fromTraineeId, toTraineeId);

    verify(calculationRepository).save(calc1.withTraineeId(toTraineeId));
    verify(calculationRepository).save(calc2.withTraineeId(toTraineeId));
  }

  @Test
  void shouldNotSaveCalculationsWhenNoneMoved() {
    String fromTraineeId = "40";
    String toTraineeId = "50";

    when(calculationRepository.findByTraineeIdOrderByLastModified(fromTraineeId))
        .thenReturn(List.of());

    service.moveCalculations(fromTraineeId, toTraineeId);

    verify(calculationRepository, never()).save(any());
  }

  private static CctCalculationSaveRequestDto request(List<CctChangeDto> changes) {
    return CctCalculationSaveRequestDto.builder()
        .name("Test Calculation")
        .programmeDetails(programmeDetails())
        .changes(changes)
        .build();
  }

  private static CctProgrammeDetailsDto programmeDetails() {
    return CctProgrammeDetailsDto.builder()
        .specialty("Paediatrics")
        .startDate(LocalDate.of(2023, 8, 2))
        .lengthMonths(84)
        .startGrade(ST1)
        .additionalMonths(0)
        .acceleratedMonths(0)
        .build();
  }

  private static CctChangeDto datedLtft() {
    return CctChangeDto.builder()
        .type(LTFT)
        .startDate(LocalDate.of(2024, 2, 1))
        .endDate(LocalDate.of(2024, 8, 1))
        .wte(0.6)
        .countsTowardsCompletion(true)
        .notes("Cardiology rotation")
        .build();
  }

  private static CctChangeDto overrunningLtft() {
    return CctChangeDto.builder()
        .type(LTFT)
        .startDate(LocalDate.of(2030, 1, 1))
        .endDate(LocalDate.of(2032, 12, 31))
        .wte(0.5)
        .countsTowardsCompletion(true)
        .build();
  }
}
