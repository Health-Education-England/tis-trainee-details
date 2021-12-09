/*
 * The MIT License (MIT)
 *
 * Copyright 2020 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.trainee.details.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDate;
import lombok.Data;
import uk.nhs.hee.trainee.details.dto.deserializer.LowerCaseDeserializer;

/**
 * A DTO for PersonalDetails entity, holds the fields for personal information of the trainee.
 */
@Data
public class PersonalDetailsDto {

  private String surname;
  private String forenames;
  private String knownAs;
  private String maidenName;
  private String title;
  private String personOwner;
  private LocalDate dateOfBirth;
  private String gender;
  private String qualification;
  private LocalDate dateAttained;
  private String medicalSchool;
  private String telephoneNumber;
  private String mobileNumber;
  @JsonDeserialize(using = LowerCaseDeserializer.class)
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
  private String prevRevalBody;
  private LocalDate currRevalDate;
  private LocalDate prevRevalDate;
}
