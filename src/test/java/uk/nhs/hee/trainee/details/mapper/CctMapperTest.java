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

package uk.nhs.hee.trainee.details.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.LTFT;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST1;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.trainee.details.dto.CctCalculateResponseDto;
import uk.nhs.hee.trainee.details.dto.CctCalculatedChangeDto;
import uk.nhs.hee.trainee.details.dto.CctCalculationDetailDto;
import uk.nhs.hee.trainee.details.dto.CctCalculationSaveRequestDto;
import uk.nhs.hee.trainee.details.dto.CctChangeDto;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto;
import uk.nhs.hee.trainee.details.model.CctCalculation;
import uk.nhs.hee.trainee.details.model.CctCalculation.CctChange;
import uk.nhs.hee.trainee.details.service.CctEngine;

@ExtendWith(MockitoExtension.class)
class CctMapperTest {

  private static final String TRAINEE_ID = "40";

  @InjectMocks
  private CctMapperImpl mapper;

  @Test
  void shouldFlattenCalculatedChangeWhenConvertingToEntity() {
    CctCalculatedChangeDto dto = CctCalculatedChangeDto.builder()
        .change(datedLtft())
        .calendarMonths(6.016438356164383)
        .wteMonths(3.6098630136986296)
        .build();

    CctChange entity = mapper.toEntity(dto);

    assertThat("Unexpected type.", entity.type(), is(LTFT));
    assertThat("Unexpected start date.", entity.startDate(), is(LocalDate.of(2024, 2, 1)));
    assertThat("Unexpected end date.", entity.endDate(), is(LocalDate.of(2024, 8, 1)));
    assertThat("Unexpected WTE.", entity.wte(), is(0.6));
    assertThat("Unexpected counts towards completion.", entity.countsTowardsCompletion(),
        is(true));
    assertThat("Unexpected notes.", entity.notes(), is("Cardiology rotation"));
    assertThat("Unexpected calendar months.", entity.calendarMonths(), is(6.016438356164383));
    assertThat("Unexpected WTE months.", entity.wteMonths(), is(3.6098630136986296));
  }

  @Test
  void shouldWrapChangeWhenConvertingToCalculatedChange() {
    CctChange entity = CctChange.builder()
        .type(LTFT)
        .startDate(LocalDate.of(2025, 2, 1))
        .wte(0.6)
        .countsTowardsCompletion(true)
        .notes("")
        .build();

    CctCalculatedChangeDto dto = mapper.toDto(entity);

    assertThat("Unexpected change.", dto.change(), is(CctChangeDto.builder()
        .type(LTFT)
        .startDate(LocalDate.of(2025, 2, 1))
        .wte(0.6)
        .countsTowardsCompletion(true)
        .notes("")
        .build()));
    assertThat("Unexpected calendar months.", dto.calendarMonths(), nullValue());
    assertThat("Unexpected WTE months.", dto.wteMonths(), nullValue());
  }

  @Test
  void shouldInjectCalculationAndTraineeIdWhenConvertingToEntity() {
    UUID id = UUID.randomUUID();
    CctCalculationSaveRequestDto dto = request(id);
    CctCalculateResponseDto calculation = calculate(dto);

    CctCalculation entity = mapper.toEntity(dto, calculation, TRAINEE_ID);

    assertThat("Unexpected ID.", entity.id(), is(id));
    assertThat("Unexpected trainee ID.", entity.traineeId(), is(TRAINEE_ID));
    assertThat("Unexpected name.", entity.name(), is("Test Calculation"));
    assertThat("Unexpected specialty.", entity.programmeDetails().specialty(),
        is("Paediatrics"));
    assertThat("Unexpected WTE months.", entity.changes().get(0).wteMonths(),
        is(calculation.changes().get(0).wteMonths()));
    assertThat("Unexpected CCT date.", entity.result().cctDate(),
        is(calculation.result().cctDate()));
    assertThat("Unexpected created timestamp.", entity.created(), nullValue());
    assertThat("Unexpected last modified timestamp.", entity.lastModified(), nullValue());
  }

  @Test
  void shouldReturnSavedDetailsUnchangedWhenConvertingEntityToDetailDto() {
    CctCalculationSaveRequestDto dto = request(UUID.randomUUID());
    CctCalculateResponseDto calculation = calculate(dto);

    CctCalculationDetailDto detail = mapper.toDetailDto(
        mapper.toEntity(dto, calculation, TRAINEE_ID));

    assertThat("Unexpected ID.", detail.id(), is(dto.id()));
    assertThat("Unexpected name.", detail.name(), is(dto.name()));
    assertThat("Unexpected programme details.", detail.programmeDetails(),
        is(dto.programmeDetails()));
    assertThat("Unexpected changes.", detail.changes(), is(calculation.changes()));
    assertThat("Unexpected result.", detail.result(), is(calculation.result()));
  }

  private static CctCalculationSaveRequestDto request(UUID id) {
    return CctCalculationSaveRequestDto.builder()
        .id(id)
        .name("Test Calculation")
        .programmeDetails(CctProgrammeDetailsDto.builder()
            .specialty("Paediatrics")
            .startDate(LocalDate.of(2023, 8, 2))
            .lengthMonths(84)
            .startGrade(ST1)
            .additionalMonths(0)
            .acceleratedMonths(0)
            .build())
        .changes(List.of(
            datedLtft(),
            CctChangeDto.builder()
                .type(LTFT)
                .startDate(LocalDate.of(2025, 2, 1))
                .wte(0.6)
                .countsTowardsCompletion(true)
                .notes("")
                .build()))
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

  private static CctCalculateResponseDto calculate(CctCalculationSaveRequestDto dto) {
    return new CctEngine().calculate(dto.programmeDetails(), dto.changes());
  }
}
