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

import com.amazonaws.xray.spring.aop.XRayEnabled;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.trainee.details.dto.LocalOfficeContact;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsDto;
import uk.nhs.hee.trainee.details.dto.TraineeIdentity;
import uk.nhs.hee.trainee.details.dto.TraineeProfileDto;
import uk.nhs.hee.trainee.details.dto.UserDetails;
import uk.nhs.hee.trainee.details.mapper.TraineeProfileMapper;
import uk.nhs.hee.trainee.details.model.LocalOfficeContactType;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.service.TraineeProfileService;

@Slf4j
@RestController
@RequestMapping("/api/trainee-profile")
@XRayEnabled
public class TraineeProfileResource {

  private final TraineeProfileService service;
  private final TraineeProfileMapper mapper;
  private final TraineeIdentity traineeIdentity;

  protected TraineeProfileResource(TraineeProfileService service, TraineeProfileMapper mapper,
      TraineeIdentity traineeIdentity) {
    this.service = service;
    this.mapper = mapper;
    this.traineeIdentity = traineeIdentity;
  }

  /**
   * Get a trainee's profile.
   *
   * @return The {@link PersonalDetailsDto} representing the trainee profile.
   */
  @GetMapping
  public ResponseEntity<TraineeProfileDto> getTraineeProfile() {
    log.info("Trainee Profile of authenticated user.");
    String tisId = traineeIdentity.getTraineeId();

    if (tisId == null) {
      log.warn("No trainee ID provided.");
      return ResponseEntity.badRequest().build();
    }

    TraineeProfile traineeProfile = service.getTraineeProfileByTraineeTisId(tisId);

    if (traineeProfile == null) {
      log.warn("Trainee profile not found for id {}.", tisId);
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(mapper.toDto(traineeProfile));
  }

  /**
   * Get the trainee IDs for an email address.
   *
   * @param email The email to search by.
   * @return The matching trainee IDs.
   */
  @GetMapping("/trainee-ids")
  public ResponseEntity<List<String>> getTraineeIds(@RequestParam String email) {
    List<String> traineeIds = service.getTraineeTisIdsByEmail(email);

    if (traineeIds.isEmpty()) {
      return ResponseEntity.notFound().build();
    } else {
      return ResponseEntity.ok(traineeIds);
    }
  }

  /**
   * Delete a trainee profile.
   *
   * @param tisId The TIS ID of the trainee to delete the profile for.
   * @return Response code 204 if successful.
   */
  @DeleteMapping("/{tisId}")
  public ResponseEntity<Void> deleteTraineeProfile(@PathVariable String tisId) {
    service.deleteTraineeProfileByTraineeTisId(tisId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Verify the trainee against the provided details.
   *
   * @param email The email to match.
   * @param gmc   The GMC number to match.
   * @param dob   The date of birth to match.
   * @return The matching trainee ID, or 404 if not verified or not unique
   */
  @GetMapping("/trainee-verify")
  public ResponseEntity<String> getVerifiedTraineeIds(@NotNull @RequestParam String email,
      @NotNull @RequestParam String gmc,
      @NotNull @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dob) {
    log.info("Request to verify trainee ID matching email '{}', GMC '{}' and DOB '{}'", email, gmc,
        dob.toString());
    List<String> traineeIds = service.getTraineeTisIdsByEmailGmcAndDob(email, gmc, dob);

    if (traineeIds.size() != 1) {
      //trainee cannot be uniquely identified
      log.info("Trainee could not be uniquely verified ({} matches)", traineeIds.size());
      return ResponseEntity.notFound().build();
    } else {
      log.info("Trainee verified with ID {}", traineeIds.get(0));
      return ResponseEntity.ok(traineeIds.get(0));
    }
  }

  /**
   * Get the details for a trainee TIS ID.
   *
   * @param tisId The TIS ID to search by.
   * @return The matching trainee user details, or not found if not found.
   */
  @GetMapping("/account-details/{tisId}")
  public ResponseEntity<UserDetails> getTraineeDetails(@PathVariable String tisId) {
    return ResponseEntity.of(service.getTraineeDetailsByTisId(tisId));
  }

  /**
   * Get the current local office contact(s) of the given type for a trainee TIS ID.
   *
   * @param tisId       The TIS ID to search by.
   * @param contactType The local office contact type to search by.
   * @return The distinct matching local office contact details, or not found if not found.
   */
  @GetMapping("/local-office-contacts/{tisId}/{contactType}")
  public ResponseEntity<Set<LocalOfficeContact>> getTraineeLocalOfficeContacts(
      @PathVariable String tisId, @PathVariable LocalOfficeContactType contactType) {
    return ResponseEntity.of(service.getTraineeLocalOfficeContacts(tisId, contactType));
  }
}
