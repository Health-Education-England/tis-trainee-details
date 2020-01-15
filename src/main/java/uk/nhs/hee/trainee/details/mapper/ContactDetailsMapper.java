package uk.nhs.hee.trainee.details.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import uk.nhs.hee.trainee.details.dto.ContactDetailsDTO;
import uk.nhs.hee.trainee.details.model.ContactDetails;

@Mapper
public interface ContactDetailsMapper {
    ContactDetailsMapper MAPPER = Mappers.getMapper( ContactDetailsMapper.class );

    ContactDetailsDTO contactDetailsToContactDetailsDTO(ContactDetails contactDetails);
    ContactDetails contactDetailsDTOToContactDetails(ContactDetailsDTO contactDetailsDTO);
}
