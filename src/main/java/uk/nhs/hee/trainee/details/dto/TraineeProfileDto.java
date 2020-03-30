package uk.nhs.hee.trainee.details.dto;

import java.util.List;
import lombok.Data;

/**
 * A DTO for TraineeProfile entity
 * Holds the fields for all the information of the trainee
 */
@Data
public class TraineeProfileDto {

  private String id;
  private String traineeTisId;
  private PersonalDetailsDto personalDetails;
  private List<ProgrammeMembershipDto> programmeMemberships;
  private List<PlacementDto> placements;

}
