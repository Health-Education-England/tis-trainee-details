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

package uk.nhs.hee.trainee.details.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.trainee.details.dto.ContactDetailsDto;
import uk.nhs.hee.trainee.details.mapper.ContactDetailsMapper;
import uk.nhs.hee.trainee.details.model.ContactDetails;
import uk.nhs.hee.trainee.details.service.ContactDetailsService;

@RestController
@RequestMapping("/api")
public class ContactDetailsResource {

  private static final Logger log = LoggerFactory.getLogger(ContactDetailsResource.class);
  @Autowired
  private ContactDetailsService contactDetailsService;
  @Autowired
  private ContactDetailsMapper contactDetailsMapper;

  /**
   * Get a contact details record with a given ID.
   *
   * @param contactDetailsId The ID of the contact details to get.
   * @return The {@link ContactDetailsDto} representing the contact details.
   */
  @GetMapping("/contactdetails/{id}")
  public ContactDetailsDto getContactDetailsById(
      @PathVariable(name = "id") String contactDetailsId) {
    log.trace("Contact Details of a trainee by contactDetailsId {}", contactDetailsId);
    ContactDetails contactDetails = contactDetailsService.getContactDetails(contactDetailsId);
    contactDetails = contactDetailsService.hidePastProgrammes(contactDetails);
    contactDetails = contactDetailsService.hidePastPlacements(contactDetails);
    return contactDetailsMapper.toDto(contactDetails);
  }

  /**
   * Get a trainee's contact details based on the trainee's ID.
   *
   * @param traineeId The trainee's ID.
   * @return The {@link ContactDetailsDto} representing the trainee's contact details.
   */
  @GetMapping("/contactdetails/trainee/{traineeId}")
  public ContactDetailsDto getContactDetailsByTraineeId(
      @PathVariable(name = "traineeId") String traineeId) {
    log.trace("Contact Details of a trainee by traineeId {}", traineeId);
    ContactDetails contactDetails = contactDetailsService
        .getContactDetailsByTraineeTisId(traineeId);
    contactDetails = contactDetailsService.hidePastProgrammes(contactDetails);
    contactDetails = contactDetailsService.hidePastPlacements(contactDetails);
    return contactDetailsMapper.toDto(contactDetails);
  }

  @PostMapping("/contactdetails/")
  public ContactDetails addContactDetails(@RequestBody ContactDetails contactDetails) {

    return null;
  }
}
