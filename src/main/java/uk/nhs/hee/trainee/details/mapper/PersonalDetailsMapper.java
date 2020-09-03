package uk.nhs.hee.trainee.details.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsDto;
import uk.nhs.hee.trainee.details.model.PersonalDetails;

@Mapper(componentModel = "spring")
public interface PersonalDetailsMapper {

  PersonalDetailsDto toDto(PersonalDetails entity);

  PersonalDetails toEntity(PersonalDetailsDto dto);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "title", source = "title")
  @Mapping(target = "forenames", source = "forenames")
  @Mapping(target = "knownAs", source = "knownAs")
  @Mapping(target = "surname", source = "surname")
  @Mapping(target = "maidenName", source = "maidenName")
  @Mapping(target = "telephoneNumber", source = "telephoneNumber")
  @Mapping(target = "mobileNumber", source = "mobileNumber")
  @Mapping(target = "email", source = "email")
  @Mapping(target = "address1", source = "address1")
  @Mapping(target = "address2", source = "address2")
  @Mapping(target = "address3", source = "address3")
  @Mapping(target = "address4", source = "address4")
  @Mapping(target = "postCode", source = "postCode")
  void updateContactDetails(@MappingTarget PersonalDetails target, PersonalDetails source);
}
