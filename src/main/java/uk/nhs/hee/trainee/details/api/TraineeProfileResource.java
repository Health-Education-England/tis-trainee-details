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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsDto;
import uk.nhs.hee.trainee.details.dto.TraineeProfileDto;
import uk.nhs.hee.trainee.details.mapper.TraineeProfileMapper;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.service.TraineeProfileService;

@Slf4j
@RestController
@RequestMapping("/api/trainee-profile")
public class TraineeProfileResource {

  private static final String TIS_ID_ATTRIBUTE = "custom:tisId";

  private final TraineeProfileService service;
  private final TraineeProfileMapper mapper;
  private final ObjectMapper objectMapper;

  protected TraineeProfileResource(TraineeProfileService service, TraineeProfileMapper mapper,
      ObjectMapper objectMapper) {
    this.service = service;
    this.mapper = mapper;
    this.objectMapper = objectMapper;
  }

  /**
   * Get a trainee's profile.
   *
   * @param token The authorization token from the request header.
   * @return The {@link PersonalDetailsDto} representing the trainee profile.
   */
  @GetMapping
  public ResponseEntity<TraineeProfileDto> getTraineeProfile(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
    log.trace("Trainee Profile of authenticated user.");

    String[] tokenSections = token.split("\\.");
    byte[] payloadBytes = Base64.getDecoder()
        .decode(tokenSections[1].getBytes(StandardCharsets.UTF_8));
    String tisId;

    try {
      Map<?, ?> payload = objectMapper.readValue(payloadBytes, Map.class);
      tisId = (String) payload.get(TIS_ID_ATTRIBUTE);
    } catch (IOException e) {
      log.warn("Unable to read tisId from token.", e);
      return ResponseEntity.badRequest().build();
    }

    TraineeProfile traineeProfile = service.getTraineeProfileByTraineeTisId(tisId);

    if (traineeProfile == null) {
      log.warn("Trainee profile not found for id {}.", tisId);
      return ResponseEntity.notFound().build();
    }

    traineeProfile = service.hidePastProgrammes(traineeProfile);
    traineeProfile = service.hidePastPlacements(traineeProfile);
    return ResponseEntity.ok(mapper.toDto(traineeProfile));
  }

  /**
   * Get a trainee's ID from an email address.
   *
   * @param email The email to search by.
   * @return The trainee's ID.
   */
  @GetMapping("/trainee-id")
  public ResponseEntity<String> getTraineeId(@RequestParam String email) {
    Optional<String> traineeId = service.getTraineeTisIdByByEmail(email);
    return ResponseEntity.of(traineeId);
  }
}
