package uk.nhs.hee.trainee.details.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import uk.nhs.hee.trainee.details.dto.enumeration.Status;


@Data
public class ProgrammeMembershipDto {

  private String programmeTisId;
  private String programmeName;
  private String programmeNumber;
  private String managingDeanery;
  private LocalDate startDate;
  private LocalDate endDate;
  private Status status;
  private List<CurriculumDto> curricula;
}
