package uk.nhs.hee.trainee.details.dto;

import lombok.Data;
import uk.nhs.hee.trainee.details.dto.enumeration.PlacementStatus;

import java.time.LocalDate;

@Data
public class PlacementDto {

  private String placementTisId;
  private LocalDate startDate;
  private LocalDate endDate;
  private String site;
  private String grade;
  private String specialty;
  private String placementType;
  private PlacementStatus status;
}
