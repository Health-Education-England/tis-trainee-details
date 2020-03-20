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
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "ContactDetails")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactDetails {
  @Id
  private String id;

  @Indexed(unique = true)
  private String traineeTisId;

  private String surname;

  private String forenames;

  private String knownAs;

  private String maidenName;

  private String title;

  private String gender;

  private LocalDate dateOfBirth;

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
  @Builder.Default
  private List<ProgrammeMembership> programmeMemberships = new ArrayList<>();
  @Builder.Default
  private List<Placement> placements = new ArrayList<>();
}
