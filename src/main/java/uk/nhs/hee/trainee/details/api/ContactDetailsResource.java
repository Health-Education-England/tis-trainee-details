package uk.nhs.hee.trainee.details.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.trainee.details.dto.ContactDetailsDto;
import uk.nhs.hee.trainee.details.mapper.ContactDetailsMapper;
import uk.nhs.hee.trainee.details.model.ContactDetails;
import uk.nhs.hee.trainee.details.service.ContactDetailsService;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api")
public class ContactDetailsResource {

  private static final Logger log = LoggerFactory.getLogger(ContactDetailsResource.class);
  @Autowired
  private ContactDetailsService contactDetailsService;
  @Autowired
  private ContactDetailsMapper contactDetailsMapper;

  /**
   * Get a contact details record with a given ID.
   *
   * @param contactDetailsId The ID of the contact details to get.
   * @return The {@link ContactDetailsDto} representing the contact details.
   */
  @GetMapping("/contactdetails/{id}")
  public ContactDetailsDto getContactDetailsById(
      @PathVariable(name = "id") String contactDetailsId) {
    log.trace("Contact Details of a trainee by contactDetailsId {}", contactDetailsId);
    ContactDetails contactDetails = contactDetailsService.getContactDetails(contactDetailsId);
    return contactDetailsMapper.toDto(contactDetails);
  }

  /**
   * Get a trainee's contact details based on the trainee's ID.
   *
   * @param traineeId The trainee's ID.
   * @return The {@link ContactDetailsDto} representing the trainee's contact details.
   */
  @GetMapping("/contactdetails/trainee/{traineeId}")
  public ContactDetailsDto getContactDetailsByTraineeId(
      @PathVariable(name = "traineeId") String traineeId) {
    log.trace("Contact Details of a trainee by traineeId {}", traineeId);
    ContactDetails contactDetails = contactDetailsService
        .getContactDetailsByTraineeTisId(traineeId);
    return contactDetailsMapper.toDto(contactDetails);
  }

  @PostMapping("/contactdetails/")
  public ContactDetails addContactDetails(@RequestBody ContactDetails contactDetails) {

    return null;
  }
}
