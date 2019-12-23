package uk.nhs.hee.trainee.details.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import uk.nhs.hee.trainee.details.service.ContactDetailsService;
import uk.nhs.hee.trainee.details.model.ContactDetails;

@RestController
@RequestMapping("/trainee")
public class ContactDetailsResource {

  @Autowired
  private ContactDetailsService contactDetailsService;

 /* @RequestMapping("/")
  public String home(){
    return "Hello World!";
  }*/

  @GetMapping("/contactdetails/{traineeId}")
  public ContactDetails getContactDetails(@PathVariable(name = "traineeId") String domainId){
    return contactDetailsService.getContactDetails(domainId);
  }


}