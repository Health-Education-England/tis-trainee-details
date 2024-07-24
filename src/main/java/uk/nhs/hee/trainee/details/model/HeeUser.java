package uk.nhs.hee.trainee.details.model;

import lombok.Data;

/**
 * A class for HEE user details.
 */
@Data
public class HeeUser {
  private String emailAddress;
  private String firstName;
  private String lastName;
  private String gmcId;
  private String phoneNumber;
}
