package uk.nhs.hee.trainee.details.service;

import java.util.Optional;
import uk.nhs.hee.trainee.details.model.PersonalDetails;

public interface PersonalDetailsService {

  /**
   * Update the personal details for the trainee with the given TIS ID.
   *
   * @param tisId           The TIS id of the trainee.
   * @param personalDetails The details to add to the trainee.
   * @return The updated personal details or empty if a trainee with the ID was not found.
   */
  Optional<PersonalDetails> updateByTisId(String tisId, PersonalDetails personalDetails);
}
