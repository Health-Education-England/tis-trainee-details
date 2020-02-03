package uk.nhs.hee.trainee.details.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Curriculum {

  private String curriculumTisId;
  private String curriculumName;
}
