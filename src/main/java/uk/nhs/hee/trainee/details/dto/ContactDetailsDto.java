package uk.nhs.hee.trainee.details.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

/**
 * A DTO for ContactDetails entity
 * Holds the fields for all the information of the trainee
 */
@Data
public class ContactDetailsDto {

  private String id;
  private String traineeTisId;
  private String surname;
  private String forenames;
  private String knownAs;
  private String maidenName;
  private String title;
  private String telephoneNumber;
  private String mobileNumber;
  private String email;
  private String address1;
  private String address2;
  private String address3;
  private String address4;
  private String postCode;
  private String gmcNumber;
  private String gmcStatus;
  private String gdcNumber;
  private String gdcStatus;
  private String publicHealthNumber;
  private String eeaResident;
  private String permitToWork;
  private String settled;
  private LocalDate visaIssued;
  private String detailsNumber;
  private List<ProgrammeMembershipDto> programmeMemberships;
  private List<PlacementDto> placements;
}
