package uk.nhs.hee.trainee.details.dto;

import lombok.Data;

@Data
public class HeeUserDto {
  private String emailAddress;
  private String firstName;
  private String lastName;
  private String gmcId;
  private String phoneNumber;
}
