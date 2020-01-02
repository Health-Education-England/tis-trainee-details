package uk.nhs.hee.trainee.details.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Document(collection = "ContactDetails")
@Getter
@Setter
@NoArgsConstructor
public class ContactDetails {

  @Id
  private String id;

  @Indexed(unique = true)
  @Field(value = "traineeTISId")
  private String traineeTISId;
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

  public ContactDetails id(String id){
    this.id = id;
    return this;
  }

  public ContactDetails traineeTISId(String traineeTISId){
    this.traineeTISId = traineeTISId;
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
}