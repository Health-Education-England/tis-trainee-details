package uk.nhs.hee.trainee.details.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsDto;
import uk.nhs.hee.trainee.details.model.PersonalDetails;

@Mapper(componentModel = "spring")
public interface PersonalDetailsMapper {

  PersonalDetailsDto toDto(PersonalDetails entity);

  PersonalDetails toEntity(PersonalDetailsDto dto);

  void update(@MappingTarget PersonalDetails target, PersonalDetails source);
}
