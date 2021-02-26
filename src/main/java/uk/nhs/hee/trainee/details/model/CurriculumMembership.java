package uk.nhs.hee.trainee.details.model;

import java.time.LocalDate;
import lombok.Data;

@Data
public class CurriculumMembership {

  private String curriculumMembershipTisId;
  private Curriculum curriculum;
  private LocalDate curriculumStartDate;
  private LocalDate curriculumEndDate;
}
