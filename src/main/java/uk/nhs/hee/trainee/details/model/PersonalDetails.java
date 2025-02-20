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

package uk.nhs.hee.trainee.details.model;

import java.time.LocalDate;
import lombok.Data;

/**
 * Trainee doctor personal details.
 */
@Data
public class PersonalDetails {

  private String surname; // ContactDetails
  private String forenames; // ContactDetails
  private String knownAs; // ContactDetails
  private String maidenName; // ContactDetails
  private String title; // ContactDetails
  private String personOwner; // PersonOwner
  private String role; // PersonalDetails
  private LocalDate dateOfBirth; // PersonalDetails
  private String gender; // PersonalDetails
  private String qualification; // TODO: Remove when FE can handle sub-collection.
  private LocalDate dateAttained; // TODO: Remove when FE can handle sub-collection.
  private String medicalSchool; // TODO: Remove when FE can handle sub-collection.
  private String telephoneNumber; // ContactDetails
  private String mobileNumber; // ContactDetails
  private String email; // ContactDetails
  private String address1; // ContactDetails
  private String address2; // ContactDetails
  private String address3; // ContactDetails
  private String address4; // ContactDetails
  private String postCode; // ContactDetails
  private String gmcNumber; // GmcDetails
  private String gmcStatus; // GmcDetails
  private String gdcNumber; // GdcDetails
  private String gdcStatus; // GdcDetails
  private String publicHealthNumber; // Person
  private String eeaResident; // TODO:
  private String permitToWork; // TODO:
  private String settled; // TODO:
  private LocalDate visaIssued; // TODO:
  private String detailsNumber; // TODO:
  private String prevRevalBody; // TODO:
  private LocalDate currRevalDate; // TODO:
  private LocalDate prevRevalDate; // TODO:
}
