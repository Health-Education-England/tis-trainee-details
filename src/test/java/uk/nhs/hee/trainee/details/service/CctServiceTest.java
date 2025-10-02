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
import static uk.nhs.hee.trainee.details.service.CctService.WTE_EPSILON;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import uk.nhs.hee.trainee.details.dto.CctCalculationDetailDto;
import uk.nhs.hee.trainee.details.dto.CctCalculationDetailDto.CctChangeDto;
import uk.nhs.hee.trainee.details.dto.CctCalculationDetailDto.CctProgrammeMembershipDto;
import uk.nhs.hee.trainee.details.dto.TraineeIdentity;
import uk.nhs.hee.trainee.details.exception.NotRecordOwnerException;
import uk.nhs.hee.trainee.details.mapper.CctMapperImpl;
import uk.nhs.hee.trainee.details.model.CctCalculation;
import uk.nhs.hee.trainee.details.model.CctCalculation.CctChange;
import uk.nhs.hee.trainee.details.model.CctCalculation.CctProgrammeMembership;
import uk.nhs.hee.trainee.details.repository.CctCalculationRepository;

class CctServiceTest {

  private static final String TRAINEE_ID = UUID.randomUUID().toString();

  private CctService service;
  private CctCalculationRepository calculationRepository;

  @BeforeEach
  void setUp() {
    TraineeIdentity traineeIdentity = new TraineeIdentity();
    traineeIdentity.setTraineeId(TRAINEE_ID);

    calculationRepository = mock(CctCalculationRepository.class);

    service = new CctService(traineeIdentity, calculationRepository, new CctMapperImpl());
  }

  @Test
  void shouldReturnEmptyGettingCalculationsWhenNotFound() {
    when(calculationRepository.findByTraineeIdOrderByLastModified(TRAINEE_ID)).thenReturn(
        List.of());

    List<CctCalculationDetailDto> result = service.getCalculations();

    assertThat("Unexpected calculation details count.", result.size(), is(0));
  }

  @Test
  void shouldGetCalculationsWhenFound() {
    UUID calculationId1 = UUID.randomUUID();
    UUID pmId1 = UUID.randomUUID();
    Instant created1 = Instant.now().minus(Duration.ofDays(1));
    Instant lastModified1 = Instant.now().plus(Duration.ofDays(1));

    CctCalculation entity1 = CctCalculation.builder()
        .id(calculationId1)
        .traineeId(TRAINEE_ID)
        .name("Test Calculation 1")
        .programmeMembership(CctProgrammeMembership.builder()
            .id(pmId1)
            .startDate(LocalDate.EPOCH)
            .endDate(LocalDate.EPOCH.plusYears(1))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .created(created1)
        .lastModified(lastModified1)
        .changes(List.of(CctChange.builder()
            .startDate(LocalDate.EPOCH.plusMonths(1))
            .wte(0.5)
            .build()))
        .build();

    UUID calculationId2 = UUID.randomUUID();
    UUID pmId2 = UUID.randomUUID();
    Instant created2 = Instant.now().minus(Duration.ofDays(2));
    Instant lastModified2 = Instant.now().plus(Duration.ofDays(2));

    CctCalculation entity2 = CctCalculation.builder()
        .id(calculationId2)
        .traineeId(TRAINEE_ID)
        .name("Test Calculation 2")
        .programmeMembership(CctProgrammeMembership.builder()
            .id(pmId2)
            .startDate(LocalDate.EPOCH)
            .endDate(LocalDate.EPOCH.plusYears(1))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .changes(List.of(CctChange.builder()
            .startDate(LocalDate.EPOCH)
            .wte(0.75)
            .build()))
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
    assertThat("Unexpected PM ID.", dto1.programmeMembership().id(), is(pmId1));
    assertThat("Unexpected CCT date.", dto1.cctDate(), is(service.calculateCctDate(entity1)));
    assertThat("Unexpected created timestamp.", dto1.created(), is(created1));
    assertThat("Unexpected last modified timestamp.", dto1.lastModified(), is(lastModified1));

    CctCalculationDetailDto dto2 = result.get(1);
    assertThat("Unexpected calculation ID.", dto2.id(), is(calculationId2));
    assertThat("Unexpected calculation name.", dto2.name(), is("Test Calculation 2"));
    assertThat("Unexpected PM ID.", dto2.programmeMembership().id(), is(pmId2));
    assertThat("Unexpected CCT date.", dto2.cctDate(), is(service.calculateCctDate(entity2)));
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
  void shouldGetCalculationWhenBelongsToUser() {
    UUID calculationId = UUID.randomUUID();
    UUID pmId = UUID.randomUUID();

    Instant created = Instant.now().minus(Duration.ofDays(1));
    Instant lastModified = Instant.now().plus(Duration.ofDays(1));

    CctCalculation entity = CctCalculation.builder()
        .id(calculationId)
        .traineeId(TRAINEE_ID)
        .name("Test Calculation")
        .programmeMembership(CctProgrammeMembership.builder()
            .id(pmId)
            .name("Test Programme")
            .startDate(LocalDate.EPOCH)
            .endDate(LocalDate.EPOCH.plusYears(1))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .managingDeanery("Test Deanery")
            .build())
        .changes(List.of(
            CctChange.builder().type(LTFT).startDate(LocalDate.EPOCH.plusMonths(6))
                .wte(0.5).build(),
            CctChange.builder().type(LTFT).startDate(LocalDate.EPOCH.plusMonths(13))
                .wte(0.75).build()
        ))
        .created(created)
        .lastModified(lastModified)
        .build();

    when(calculationRepository.findById(calculationId)).thenReturn(Optional.of(entity));

    Optional<CctCalculationDetailDto> result = service.getCalculation(calculationId);

    assertThat("Unexpected calculation presence.", result.isPresent(), is(true));

    CctCalculationDetailDto dto = result.get();
    assertThat("Unexpected calculation ID.", dto.id(), is(calculationId));
    assertThat("Unexpected calculation name.", dto.name(), is("Test Calculation"));
    assertThat("Unexpected CCT date.", dto.cctDate(), is(service.calculateCctDate(entity)));
    assertThat("Unexpected created timestamp.", dto.created(), is(created));
    assertThat("Unexpected last modified timestamp.", dto.lastModified(), is(lastModified));

    CctProgrammeMembershipDto pm = dto.programmeMembership();
    assertThat("Unexpected PM ID.", pm.id(), is(pmId));
    assertThat("Unexpected PM name.", pm.name(), is("Test Programme"));
    assertThat("Unexpected PM start date.", pm.startDate(), is(LocalDate.EPOCH));
    assertThat("Unexpected PM end date.", pm.endDate(), is(LocalDate.EPOCH.plusYears(1)));
    assertThat("Unexpected PM WTE.", pm.wte(), is(1.0));
    assertThat("Unexpected PM DBC.", pm.designatedBodyCode(), is("testDbc"));
    assertThat("Unexpected PM deanery.", pm.managingDeanery(), is("Test Deanery"));

    List<CctChangeDto> changes = dto.changes();
    assertThat("Unexpected change count.", changes.size(), is(2));

    CctChangeDto change1 = changes.get(0);
    assertThat("Unexpected change type.", change1.type(), is(LTFT));
    assertThat("Unexpected change type.", change1.startDate(),
        is(LocalDate.EPOCH.plusMonths(6)));
    assertThat("Unexpected change type.", change1.wte(), is(0.5));

    CctChangeDto change2 = changes.get(1);
    assertThat("Unexpected change type.", change2.type(), is(LTFT));
    assertThat("Unexpected change type.", change2.startDate(),
        is(LocalDate.EPOCH.plusMonths(13)));
    assertThat("Unexpected change type.", change2.wte(), is(0.75));
  }

  @Test
  void shouldCreateCalculation() {
    UUID pmId = UUID.randomUUID();

    CctCalculationDetailDto dto = CctCalculationDetailDto.builder()
        .name("Test Calculation")
        .programmeMembership(CctProgrammeMembershipDto.builder()
            .id(pmId)
            .name("Test Programme")
            .startDate(LocalDate.EPOCH)
            .endDate(LocalDate.EPOCH.plusYears(1))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .changes(List.of(
            CctChangeDto.builder().type(LTFT).startDate(LocalDate.EPOCH.plusMonths(6))
                .wte(0.5).build(),
            CctChangeDto.builder().type(LTFT).startDate(LocalDate.EPOCH.plusMonths(13))
                .wte(0.75).build()
        ))
        .build();

    UUID calculationId = UUID.randomUUID();
    when(calculationRepository.insert(any(CctCalculation.class))).thenAnswer(inv -> {
      CctCalculation entity = inv.getArgument(0);
      CctProgrammeMembership pm = entity.programmeMembership();
      CctChange change1 = entity.changes().get(0);
      CctChange change2 = entity.changes().get(1);

      return CctCalculation.builder()
          .id(calculationId)
          .traineeId(entity.traineeId())
          .name(entity.name())
          .programmeMembership(CctProgrammeMembership.builder()
              .id(pm.id())
              .name(pm.name())
              .startDate(pm.startDate())
              .endDate(pm.endDate())
              .wte(pm.wte())
              .designatedBodyCode("testDbc")
              .managingDeanery("Test Deanery")
              .build())
          .changes(List.of(
              CctChange.builder()
                  .type(change1.type())
                  .startDate(change1.startDate())
                  .wte(change1.wte())
                  .build(),
              CctChange.builder()
                  .type(change2.type())
                  .startDate(change2.startDate())
                  .wte(change2.wte()).build()
          ))
          .build();
    });

    CctCalculationDetailDto savedDto = service.createCalculation(dto);
    assertThat("Unexpected calculation ID.", savedDto.id(), is(calculationId));
    assertThat("Unexpected calculation name.", savedDto.name(), is("Test Calculation"));
    assertThat("Unexpected CCT date.", savedDto.cctDate(),
        is(service.calculateCctDate(savedDto)));

    CctProgrammeMembershipDto pm = savedDto.programmeMembership();
    assertThat("Unexpected PM ID.", pm.id(), is(pmId));
    assertThat("Unexpected PM name.", pm.name(), is("Test Programme"));
    assertThat("Unexpected PM start date.", pm.startDate(), is(LocalDate.EPOCH));
    assertThat("Unexpected PM end date.", pm.endDate(), is(LocalDate.EPOCH.plusYears(1)));
    assertThat("Unexpected PM WTE.", pm.wte(), is(1.0));
    assertThat("Unexpected PM DBC.", pm.designatedBodyCode(), is("testDbc"));
    assertThat("Unexpected PM deanery.", pm.managingDeanery(), is("Test Deanery"));

    List<CctChangeDto> changes = savedDto.changes();
    assertThat("Unexpected change count.", changes.size(), is(2));

    CctChangeDto change1 = changes.get(0);
    assertThat("Unexpected change type.", change1.type(), is(LTFT));
    assertThat("Unexpected change type.", change1.startDate(),
        is(LocalDate.EPOCH.plusMonths(6)));
    assertThat("Unexpected change type.", change1.wte(), is(0.5));

    CctChangeDto change2 = changes.get(1);
    assertThat("Unexpected change type.", change2.type(), is(LTFT));
    assertThat("Unexpected change type.", change2.startDate(),
        is(LocalDate.EPOCH.plusMonths(13)));
    assertThat("Unexpected change type.", change2.wte(), is(0.75));
  }

  @Test
  void shouldInsertCctDateAndIncludeOtherDetailsInReturnedDto() {
    UUID pmId = UUID.randomUUID();

    UUID calculationId = UUID.randomUUID();
    CctCalculationDetailDto dto = CctCalculationDetailDto.builder()
        .id(calculationId)
        .name("Test Calculation")
        .programmeMembership(CctProgrammeMembershipDto.builder()
            .id(pmId)
            .name("Test Programme")
            .startDate(LocalDate.EPOCH)
            .endDate(LocalDate.EPOCH.plusYears(1))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .managingDeanery("Test Deanery")
            .build())
        .changes(List.of(
            CctChangeDto.builder().type(LTFT).startDate(LocalDate.EPOCH.plusMonths(6))
                .wte(0.5).build(),
            CctChangeDto.builder().type(LTFT).startDate(LocalDate.EPOCH.plusMonths(13))
                .wte(0.75).build()
        ))
        .build();

    Optional<CctCalculationDetailDto> calculatedDtoOptional = service.insertCctDate(dto);

    assertThat("Unexpected CCT calculation.", calculatedDtoOptional.isPresent(), is(true));
    CctCalculationDetailDto calculatedDto = calculatedDtoOptional.get();

    assertThat("Unexpected calculation ID.", calculatedDto.id(), is(calculationId));
    assertThat("Unexpected calculation name.", calculatedDto.name(), is("Test Calculation"));
    assertThat("Unexpected calculated CCT date.", calculatedDto.cctDate(),
        is(service.calculateCctDate(calculatedDto)));

    CctProgrammeMembershipDto pm = calculatedDto.programmeMembership();
    assertThat("Unexpected PM ID.", pm.id(), is(pmId));
    assertThat("Unexpected PM name.", pm.name(), is("Test Programme"));
    assertThat("Unexpected PM start date.", pm.startDate(), is(LocalDate.EPOCH));
    assertThat("Unexpected PM end date.", pm.endDate(), is(LocalDate.EPOCH.plusYears(1)));
    assertThat("Unexpected PM WTE.", pm.wte(), is(1.0));
    assertThat("Unexpected PM DBC.", pm.designatedBodyCode(), is("testDbc"));
    assertThat("Unexpected PM deanery.", pm.managingDeanery(), is("Test Deanery"));

    List<CctChangeDto> changes = calculatedDto.changes();
    assertThat("Unexpected change count.", changes.size(), is(2));

    CctChangeDto change1 = changes.get(0);
    assertThat("Unexpected change type.", change1.type(), is(LTFT));
    assertThat("Unexpected change type.", change1.startDate(),
        is(LocalDate.EPOCH.plusMonths(6)));
    assertThat("Unexpected change type.", change1.wte(), is(0.5));

    CctChangeDto change2 = changes.get(1);
    assertThat("Unexpected change type.", change2.type(), is(LTFT));
    assertThat("Unexpected change type.", change2.startDate(),
        is(LocalDate.EPOCH.plusMonths(13)));
    assertThat("Unexpected change type.", change2.wte(), is(0.75));
  }

  @Test
  void shouldUpdateExistingCalculationIfExistsAndOwnedByUser()
      throws MethodArgumentNotValidException {
    UUID pmId = UUID.randomUUID();
    UUID id = UUID.randomUUID();
    UUID changeId1 = UUID.randomUUID();
    UUID changeId2 = UUID.randomUUID();
    Instant created = Instant.now();

    CctCalculationDetailDto dto = CctCalculationDetailDto.builder()
        .id(id)
        .created(created)
        .name("Test Calculation")
        .programmeMembership(CctProgrammeMembershipDto.builder()
            .id(pmId)
            .name("Test Programme")
            .startDate(LocalDate.EPOCH)
            .endDate(LocalDate.EPOCH.plusYears(1))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .changes(List.of(
            CctChangeDto.builder().id(changeId1).type(LTFT).startDate(LocalDate.EPOCH.plusMonths(6))
                .wte(0.5).build(),
            CctChangeDto.builder().type(LTFT).startDate(LocalDate.EPOCH.plusMonths(13)).wte(0.75)
                .build()
        ))
        .build();

    CctCalculation existingCalc = CctCalculation.builder()
        .id(id)
        .traineeId(TRAINEE_ID)
        .programmeMembership(CctProgrammeMembership.builder()
            .startDate(LocalDate.EPOCH)
            .endDate(LocalDate.EPOCH.plusYears(1))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .changes(List.of(
            CctChange.builder().id(changeId1).type(LTFT).startDate(LocalDate.MIN).wte(0.5).build()))
        .build();
    when(calculationRepository.findById(any())).thenReturn(Optional.of(existingCalc));

    Instant modified = created.plusSeconds(1);
    when(calculationRepository.save(any(CctCalculation.class))).thenAnswer(inv -> {
      CctCalculation entity = inv.getArgument(0);
      CctProgrammeMembership pm = entity.programmeMembership();
      CctChange change1 = entity.changes().get(0);
      CctChange change2 = entity.changes().get(1);

      return CctCalculation.builder()
          .id(id)
          .created(created)
          .lastModified(modified)
          .traineeId(entity.traineeId())
          .name(entity.name())
          .programmeMembership(CctProgrammeMembership.builder()
              .id(pm.id())
              .name(pm.name())
              .startDate(pm.startDate())
              .endDate(pm.endDate())
              .wte(pm.wte())
              .designatedBodyCode(pm.designatedBodyCode())
              .managingDeanery("Test Deanery")
              .build())
          .changes(List.of(
              CctChange.builder()
                  .id(changeId1)
                  .type(change1.type())
                  .startDate(change1.startDate())
                  .wte(change1.wte())
                  .build(),
              CctChange.builder()
                  .id(changeId2)
                  .type(change2.type())
                  .startDate(change2.startDate())
                  .wte(change2.wte()).build()
          ))
          .build();
    });

    Optional<CctCalculationDetailDto> updatedDtoOptional = service.updateCalculation(id, dto);
    assertThat("Unexpected saved calculation.", updatedDtoOptional.isPresent(), is(true));

    CctCalculationDetailDto updatedDto = updatedDtoOptional.get();
    assertThat("Unexpected calculation ID.", updatedDto.id(), is(id));
    assertThat("Unexpected calculation created.", updatedDto.created(), is(created));
    assertThat("Unexpected calculation last modified.", updatedDto.lastModified(),
        is(modified));
    assertThat("Unexpected calculation name.", updatedDto.name(), is("Test Calculation"));
    assertThat("Unexpected CCT date.", updatedDto.cctDate(),
        is(service.calculateCctDate(updatedDto)));

    CctProgrammeMembershipDto pm = updatedDto.programmeMembership();
    assertThat("Unexpected PM ID.", pm.id(), is(pmId));
    assertThat("Unexpected PM name.", pm.name(), is("Test Programme"));
    assertThat("Unexpected PM start date.", pm.startDate(), is(LocalDate.EPOCH));
    assertThat("Unexpected PM end date.", pm.endDate(), is(LocalDate.EPOCH.plusYears(1)));
    assertThat("Unexpected PM WTE.", pm.wte(), is(1.0));
    assertThat("Unexpected PM DBC.", pm.designatedBodyCode(), is("testDbc"));
    assertThat("Unexpected PM deanery.", pm.managingDeanery(), is("Test Deanery"));

    List<CctChangeDto> changes = updatedDto.changes();
    assertThat("Unexpected change count.", changes.size(), is(2));

    CctChangeDto change1 = changes.get(0);
    assertThat("Unexpected change ID.", change1.id(), is(changeId1));
    assertThat("Unexpected change type.", change1.type(), is(LTFT));
    assertThat("Unexpected change type.", change1.startDate(),
        is(LocalDate.EPOCH.plusMonths(6)));
    assertThat("Unexpected change type.", change1.wte(), is(0.5));

    CctChangeDto change2 = changes.get(1);
    assertThat("Unexpected change ID.", change2.id(), is(changeId2));
    assertThat("Unexpected change type.", change2.type(), is(LTFT));
    assertThat("Unexpected change type.", change2.startDate(),
        is(LocalDate.EPOCH.plusMonths(13)));
    assertThat("Unexpected change type.", change2.wte(), is(0.75));
  }

  @Test
  void shouldNotUpdateExistingCalculationIfNotExists() throws MethodArgumentNotValidException {
    UUID pmId = UUID.randomUUID();
    UUID id = UUID.randomUUID();
    Instant created = Instant.now();

    CctCalculationDetailDto dto = CctCalculationDetailDto.builder()
        .id(id)
        .created(created)
        .name("Test Calculation")
        .programmeMembership(CctProgrammeMembershipDto.builder()
            .id(pmId)
            .name("Test Programme")
            .startDate(LocalDate.EPOCH)
            .endDate(LocalDate.EPOCH.plusYears(1))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .changes(List.of(
            CctChangeDto.builder().type(LTFT).startDate(LocalDate.EPOCH.plusMonths(6))
                .wte(0.5).build(),
            CctChangeDto.builder().type(LTFT).startDate(LocalDate.EPOCH.plusMonths(13))
                .wte(0.75).build()
        ))
        .build();

    when(calculationRepository.findById(any())).thenReturn(Optional.empty());

    Optional<CctCalculationDetailDto> updatedDtoOptional = service.updateCalculation(id, dto);

    assertThat("Unexpected saved calculation.", updatedDtoOptional.isPresent(), is(false));
    verify(calculationRepository).findById(id);
    verifyNoMoreInteractions(calculationRepository);
  }

  @Test
  void shouldNotUpdateExistingCalculationIfChangeIdsInvalid() {
    UUID pmId = UUID.randomUUID();
    UUID id = UUID.randomUUID();
    UUID changeId1 = UUID.randomUUID();
    UUID changeId2 = UUID.randomUUID();
    UUID changeId3 = UUID.randomUUID();

    CctCalculationDetailDto dto = CctCalculationDetailDto.builder()
        .id(id)
        .name("Test Calculation")
        .programmeMembership(CctProgrammeMembershipDto.builder()
            .id(pmId)
            .name("Test Programme")
            .startDate(LocalDate.EPOCH)
            .endDate(LocalDate.EPOCH.plusYears(1))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .changes(List.of(
            CctChangeDto.builder().id(changeId1).type(LTFT)
                .startDate(LocalDate.EPOCH.plusMonths(1)).wte(0.5)
                .build(),
            CctChangeDto.builder().id(changeId2).type(LTFT)
                .startDate(LocalDate.EPOCH.plusMonths(2)).wte(1.0)
                .build(),
            CctChangeDto.builder().id(changeId3).type(LTFT)
                .startDate(LocalDate.EPOCH.plusMonths(3)).wte(0.8)
                .build(),
            CctChangeDto.builder().id(changeId3).type(LTFT)
                .startDate(LocalDate.EPOCH.plusMonths(4)).wte(1.0)
                .build(),
            CctChangeDto.builder().type(LTFT)
                .startDate(LocalDate.EPOCH.plusMonths(5)).wte(0.5)
                .build()
        ))
        .build();

    CctCalculation existingCalc = CctCalculation.builder()
        .id(id)
        .traineeId(TRAINEE_ID)
        .programmeMembership(CctProgrammeMembership.builder()
            .startDate(LocalDate.EPOCH)
            .endDate(LocalDate.EPOCH.plusYears(1))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .changes(List.of(
            CctChange.builder().id(changeId1).type(LTFT).startDate(LocalDate.MIN).wte(0.5).build()))
        .build();
    when(calculationRepository.findById(any())).thenReturn(Optional.of(existingCalc));

    MethodArgumentNotValidException exception = assertThrows(MethodArgumentNotValidException.class,
        () -> service.updateCalculation(id, dto));

    List<FieldError> fieldErrors = exception.getFieldErrors();
    assertThat("Unexpected error count.", fieldErrors, hasSize(4));

    FieldError fieldError = fieldErrors.get(0);
    assertThat("Unexpected error field.", fieldError.getField(), is("changes[1].id"));
    assertThat("Unexpected error message.", fieldError.getDefaultMessage(),
        is("must be null or match existing value"));

    fieldError = fieldErrors.get(1);
    assertThat("Unexpected error field.", fieldError.getField(), is("changes[2].id"));
    assertThat("Unexpected error message.", fieldError.getDefaultMessage(),
        is("must be null or match existing value"));

    fieldError = fieldErrors.get(2);
    assertThat("Unexpected error field.", fieldError.getField(), is("changes[3].id"));
    assertThat("Unexpected error message.", fieldError.getDefaultMessage(),
        is("must be null or match existing value"));

    fieldError = fieldErrors.get(3);
    assertThat("Unexpected error field.", fieldError.getField(), is("changes[3].id"));
    assertThat("Unexpected error message.", fieldError.getDefaultMessage(), is("must be unique"));

    verify(calculationRepository, never()).save(any());
  }

  @Test
  void shouldReturnNullCctDateIfNullEntity() {
    assertThat("Unexpected CCT date.", service.calculateCctDate((CctCalculation) null),
        is(nullValue()));
  }

  @Test
  void shouldReturnNullCctDateIfInvalidChangeStartDate() {
    LocalDate pmEndDate = LocalDate.EPOCH.plusMonths(12);
    CctCalculation entity = CctCalculation.builder()
        .traineeId(TRAINEE_ID)
        .programmeMembership(CctProgrammeMembership.builder()
            .startDate(LocalDate.EPOCH)
            .endDate(pmEndDate)
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .changes(List.of(
            CctChange.builder().type(LTFT).startDate(pmEndDate.plusDays(1))
                .wte(0.5).build()))
        .build();
    assertThat("Unexpected CCT date.", service.calculateCctDate(entity),
        is(nullValue()));
  }

  @Test
  void shouldIgnoreChangeStartDateBeforePmStartDateForCctDate() {
    LocalDate pmStartDate = LocalDate.EPOCH;
    CctCalculation entityEarlyChangeStartDate = CctCalculation.builder()
        .traineeId(TRAINEE_ID)
        .programmeMembership(CctProgrammeMembership.builder()
            .startDate(pmStartDate)
            .endDate(LocalDate.EPOCH.plusYears(1))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .changes(List.of(
            CctChange.builder().type(LTFT).startDate(LocalDate.MIN)
                .wte(0.5).build()))
        .build();

    CctCalculation entityNormalStartDate = CctCalculation.builder()
        .traineeId(TRAINEE_ID)
        .programmeMembership(CctProgrammeMembership.builder()
            .startDate(pmStartDate)
            .endDate(LocalDate.EPOCH.plusYears(1))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .changes(List.of(
            CctChange.builder().type(LTFT).startDate(pmStartDate)
                .wte(0.5).build()))
        .build();
    assertThat("Unexpected CCT date.", service.calculateCctDate(entityEarlyChangeStartDate),
        is(service.calculateCctDate(entityNormalStartDate)));
  }

  @Test
  void shouldOrderChangesByStartDateForCctDate() {
    LocalDate pmStartDate = LocalDate.EPOCH;
    CctCalculation entity = CctCalculation.builder()
        .traineeId(TRAINEE_ID)
        .programmeMembership(CctProgrammeMembership.builder()
            .startDate(pmStartDate)
            .endDate(LocalDate.EPOCH.plusYears(1))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .changes(List.of(
            CctChange.builder().type(LTFT).startDate(pmStartDate.plusMonths(3))
                .wte(0.5).build(),
            CctChange.builder().type(LTFT).startDate(pmStartDate.plusMonths(9))
                .wte(1.0).build(),
            CctChange.builder().type(LTFT).startDate(pmStartDate.plusMonths(6))
                .wte(0.75).build()))
        .build();

    CctCalculation entityChangesReordered = CctCalculation.builder()
        .traineeId(TRAINEE_ID)
        .programmeMembership(CctProgrammeMembership.builder()
            .startDate(pmStartDate)
            .endDate(LocalDate.EPOCH.plusYears(1))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .changes(List.of(
            CctChange.builder().type(LTFT).startDate(pmStartDate.plusMonths(9))
                .wte(1.0).build(),
            CctChange.builder().type(LTFT).startDate(pmStartDate.plusMonths(3))
                .wte(0.5).build(),
            CctChange.builder().type(LTFT).startDate(pmStartDate.plusMonths(6))
                .wte(0.75).build()))
        .build();

    assertThat("Unexpected CCT date.", service.calculateCctDate(entity),
        is(service.calculateCctDate(entityChangesReordered)));
  }

  @Test
  void shouldReturnNullCctDateIfAnyChangeHasTooSmallWte() {
    LocalDate pmStartDate = LocalDate.EPOCH;
    CctCalculation entity = CctCalculation.builder()
        .traineeId(TRAINEE_ID)
        .programmeMembership(CctProgrammeMembership.builder()
            .startDate(pmStartDate)
            .endDate(LocalDate.EPOCH.plusYears(1))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .changes(List.of(
            CctChange.builder().type(LTFT).startDate(pmStartDate.plusMonths(3))
                .wte(WTE_EPSILON - 0.001).build()))
        .build();

    assertThat("Unexpected CCT date.", service.calculateCctDate(entity),
        is(nullValue()));
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.8, 0.7, 0.6, 0.5, 0.25})
  void shouldCalculateCctDate(double changeWte) {
    LocalDate pmStartDate = LocalDate.of(2024, 9, 25);
    CctCalculation entity = CctCalculation.builder()
        .traineeId(TRAINEE_ID)
        .programmeMembership(CctProgrammeMembership.builder()
            .startDate(pmStartDate)
            .endDate(LocalDate.of(2028, 1, 1))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .changes(List.of(
            CctChange.builder().type(LTFT).startDate(pmStartDate)
                .wte(changeWte).build()))
        .build();

    //examples from front-end tests
    LocalDate expectedCctDate = switch ((int) (changeWte * 100)) {
      case 80 -> LocalDate.of(2028, 10, 26);
      case 70 -> LocalDate.of(2029, 5, 27);
      case 60 -> LocalDate.of(2030, 3, 7);
      case 50 -> LocalDate.of(2031, 4, 8);
      case 25 -> LocalDate.of(2037, 10, 19);
      default -> LocalDate.MIN;
    };
    assertThat("Unexpected CCT date.", service.calculateCctDate(entity),
        is(expectedCctDate));
  }

  @Test
  void shouldCalcCctDate2() {
    //don't ask why these are the way they are, ask the FE :>
    LocalDate pmStartDate = LocalDate.of(2024, 12, 3).plusWeeks(16);
    LocalDate pmEndDate = LocalDate.of(2024, 12, 3).plusDays(1701);
    CctCalculation entity = CctCalculation.builder()
        .traineeId(TRAINEE_ID)
        .programmeMembership(CctProgrammeMembership.builder()
            .startDate(pmStartDate)
            .endDate(pmEndDate)
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .changes(List.of(
            CctChange.builder().type(LTFT).startDate(pmStartDate)
                .wte(0.5).build()))
        .build();

    assertThat("Unexpected CCT date.", service.calculateCctDate(entity),
        is(LocalDate.of(2033, 12, 6)));
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
        .programmeMembership(CctProgrammeMembership.builder()
            .startDate(LocalDate.EPOCH)
            .endDate(LocalDate.EPOCH.plusYears(1))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .changes(List.of(
            CctChange.builder().type(LTFT).startDate(LocalDate.MIN)
                .wte(0.5).build()))
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

    Map<String, Integer> movedStats = service.moveCalculations(fromTraineeId, toTraineeId);

    Map<String, Integer> expectedMap = Map.of("cct", 2);
    assertThat("Unexpected moved CCT count.", movedStats, is(expectedMap));

    verify(calculationRepository).save(calc1.withTraineeId(toTraineeId));
    verify(calculationRepository).save(calc2.withTraineeId(toTraineeId));
  }

  @Test
  void shouldNotSaveCalculationsWhenNoneMoved() {
    String fromTraineeId = "40";
    String toTraineeId = "50";

    when(calculationRepository.findByTraineeIdOrderByLastModified(fromTraineeId))
        .thenReturn(List.of());

    Map<String, Integer> movedStats = service.moveCalculations(fromTraineeId, toTraineeId);

    Map<String, Integer> expectedMap = Map.of("cct", 0);
    assertThat("Unexpected moved CCT count.", movedStats, is(expectedMap));

    verify(calculationRepository, never()).save(any());
  }
}
