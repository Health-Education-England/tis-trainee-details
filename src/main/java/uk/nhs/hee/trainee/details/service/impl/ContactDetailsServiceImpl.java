package  uk.nhs.hee.trainee.details.service.impl;

import uk.nhs.hee.trainee.details.service.ContactDetailsService;
import uk.nhs.hee.trainee.details.model.ContactDetails;
import uk.nhs.hee.trainee.details.repository.ContactDetailsRepository;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ContactDetailsServiceImpl implements ContactDetailsService{

  @Autowired
  ContactDetailsRepository contactDetailsRepository;

  public ContactDetails getContactDetails(String id){
    return contactDetailsRepository.findById(id).orElse(null);
  }

  public ContactDetails getContactDetailsByTraineeTisId(String traineeTisId){
    return contactDetailsRepository.findByTraineeTisId(traineeTisId);
  }

}