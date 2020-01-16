package uk.nhs.hee.trainee.details.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.nhs.hee.trainee.details.model.ContactDetails;

@Repository
public interface ContactDetailsRepository extends MongoRepository<ContactDetails, String> {

  ContactDetails findByTraineeTisId(String traineeTisId);
}
