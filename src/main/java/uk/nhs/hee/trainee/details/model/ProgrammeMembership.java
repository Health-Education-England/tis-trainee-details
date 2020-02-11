package uk.nhs.hee.trainee.details.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.nhs.hee.trainee.details.dto.enumeration.Status;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgrammeMembership {

  private String programmeTisId;
  private String programmeName;
  private String programmeNumber;
  private String managingDeanery;
  private LocalDate startDate;
  private LocalDate endDate;
  @Builder.Default
  private List<Curriculum> curricula = new ArrayList<>();

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
