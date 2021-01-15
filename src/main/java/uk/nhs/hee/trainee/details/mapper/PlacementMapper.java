package uk.nhs.hee.trainee.details.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import uk.nhs.hee.trainee.details.dto.PlacementDto;
import uk.nhs.hee.trainee.details.model.Placement;

@Mapper(componentModel = "spring")
public interface PlacementMapper {

  PlacementDto toDto(Placement entity);

  Placement toEntity(PlacementDto dto);

  void updatePlacement(@MappingTarget Placement target, Placement source);
}
