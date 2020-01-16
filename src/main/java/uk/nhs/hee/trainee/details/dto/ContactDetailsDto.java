package uk.nhs.hee.trainee.details.dto;

import lombok.Data;

import java.time.LocalDate;

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
}
