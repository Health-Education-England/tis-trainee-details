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

import java.time.LocalDate;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import uk.nhs.hee.trainee.details.dto.CctCalculationDetailDto;
import uk.nhs.hee.trainee.details.dto.CctCalculationSummaryDto;
import uk.nhs.hee.trainee.details.model.CctCalculation;

/**
 * A mapper to convert between CCT related entities and DTOs.
 */
@Mapper(componentModel = SPRING)
public interface CctMapper {

  public static final LocalDate PLACEHOLDER_CCT_DATE = LocalDate.of(2030, 1, 1);

  /**
   * Convert a {@link CctCalculation} entity to a {@link CctCalculationSummaryDto} DTO.
   *
   * @param entity The entity to convert to a DTO.
   * @return The equivalent summary DTO.
   */
  @Mapping(target = "programmeMembershipId", source = "programmeMembership.id")
  CctCalculationSummaryDto toSummaryDto(CctCalculation entity);

  /**
   * Convert a list of {@link CctCalculation} to a list of {@link CctCalculationDetailDto}.
   *
   * @param entities The entities to convert to DTOs.
   * @return The equivalent summary DTOs.
   */
  List<CctCalculationSummaryDto> toSummaryDtos(List<CctCalculation> entities);

  /**
   * Convert a {@link CctCalculation} entity to a {@link CctCalculationDetailDto} DTO.
   *
   * @param entity The entity to convert to a DTO.
   * @return The equivalent detail DTO.
   */
  @Mapping(target = "cctDate", source = "entity", qualifiedByName = "calculateCctDate")
  CctCalculationDetailDto toDetailDto(CctCalculation entity);

  /**
   * Convert a {@link CctCalculationDetailDto} DTO to a {@link CctCalculation} entity.
   *
   * @param dto       The DTO to convert to an entity.
   * @param traineeId The ID of the trainee the calculation is for.
   * @return The equivalent entity with trainee ID injected.
   */
  @Mapping(target = "traineeId", source = "traineeId")
  CctCalculation toEntity(CctCalculationDetailDto dto, String traineeId);

  /**
   * Calculate the CCT end date for a CCT Calculation.
   *
   * @param entity The CctCalculation to use to calculate the CCT end date.
   * @return the CCT end date, or null if CctCalculation is null.
   */
  @Named("calculateCctDate")
  static LocalDate calculateCctDate(CctCalculation entity) {
    if (entity != null) {
      return PLACEHOLDER_CCT_DATE;
    }
    return null;
  }
}
