package uk.nhs.hee.trainee.details.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.Objects;
/*import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor*/
@Document(collection = "ContactDetails")
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

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTraineeTISId() {
    return traineeTISId;
  }

  public void setTraineeTISId(String traineeTISId) {
    this.traineeTISId = traineeTISId;
  }

  public String getSurname() {
    return surname;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

  public String getForenames() {
    return forenames;
  }

  public void setForenames(String forenames) {
    this.forenames = forenames;
  }

  public String getKnownAs() {
    return knownAs;
  }

  public void setKnownAs(String knownAs) {
    this.knownAs = knownAs;
  }

  public String getMaidenName() {
    return maidenName;
  }

  public void setMaidenName(String maidenName) {
    this.maidenName = maidenName;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTelephoneNumber() {
    return telephoneNumber;
  }

  public void setTelephoneNumber(String telephoneNumber) {
    this.telephoneNumber = telephoneNumber;
  }

  public String getMobileNumber() {
    return mobileNumber;
  }

  public void setMobileNumber(String mobileNumber) {
    this.mobileNumber = mobileNumber;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getAddress1() {
    return address1;
  }

  public void setAddress1(String address1) {
    this.address1 = address1;
  }

  public String getAddress2() {
    return address2;
  }

  public void setAddress2(String address2) {
    this.address2 = address2;
  }

  public String getAddress3() {
    return address3;
  }

  public void setAddress3(String address3) {
    this.address3 = address3;
  }

  public String getAddress4() {
    return address4;
  }

  public void setAddress4(String address4) {
    this.address4 = address4;
  }

  public String getPostCode() {
    return postCode;
  }

  public void setPostCode(String postCode) {
    this.postCode = postCode;
  }

  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof ContactDetails)) {
      return false;
    }
    if (!super.equals(object)) {
      return false;
    }
    ContactDetails that = (ContactDetails) object;
    return java.util.Objects.equals(getId(), that.getId()) &&
        java.util.Objects.equals(getTraineeTISId(), that.getTraineeTISId()) &&
        java.util.Objects.equals(getSurname(), that.getSurname()) &&
        java.util.Objects.equals(getForenames(), that.getForenames()) &&
        java.util.Objects.equals(getKnownAs(), that.getKnownAs()) &&
        java.util.Objects.equals(getMaidenName(), that.getMaidenName()) &&
        java.util.Objects.equals(getTitle(), that.getTitle()) &&
        java.util.Objects.equals(getTelephoneNumber(), that.getTelephoneNumber()) &&
        java.util.Objects.equals(getMobileNumber(), that.getMobileNumber()) &&
        java.util.Objects.equals(getEmail(), that.getEmail()) &&
        java.util.Objects.equals(getAddress1(), that.getAddress1()) &&
        java.util.Objects.equals(getAddress2(), that.getAddress2()) &&
        java.util.Objects.equals(getAddress3(), that.getAddress3()) &&
        java.util.Objects.equals(getAddress4(), that.getAddress4()) &&
        java.util.Objects.equals(getPostCode(), that.getPostCode());
  }

  public int hashCode() {

    return Objects.hash(super.hashCode(), getId(), getTraineeTISId(), getSurname(), getForenames(),
        getKnownAs(), getMaidenName(), getTitle(), getTelephoneNumber(), getMobileNumber(),
        getEmail(), getAddress1(), getAddress2(), getAddress3(), getAddress4(), getPostCode());
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "ContactDetails{" +
        "id='" + id + '\'' +
        ", traineeTISId='" + traineeTISId + '\'' +
        ", surname='" + surname + '\'' +
        ", forenames='" + forenames + '\'' +
        ", knownAs='" + knownAs + '\'' +
        ", maidenName='" + maidenName + '\'' +
        ", title='" + title + '\'' +
        ", telephoneNumber='" + telephoneNumber + '\'' +
        ", mobileNumber='" + mobileNumber + '\'' +
        ", email='" + email + '\'' +
        ", address1='" + address1 + '\'' +
        ", address2='" + address2 + '\'' +
        ", address3='" + address3 + '\'' +
        ", address4='" + address4 + '\'' +
        ", postCode='" + postCode + '\'' +
        '}';
  }
}