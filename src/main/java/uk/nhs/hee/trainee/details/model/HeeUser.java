package uk.nhs.hee.trainee.details.model;

import lombok.Data;

@Data
public class HeeUser {
  private String emailAddress;
  private String firstName;
  private String lastName;
  private String gmcId;
  private String phoneNumber;
}
