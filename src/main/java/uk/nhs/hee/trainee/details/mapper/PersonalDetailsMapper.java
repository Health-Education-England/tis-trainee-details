/*
 * The MIT License (MIT)
 *
 * Copyright 2020 Crown Copyright (Health Education England)
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

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "dateOfBirth", source = "dateOfBirth")
  @Mapping(target = "gender", source = "gender")
  void updatePersonalInfo(@MappingTarget PersonalDetails target, PersonalDetails source);
}