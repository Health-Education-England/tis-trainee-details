package uk.nhs.hee.trainee.details.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.CrossOrigin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.nhs.hee.trainee.details.dto.ContactDetailsDTO;
import uk.nhs.hee.trainee.details.mapper.ContactDetailsMapper;
import uk.nhs.hee.trainee.details.service.ContactDetailsService;
import uk.nhs.hee.trainee.details.model.ContactDetails;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api")
public class ContactDetailsResource {

  @Autowired
  private ContactDetailsService contactDetailsService;

  @Autowired
  private ContactDetailsMapper contactDetailsMapper;

  private static final Logger log = LoggerFactory.getLogger(ContactDetailsResource.class);

  @GetMapping("/contactdetails/{id}")
  public ContactDetailsDTO getContactDetailsById(@PathVariable(name = "id") String contactDetailsId){
    log.trace("Contact Details of a trainee by contactDetailsId {}", contactDetailsId);
    ContactDetails contactDetails = contactDetailsService.getContactDetails(contactDetailsId);
    return contactDetailsMapper.contactDetailsToContactDetailsDTO(contactDetails);
  }

  @GetMapping("/contactdetails/trainee/{traineeId}")
  public ContactDetailsDTO getContactDetailsByTraineeId(@PathVariable(name = "traineeId") String traineeId){
    log.trace("Contact Details of a trainee by traineeId {}", traineeId);
    ContactDetails contactDetails = contactDetailsService.getContactDetailsByTraineeTisId(traineeId);
      return contactDetailsMapper.contactDetailsToContactDetailsDTO(contactDetails);
  }

  @PostMapping("/contactdetails/")
  public ContactDetails addContactDetails(@RequestBody ContactDetails contactDetails){

    return null;
  }
}