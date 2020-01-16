package uk.nhs.hee.trainee.details.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import uk.nhs.hee.trainee.details.dto.ContactDetailsDto;
import uk.nhs.hee.trainee.details.model.ContactDetails;

@Mapper(componentModel = "spring")
public interface ContactDetailsMapper {
    ContactDetailsDto contactDetailsToContactDetailsDto(ContactDetails contactDetails);
    ContactDetails contactDetailsDtoToContactDetails(ContactDetailsDto contactDetailsDto);
}
