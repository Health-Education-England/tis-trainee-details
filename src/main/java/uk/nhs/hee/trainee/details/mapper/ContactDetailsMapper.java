package uk.nhs.hee.trainee.details.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import uk.nhs.hee.trainee.details.dto.ContactDetailsDTO;
import uk.nhs.hee.trainee.details.model.ContactDetails;

@Mapper(componentModel = "spring")
public interface ContactDetailsMapper {
    ContactDetailsDTO contactDetailsToContactDetailsDTO(ContactDetails contactDetails);
    ContactDetails contactDetailsDTOToContactDetails(ContactDetailsDTO contactDetailsDTO);
}
