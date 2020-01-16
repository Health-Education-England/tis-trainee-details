package uk.nhs.hee.trainee.details.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.Data;
import java.time.LocalDate;
import lombok.NoArgsConstructor;

@Document(collection = "ContactDetails")
@Data
@NoArgsConstructor
public class ContactDetails {

  @Id
  private String id;

  @Indexed(unique = true)
  @Field(value = "traineeTisId")
  private String traineeTisId;
  @Field(value = "surname")
  private String surname;
  @Field(value = "forenames")
  private String forenames;
  @Field(value = "knownAs")
  private String knownAs;
  @Field(value = "maidenName")
  private String maidenName;
  @Field(value = "title")
  private String title;
  @Field(value = "telephoneNumber")
  private String telephoneNumber;
  @Field(value = "mobileNumber")
  private String mobileNumber;
  @Field(value = "email")
  private String email;
  @Field(value = "address1")
  private String address1;
  @Field(value = "address2")
  private String address2;
  @Field(value = "address3")
  private String address3;
  @Field(value = "address4")
  private String address4;
  @Field(value = "postCode")
  private String postCode;
  @Field(value = "gmcNumber")
  private String gmcNumber;
  @Field(value = "gmcStatus")
  private String gmcStatus;
  @Field(value = "gdcNumber")
  private String gdcNumber;
  @Field(value = "gdcStatus")
  private String gdcStatus;
  @Field(value = "publicHealthNumber")
  private String publicHealthNumber;
  @Field(value = "eeaResident")
  private String eeaResident;
  @Field(value = "permitToWork")
  private String permitToWork;
  @Field(value = "settled")
  private String settled;
  @Field(value = "visaIssued")
  private LocalDate visaIssued;
  @Field(value = "detailsNumber")
  private String detailsNumber;

  public ContactDetails id(String id){
    this.id = id;
    return this;
  }

  public ContactDetails traineeTisId(String traineeTisId){
    this.traineeTisId = traineeTisId;
    return this;
  }

  public ContactDetails surname(String surname){
    this.surname = surname;
    return this;
  }

  public ContactDetails forenames(String forenames){
    this.forenames = forenames;
    return this;
  }

  public ContactDetails knownAs(String knownAs){
    this.knownAs = knownAs;
    return this;
  }

  public ContactDetails maidenName(String maidenName){
    this.maidenName = maidenName;
    return this;
  }

  public ContactDetails title(String title){
    this.title = title;
    return this;
  }

  public ContactDetails telephoneNumber(String telephoneNumber){
    this.telephoneNumber = telephoneNumber;
    return this;
  }

  public ContactDetails mobileNumber(String mobileNumber){
    this.mobileNumber = mobileNumber;
    return this;
  }

  public ContactDetails email(String email){
    this.email = email;
    return this;
  }

  public ContactDetails address1(String address1){
    this.address1 = address1;
    return this;
  }

  public ContactDetails address2(String address2){
    this.address2 = address2;
    return this;
  }

  public ContactDetails address3(String address3){
    this.address3 = address3;
    return this;
  }

  public ContactDetails address4(String address4){
    this.address4 = address4;
    return this;
  }

  public ContactDetails postCode(String postCode){
    this.postCode = postCode;
    return this;
  }

  public ContactDetails gmcNumber(String gmcNumber){
    this.gmcNumber = gmcNumber;
    return this;
  }

  public ContactDetails gmcStatus(String gmcStatus){
    this.gmcStatus = gmcStatus;
    return this;
  }

  public ContactDetails gdcNumber(String gdcNumber){
    this.gdcNumber = gdcNumber;
    return this;
  }

  public ContactDetails gdcStatus(String gdcStatus){
    this.gdcStatus = gdcStatus;
    return this;
  }

  public ContactDetails publicHealthNumber(String publicHealthNumber){
    this.publicHealthNumber = publicHealthNumber;
    return this;
  }

  public ContactDetails eeaResident(String eeaResident){
    this.eeaResident = eeaResident;
    return this;
  }

  public ContactDetails permitToWork(String permitToWork){
    this.permitToWork = permitToWork;
    return this;
  }

  public ContactDetails settled(String settled){
    this.settled = settled;
    return this;
  }

  public ContactDetails visaIssued(LocalDate visaIssued){
    this.visaIssued = visaIssued;
    return this;
  }

  public ContactDetails detailsNumber(String detailsNumber){
    this.detailsNumber = detailsNumber;
    return this;
  }
}