package uk.nhs.hee.trainee.details.dto;

import lombok.Data;

import java.time.LocalDate;
import uk.nhs.hee.trainee.details.dto.enumeration.Status;

@Data
public class PlacementDto {

  private String placementTisId;
  private LocalDate startDate;
  private LocalDate endDate;
  private String site;
  private String grade;
  private String specialty;
  private String placementType;
  private Status status;
}
