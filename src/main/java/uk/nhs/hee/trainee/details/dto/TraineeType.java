/*
 * The MIT License (MIT)
 *
 * Copyright 2026 Crown Copyright (Health Education England)
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

import static uk.nhs.hee.trainee.details.service.ProgrammeMembershipService.isFoundationProgramme;
import static uk.nhs.hee.trainee.details.service.ProgrammeMembershipService.isPublicHealth;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;

/**
 * The type of the trainee.
 */
public enum TraineeType {
  FOUNDATION,
  PUBLIC_HEALTH,
  SPECIALTY;

  /**
   * Determine the trainee type from a programme membership.
   *
   * <p>The checks are evaluated in order: foundation, then public health. Any membership that does
   * not match either category is treated as specialty.
   *
   * @param programmeMembership the programme membership to classify
   * @return {@link #FOUNDATION} for foundation programmes, {@link #PUBLIC_HEALTH} for public health
   *     programmes, otherwise {@link #SPECIALTY}
   */
  public static TraineeType from(ProgrammeMembership programmeMembership) {
    if (isFoundationProgramme(programmeMembership)) {
      return FOUNDATION;
    }
    if (isPublicHealth(programmeMembership)) {
      return PUBLIC_HEALTH;
    }

    // Assume SPECIALTY if not FOUNDATION or PUBLIC_HEALTH.
    return SPECIALTY;
  }
}