/*
 * The MIT License (MIT)
 *
 * Copyright 2020 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.trainee.details.model;

import java.time.LocalDate;
import java.util.Set;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Unwrapped;
import org.springframework.data.mongodb.core.mapping.Unwrapped.OnEmpty;
import uk.nhs.hee.trainee.details.dto.enumeration.Status;

@Data
public class Placement {

  private String tisId;
  private LocalDate startDate;
  private LocalDate endDate;
  @Unwrapped(onEmpty = OnEmpty.USE_NULL)
  private Site site;
  private Set<Site> otherSites;
  private String grade;
  private String specialty;
  private String subSpecialty;
  private Boolean postAllowsSubspecialty;
  private Set<Specialty> otherSpecialties;
  private String placementType;
  private String employingBody;
  private String trainingBody;
  private String wholeTimeEquivalent;
  private Status status;
}
