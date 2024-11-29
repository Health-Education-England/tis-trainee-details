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
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.LTFT;
import static uk.nhs.hee.trainee.details.mapper.CctMapper.PLACEHOLDER_CCT_DATE;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.trainee.details.dto.CctCalculationDetailDto;
import uk.nhs.hee.trainee.details.dto.CctCalculationDetailDto.CctChangeDto;
import uk.nhs.hee.trainee.details.dto.CctCalculationDetailDto.CctProgrammeMembershipDto;
import uk.nhs.hee.trainee.details.dto.CctCalculationSummaryDto;
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

    List<CctCalculationSummaryDto> result = service.getCalculations();

    assertThat("Unexpected calculation summary count.", result.size(), is(0));
  }

  @Test
  void shouldGetCalculationsWhenFound() {
    ObjectId calculationId1 = ObjectId.get();
    UUID pmId1 = UUID.randomUUID();
    Instant created1 = Instant.now().minus(Duration.ofDays(1));
    Instant lastModified1 = Instant.now().plus(Duration.ofDays(1));

    CctCalculation entity1 = CctCalculation.builder()
        .id(calculationId1)
        .traineeId(TRAINEE_ID)
        .name("Test Calculation 1")
        .programmeMembership(CctProgrammeMembership.builder()
            .id(pmId1)
            .build())
        .created(created1)
        .lastModified(lastModified1)
        .build();

    ObjectId calculationId2 = ObjectId.get();
    UUID pmId2 = UUID.randomUUID();
    Instant created2 = Instant.now().minus(Duration.ofDays(2));
    Instant lastModified2 = Instant.now().plus(Duration.ofDays(2));

    CctCalculation entity2 = CctCalculation.builder()
        .id(calculationId2)
        .traineeId(TRAINEE_ID)
        .name("Test Calculation 2")
        .programmeMembership(CctProgrammeMembership.builder()
            .id(pmId2)
            .build())
        .created(created2)
        .lastModified(lastModified2)
        .build();

    when(calculationRepository.findByTraineeIdOrderByLastModified(TRAINEE_ID)).thenReturn(
        List.of(entity1, entity2));

    List<CctCalculationSummaryDto> result = service.getCalculations();

    assertThat("Unexpected calculation summary count.", result.size(), is(2));

    CctCalculationSummaryDto dto1 = result.get(0);
    assertThat("Unexpected calculation ID.", dto1.id(), is(calculationId1));
    assertThat("Unexpected calculation name.", dto1.name(), is("Test Calculation 1"));
    assertThat("Unexpected PM ID.", dto1.programmeMembershipId(), is(pmId1));
    assertThat("Unexpected created timestamp.", dto1.created(), is(created1));
    assertThat("Unexpected last modified timestamp.", dto1.lastModified(), is(lastModified1));

    CctCalculationSummaryDto dto2 = result.get(1);
    assertThat("Unexpected calculation ID.", dto2.id(), is(calculationId2));
    assertThat("Unexpected calculation name.", dto2.name(), is("Test Calculation 2"));
    assertThat("Unexpected PM ID.", dto2.programmeMembershipId(), is(pmId2));
    assertThat("Unexpected created timestamp.", dto2.created(), is(created2));
    assertThat("Unexpected last modified timestamp.", dto2.lastModified(), is(lastModified2));
  }

  @Test
  void shouldReturnEmptyGettingCalculationWhenNotFound() {
    ObjectId id = ObjectId.get();

    when(calculationRepository.findById(id)).thenReturn(Optional.empty());

    Optional<CctCalculationDetailDto> result = service.getCalculation(id);

    assertThat("Unexpected calculation presence.", result.isPresent(), is(false));
  }

  @Test
  void shouldThrowExceptionGettingCalculationWhenNotBelongsToUser() {
    ObjectId id = ObjectId.get();
    CctCalculation entity = CctCalculation.builder()
        .id(id)
        .traineeId(UUID.randomUUID().toString())
        .build();

    when(calculationRepository.findById(id)).thenReturn(Optional.of(entity));

    assertThrows(NotRecordOwnerException.class, () -> service.getCalculation(id));
  }

  @Test
  void shouldGetCalculationWhenBelongsToUser() {
    ObjectId calculationId = ObjectId.get();
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
            .build())
        .changes(List.of(
            CctChange.builder().type(LTFT).startDate(LocalDate.MIN).wte(0.5).build(),
            CctChange.builder().type(LTFT).startDate(LocalDate.MAX).wte(0.75).build()
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
    assertThat("Unexpected CCT date.", dto.cctDate(), is(PLACEHOLDER_CCT_DATE));
    assertThat("Unexpected created timestamp.", dto.created(), is(created));
    assertThat("Unexpected last modified timestamp.", dto.lastModified(), is(lastModified));

    CctProgrammeMembershipDto pm = dto.programmeMembership();
    assertThat("Unexpected PM ID.", pm.id(), is(pmId));
    assertThat("Unexpected PM name.", pm.name(), is("Test Programme"));
    assertThat("Unexpected PM start date.", pm.startDate(), is(LocalDate.EPOCH));
    assertThat("Unexpected PM end date.", pm.endDate(), is(LocalDate.EPOCH.plusYears(1)));
    assertThat("Unexpected PM WTE.", pm.wte(), is(1.0));

    List<CctChangeDto> changes = dto.changes();
    assertThat("Unexpected change count.", changes.size(), is(2));

    CctChangeDto change1 = changes.get(0);
    assertThat("Unexpected change type.", change1.type(), is(LTFT));
    assertThat("Unexpected change type.", change1.startDate(), is(LocalDate.MIN));
    assertThat("Unexpected change type.", change1.wte(), is(0.5));

    CctChangeDto change2 = changes.get(1);
    assertThat("Unexpected change type.", change2.type(), is(LTFT));
    assertThat("Unexpected change type.", change2.startDate(), is(LocalDate.MAX));
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
            .build())
        .changes(List.of(
            CctChangeDto.builder().type(LTFT).startDate(LocalDate.MIN).wte(0.5).build(),
            CctChangeDto.builder().type(LTFT).startDate(LocalDate.MAX).wte(0.75).build()
        ))
        .build();

    ObjectId calculationId = ObjectId.get();
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
    assertThat("Unexpected CCT date.", savedDto.cctDate(), is(PLACEHOLDER_CCT_DATE));

    CctProgrammeMembershipDto pm = savedDto.programmeMembership();
    assertThat("Unexpected PM ID.", pm.id(), is(pmId));
    assertThat("Unexpected PM name.", pm.name(), is("Test Programme"));
    assertThat("Unexpected PM start date.", pm.startDate(), is(LocalDate.EPOCH));
    assertThat("Unexpected PM end date.", pm.endDate(), is(LocalDate.EPOCH.plusYears(1)));
    assertThat("Unexpected PM WTE.", pm.wte(), is(1.0));

    List<CctChangeDto> changes = savedDto.changes();
    assertThat("Unexpected change count.", changes.size(), is(2));

    CctChangeDto change1 = changes.get(0);
    assertThat("Unexpected change type.", change1.type(), is(LTFT));
    assertThat("Unexpected change type.", change1.startDate(), is(LocalDate.MIN));
    assertThat("Unexpected change type.", change1.wte(), is(0.5));

    CctChangeDto change2 = changes.get(1);
    assertThat("Unexpected change type.", change2.type(), is(LTFT));
    assertThat("Unexpected change type.", change2.startDate(), is(LocalDate.MAX));
    assertThat("Unexpected change type.", change2.wte(), is(0.75));
  }

  @Test
  void shouldCalculateCctDateAndIncludeOtherDetailsInReturnedDto() {
    UUID pmId = UUID.randomUUID();

    ObjectId calculationId = ObjectId.get();
    CctCalculationDetailDto dto = CctCalculationDetailDto.builder()
        .id(calculationId)
        .name("Test Calculation")
        .programmeMembership(CctProgrammeMembershipDto.builder()
            .id(pmId)
            .name("Test Programme")
            .startDate(LocalDate.EPOCH)
            .endDate(LocalDate.EPOCH.plusYears(1))
            .wte(1.0)
            .build())
        .changes(List.of(
            CctChangeDto.builder().type(LTFT).startDate(LocalDate.MIN).wte(0.5).build(),
            CctChangeDto.builder().type(LTFT).startDate(LocalDate.MAX).wte(0.75).build()
        ))
        .build();

    Optional<CctCalculationDetailDto> calculatedDtoOptional = service.calculateCctDate(dto);

    assertThat("Unexpected CCT calculation.", calculatedDtoOptional.isPresent(), is(true));
    CctCalculationDetailDto calculatedDto = calculatedDtoOptional.get();

    assertThat("Unexpected calculation ID.", calculatedDto.id(), is(calculationId));
    assertThat("Unexpected calculation name.", calculatedDto.name(), is("Test Calculation"));
    assertThat("Unexpected calculated CCT date.", calculatedDto.cctDate(),
        is(PLACEHOLDER_CCT_DATE));

    CctProgrammeMembershipDto pm = calculatedDto.programmeMembership();
    assertThat("Unexpected PM ID.", pm.id(), is(pmId));
    assertThat("Unexpected PM name.", pm.name(), is("Test Programme"));
    assertThat("Unexpected PM start date.", pm.startDate(), is(LocalDate.EPOCH));
    assertThat("Unexpected PM end date.", pm.endDate(), is(LocalDate.EPOCH.plusYears(1)));
    assertThat("Unexpected PM WTE.", pm.wte(), is(1.0));

    List<CctChangeDto> changes = calculatedDto.changes();
    assertThat("Unexpected change count.", changes.size(), is(2));

    CctChangeDto change1 = changes.get(0);
    assertThat("Unexpected change type.", change1.type(), is(LTFT));
    assertThat("Unexpected change type.", change1.startDate(), is(LocalDate.MIN));
    assertThat("Unexpected change type.", change1.wte(), is(0.5));

    CctChangeDto change2 = changes.get(1);
    assertThat("Unexpected change type.", change2.type(), is(LTFT));
    assertThat("Unexpected change type.", change2.startDate(), is(LocalDate.MAX));
    assertThat("Unexpected change type.", change2.wte(), is(0.75));
  }

  @Test
  void shouldUpdateExistingCalculationIfExistsAndOwnedByUser() {
    UUID pmId = UUID.randomUUID();
    ObjectId id = ObjectId.get();
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
            .build())
        .changes(List.of(
            CctChangeDto.builder().type(LTFT).startDate(LocalDate.MIN).wte(0.5).build(),
            CctChangeDto.builder().type(LTFT).startDate(LocalDate.MAX).wte(0.75).build()
        ))
        .build();

    CctCalculation existingCalc = CctCalculation.builder()
        .id(id)
        .traineeId(TRAINEE_ID)
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

    Optional<CctCalculationDetailDto> updatedDtoOptional = service.updateCalculation(id, dto);
    assertThat("Unexpected saved calculation.", updatedDtoOptional.isPresent(), is(true));

    CctCalculationDetailDto updatedDto = updatedDtoOptional.get();
    assertThat("Unexpected calculation ID.", updatedDto.id(), is(id));
    assertThat("Unexpected calculation created.", updatedDto.created(), is(created));
    assertThat("Unexpected calculation last modified.", updatedDto.lastModified(),
        is(modified));
    assertThat("Unexpected calculation name.", updatedDto.name(), is("Test Calculation"));
    assertThat("Unexpected CCT date.", updatedDto.cctDate(), is(PLACEHOLDER_CCT_DATE));

    CctProgrammeMembershipDto pm = updatedDto.programmeMembership();
    assertThat("Unexpected PM ID.", pm.id(), is(pmId));
    assertThat("Unexpected PM name.", pm.name(), is("Test Programme"));
    assertThat("Unexpected PM start date.", pm.startDate(), is(LocalDate.EPOCH));
    assertThat("Unexpected PM end date.", pm.endDate(), is(LocalDate.EPOCH.plusYears(1)));
    assertThat("Unexpected PM WTE.", pm.wte(), is(1.0));

    List<CctChangeDto> changes = updatedDto.changes();
    assertThat("Unexpected change count.", changes.size(), is(2));

    CctChangeDto change1 = changes.get(0);
    assertThat("Unexpected change type.", change1.type(), is(LTFT));
    assertThat("Unexpected change type.", change1.startDate(), is(LocalDate.MIN));
    assertThat("Unexpected change type.", change1.wte(), is(0.5));

    CctChangeDto change2 = changes.get(1);
    assertThat("Unexpected change type.", change2.type(), is(LTFT));
    assertThat("Unexpected change type.", change2.startDate(), is(LocalDate.MAX));
    assertThat("Unexpected change type.", change2.wte(), is(0.75));
  }

  @Test
  void shouldNotUpdateExistingCalculationIfNotExists() {
    UUID pmId = UUID.randomUUID();
    ObjectId id = ObjectId.get();
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
            .build())
        .changes(List.of(
            CctChangeDto.builder().type(LTFT).startDate(LocalDate.MIN).wte(0.5).build(),
            CctChangeDto.builder().type(LTFT).startDate(LocalDate.MAX).wte(0.75).build()
        ))
        .build();

    when(calculationRepository.findById(any())).thenReturn(Optional.empty());

    Optional<CctCalculationDetailDto> updatedDtoOptional = service.updateCalculation(id, dto);

    assertThat("Unexpected saved calculation.", updatedDtoOptional.isPresent(), is(false));
    verify(calculationRepository).findById(id);
    verifyNoMoreInteractions(calculationRepository);
  }
}
