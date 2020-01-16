package uk.nhs.hee.trainee.details.service;

import uk.nhs.hee.trainee.details.model.ContactDetails;

public interface ContactDetailsService {
  ContactDetails getContactDetails(String id);
  ContactDetails getContactDetailsByTraineeTisId(String traineeTisId);
}