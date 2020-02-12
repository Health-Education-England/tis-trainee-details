package uk.nhs.hee.trainee.details.model;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.nhs.hee.trainee.details.dto.enumeration.Status;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Placement {

  private String placementTisId;
  private LocalDate startDate;
  private LocalDate endDate;
  private String site;
  private String grade;
  private String specialty;
  private String placementType;
  private Status status;
}
