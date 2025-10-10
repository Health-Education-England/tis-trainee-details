/*
 * The MIT License (MIT)
 *
 * Copyright 2025 Crown Copyright (Health Education England)
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
 *
 */

package uk.nhs.hee.trainee.details.event;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.nhs.hee.trainee.details.dto.ProfileMoveEvent;
import uk.nhs.hee.trainee.details.service.CctService;

/**
 * A listener for profile move events.
 */
@Slf4j
@Component
public class ProfileMoveListener {

  private final CctService cctService;

  /**
   * Construct a listener for profile move events.
   *
   * @param cctService The CCT service.
   */
  public ProfileMoveListener(CctService cctService) {
    this.cctService = cctService;
  }

  /**
   * Handle profile move events.
   *
   * @param event The profile move event.
   */
  @SqsListener("${application.aws.sqs.profile-move}")
  public void handleProfileMove(ProfileMoveEvent event) {
    log.info("Handling profile move CCT calculations from trainee {} to trainee {}",
        event.fromTraineeId(), event.toTraineeId());

    cctService.moveCalculations(event.fromTraineeId(), event.toTraineeId());
  }
}