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
import lombok.Data;
import uk.nhs.hee.trainee.details.dto.enumeration.Status;

@Data
public class ProgrammeMembership {

  private String tisId;
  private String ntn;
  private String programmeTisId;
  private String programmeName;
  private String programmeNumber;
  private String managingDeanery;
  private String programmeMembershipType;
  private LocalDate startDate;
  private LocalDate endDate;
  private LocalDate programmeCompletionDate;
  private List<Curriculum> curricula = new ArrayList<>();
  private String trainingPathway; // TODO: sync from TIS
  private ConditionsOfJoining conditionsOfJoining;

  /**
   * Get programme status according to programme startDate and endDate.
   */
  public Status getStatus() {
    if (this.startDate == null || this.endDate == null) {
      return null;
    }

    LocalDate today = LocalDate.now();
    if (today.isBefore(this.startDate)) {
      return Status.FUTURE;
    } else if (today.isAfter(this.endDate)) {
      return Status.PAST;
    }
    return Status.CURRENT;
  }
}
