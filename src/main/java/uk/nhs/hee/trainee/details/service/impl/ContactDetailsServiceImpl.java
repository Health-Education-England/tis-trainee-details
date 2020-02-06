package uk.nhs.hee.trainee.details.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.dto.enumeration.PlacementStatus;
import uk.nhs.hee.trainee.details.dto.enumeration.ProgrammeStatus;
import uk.nhs.hee.trainee.details.model.ContactDetails;
import uk.nhs.hee.trainee.details.repository.ContactDetailsRepository;
import uk.nhs.hee.trainee.details.service.ContactDetailsService;

@Service
public class ContactDetailsServiceImpl implements ContactDetailsService {

  @Autowired
  ContactDetailsRepository contactDetailsRepository;

  public ContactDetails getContactDetails(String id) {
    return contactDetailsRepository.findById(id).orElse(null);
  }

  public ContactDetails getContactDetailsByTraineeTisId(String traineeTisId) {
    return contactDetailsRepository.findByTraineeTisId(traineeTisId);
  }

  public ContactDetails hidePastProgrammes(ContactDetails contactDetails) {
    contactDetails.getProgrammeMemberships().removeIf(c -> c.getStatus() == ProgrammeStatus.PAST);
    return contactDetails;
  }

  public ContactDetails hidePastPlacements(ContactDetails contactDetails) {
    contactDetails.getPlacements().removeIf(c -> c.getStatus() == PlacementStatus.PAST);
    return contactDetails;
  }

}
