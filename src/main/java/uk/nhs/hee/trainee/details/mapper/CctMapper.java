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

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.nhs.hee.trainee.details.dto.CctCalculateResponseDto;
import uk.nhs.hee.trainee.details.dto.CctCalculatedChangeDto;
import uk.nhs.hee.trainee.details.dto.CctCalculationDetailDto;
import uk.nhs.hee.trainee.details.dto.CctCalculationSaveRequestDto;
import uk.nhs.hee.trainee.details.model.CctCalculation;
import uk.nhs.hee.trainee.details.model.CctCalculation.CctChange;

/**
 * A mapper to convert between CCT related entities and DTOs.
 */
@Mapper(componentModel = SPRING)
public interface CctMapper {

  /**
   * Convert a {@link CctCalculation} entity to a {@link CctCalculationDetailDto} DTO.
   *
   * @param entity The entity to convert to a DTO.
   * @return The equivalent detail DTO.
   */
  CctCalculationDetailDto toDetailDto(CctCalculation entity);

  /**
   * Convert a list of {@link CctCalculation} to a list of {@link CctCalculationDetailDto}.
   *
   * @param entities The entities to convert to DTOs.
   * @return The equivalent detail DTOs.
   */
  List<CctCalculationDetailDto> toDetailDtos(List<CctCalculation> entities);

  /**
   * Convert a {@link CctCalculationSaveRequestDto} DTO to a {@link CctCalculation} entity.
   *
   * @param dto         The DTO to convert to an entity.
   * @param calculation The calculation made from the DTO.
   * @param traineeId   The ID of the trainee the calculation is for.
   * @return The equivalent entity with the calculation and trainee ID injected.
   */
  @Mapping(target = "changes", source = "calculation.changes")
  @Mapping(target = "result", source = "calculation.result")
  @Mapping(target = "traineeId", source = "traineeId")
  @Mapping(target = "created", ignore = true)
  @Mapping(target = "lastModified", ignore = true)
  CctCalculation toEntity(CctCalculationSaveRequestDto dto, CctCalculateResponseDto calculation,
      String traineeId);

  /**
   * Convert a {@link CctCalculatedChangeDto} DTO to a {@link CctChange} entity.
   *
   * @param dto The DTO to convert to an entity.
   * @return The equivalent entity.
   */
  @Mapping(target = ".", source = "change")
  CctChange toEntity(CctCalculatedChangeDto dto);

  /**
   * Convert a {@link CctChange} entity to a {@link CctCalculatedChangeDto} DTO.
   *
   * @param entity The entity to convert to a DTO.
   * @return The equivalent DTO.
   */
  @Mapping(target = "change", source = ".")
  CctCalculatedChangeDto toDto(CctChange entity);
}
