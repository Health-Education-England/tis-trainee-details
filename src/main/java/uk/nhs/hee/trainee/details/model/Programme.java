package uk.nhs.hee.trainee.details.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "Programmes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Programme {

  @Id
  private String id;

  @Field(value = "programmeTisId")
  private String programmeTisId;

  @Field(value = "programmeName")
  private String programmeName;

  @Field(value = "programmeNumber")
  private String programmeNumber;

  @Field(value = "managingDeanery")
  private String managingDeanery;

}
