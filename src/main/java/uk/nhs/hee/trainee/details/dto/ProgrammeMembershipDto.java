package uk.nhs.hee.trainee.details.dto;

import lombok.Data;
import uk.nhs.hee.trainee.details.dto.enumeration.PlacementStatus;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProgrammeMembershipDto {

  private String programmeTisId;
  private String programmeName;
  private String programmeNumber;
  private String managingDeanery;
  private LocalDate startDate;
  private LocalDate endDate;
  private PlacementStatus status;
  private List<CurriculumDto> curricula;
}
