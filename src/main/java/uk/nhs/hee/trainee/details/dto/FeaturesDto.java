/*
 * The MIT License (MIT)
 *
 * Copyright 2025 Crown Copyright (Health Education England)
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

package uk.nhs.hee.trainee.details.dto;

import java.util.List;
import java.util.Set;
import lombok.Builder;

/**
 * A DTO for trainee details feature flags.
 */
@Builder
public record FeaturesDto(
    Feature actions,
    Feature cct,
    DetailsFeatures details,
    FormFeatures forms,
    Feature notifications,

    // TODO: The API is unversioned, so these are needed until the client is updated.
    boolean ltft,
    List<String> ltftProgrammes) {


  /**
   * Generic feature flag.
   *
   * @param enabled Whether the feature is enabled.
   */
  public record Feature(boolean enabled) {

  }

  /**
   * A sub-set of details flags.
   *
   * @param enabled    Whether details are enabled.
   * @param placements Placement feature flag.
   * @param profile    A sub-set of profile features.
   * @param programmes A sub-set of programme features.
   */
  @Builder
  public record DetailsFeatures(
      boolean enabled,
      Feature placements,
      ProfileFeatures profile,
      ProgrammeFeatures programmes) {

    /**
     * A sub-set of profile features.
     *
     * @param enabled   Whether the profile is enabled.
     * @param gmcUpdate GMC Update feature flag.
     */
    @Builder
    public record ProfileFeatures(
        boolean enabled,
        Feature gmcUpdate) {

    }

    /**
     * A sub-set of programme features.
     *
     * @param enabled             Whether programme features are enabled.
     * @param conditionsOfJoining Conditions of Joining feature flag.
     * @param confirmation        Programme confirmation feature flag.
     */
    @Builder
    public record ProgrammeFeatures(
        boolean enabled,
        Feature conditionsOfJoining,
        Feature confirmation) {

    }
  }

  /**
   * A sub-set of form features.
   *
   * @param enabled Whether form features are enabled.
   * @param formr   Form-R feature flags.
   * @param ltft    A sub-set of LTFT features.
   */
  @Builder
  public record FormFeatures(
      boolean enabled,
      Feature formr,
      LtftFeatures ltft) {

    /**
     * A sub-set of LTFT features.
     *
     * @param enabled              Whether LTFT is enabled.
     * @param qualifyingProgrammes A set of programmes which qualify for LTFT.
     */
    @Builder
    public record LtftFeatures(
        boolean enabled,
        Set<String> qualifyingProgrammes) {

    }
  }
}
