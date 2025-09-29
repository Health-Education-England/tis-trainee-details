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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.With;

/**
 * A DTO for trainee details feature flags.
 */
@With
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
   * Create a set of feature flags with all features enabled, excluding any pilot features.
   *
   * @return The created set of feature flags.
   */
  public static FeaturesDto enable() {
    return new FeaturesDto(
        Feature.enable(),
        Feature.enable(),
        DetailsFeatures.enable(),
        FormFeatures.enable(),
        Feature.enable(),
        false,
        List.of()
    );
  }

  /**
   * Create a set of feature flags with all features enabled, as well as LTFT pilot features.
   *
   * @param ltftProgrammeIds A list of IDs for LTFT qualifying programmes.
   * @return The created set of feature flags.
   */
  public static FeaturesDto enableWithLtftPilot(List<String> ltftProgrammeIds) {
    return enable()
        .withLtft(true)
        .withLtftProgrammes(List.copyOf(ltftProgrammeIds))
        .withForms(FormFeatures.enableWithLtftPilot(ltftProgrammeIds));
  }

  /**
   * Create a set of feature flags with only read-only features enabled. Any functionality with
   * persistence or side effects are disabled.
   *
   * @return The created set of feature flags.
   */
  public static FeaturesDto readOnly() {
    return new FeaturesDto(
        Feature.disable(),
        Feature.disable(),
        DetailsFeatures.readOnly(),
        FormFeatures.disable(),
        Feature.disable(),
        false,
        List.of()
    );
  }

  /**
   * Create a set of feature flags with all features disabled.
   *
   * @return The created set of feature flags.
   */
  public static FeaturesDto disable() {
    return new FeaturesDto(
        Feature.disable(),
        Feature.disable(),
        DetailsFeatures.disable(),
        FormFeatures.disable(),
        Feature.disable(),
        false,
        List.of()
    );
  }

  /**
   * Generic feature flag.
   *
   * @param enabled Whether the feature is enabled.
   */
  public record Feature(boolean enabled) {

    /**
     * Create an enabled feature.
     *
     * @return The enabled feature.
     */
    private static Feature enable() {
      return new Feature(true);
    }

    /**
     * Create a disabled feature.
     *
     * @return The disabled feature.
     */
    private static Feature disable() {
      return new Feature(false);
    }
  }

  /**
   * A sub-set of details flags.
   *
   * @param enabled    Whether details are enabled.
   * @param placements Placement feature flag.
   * @param profile    A sub-set of profile features.
   * @param programmes A sub-set of programme features.
   */
  public record DetailsFeatures(
      boolean enabled,
      Feature placements,
      ProfileFeatures profile,
      ProgrammeFeatures programmes) {

    /**
     * Create a set of Details feature flags with all features enabled, excluding any pilot
     * features.
     *
     * @return The created set of feature flags.
     */
    private static DetailsFeatures enable() {
      return new DetailsFeatures(
          true,
          Feature.enable(),
          ProfileFeatures.enable(),
          ProgrammeFeatures.enable()
      );
    }

    /**
     * Create a set of Details feature flags with only read-only features enabled. Any functionality
     * with persistence or side effects are disabled.
     *
     * @return The created set of feature flags.
     */
    private static DetailsFeatures readOnly() {
      return new DetailsFeatures(
          true,
          Feature.enable(),
          ProfileFeatures.readOnly(),
          ProgrammeFeatures.readOnly()
      );
    }

    /**
     * Create a set of Details feature flags with all features disabled.
     *
     * @return The created set of feature flags.
     */
    private static DetailsFeatures disable() {
      return new DetailsFeatures(
          false,
          Feature.disable(),
          ProfileFeatures.disable(),
          ProgrammeFeatures.disable()
      );
    }

    /**
     * A sub-set of profile features.
     *
     * @param enabled   Whether the profile is enabled.
     * @param gmcUpdate GMC Update feature flag.
     */
    public record ProfileFeatures(
        boolean enabled,
        Feature gmcUpdate) {

      /**
       * Create a set of Profile feature flags with all features enabled, excluding any pilot
       * features.
       *
       * @return The created set of feature flags.
       */
      private static ProfileFeatures enable() {
        return new ProfileFeatures(
            true,
            Feature.enable()
        );
      }

      /**
       * Create a set of Profile feature flags with only read-only features enabled. Any
       * functionality with persistence or side effects are disabled.
       *
       * @return The created set of feature flags.
       */
      private static ProfileFeatures readOnly() {
        return new ProfileFeatures(
            true,
            Feature.disable()
        );
      }

      /**
       * Create a set of Profile feature flags with all features disabled.
       *
       * @return The created set of feature flags.
       */
      private static ProfileFeatures disable() {
        return new ProfileFeatures(
            false,
            Feature.disable()
        );
      }
    }

    /**
     * A sub-set of programme features.
     *
     * @param enabled             Whether programme features are enabled.
     * @param conditionsOfJoining Conditions of Joining feature flag.
     * @param confirmation        Programme confirmation feature flag.
     */
    public record ProgrammeFeatures(
        boolean enabled,
        Feature conditionsOfJoining,
        Feature confirmation) {

      /**
       * Create a set of Programme feature flags with all features enabled, excluding any pilot
       * features.
       *
       * @return The created set of feature flags.
       */
      private static ProgrammeFeatures enable() {
        return new ProgrammeFeatures(
            true,
            Feature.enable(),
            Feature.enable()
        );
      }

      /**
       * Create a set of Programme feature flags with only read-only features enabled. Any
       * functionality with persistence or side effects are disabled.
       *
       * @return The created set of feature flags.
       */
      private static ProgrammeFeatures readOnly() {
        return new ProgrammeFeatures(
            true,
            Feature.disable(),
            Feature.disable()
        );
      }

      /**
       * Create a set of Programme feature flags with all features disabled.
       *
       * @return The created set of feature flags.
       */
      private static ProgrammeFeatures disable() {
        return new ProgrammeFeatures(
            false,
            Feature.disable(),
            Feature.disable()
        );
      }
    }
  }

  /**
   * A sub-set of form features.
   *
   * @param enabled Whether form features are enabled.
   * @param formr   Form-R feature flags.
   * @param ltft    A sub-set of LTFT features.
   */
  @With
  public record FormFeatures(
      boolean enabled,
      Feature formr,
      LtftFeatures ltft) {

    /**
     * Create a set of Form feature flags with all features enabled, excluding any pilot features.
     *
     * @return The created set of feature flags.
     */
    private static FormFeatures enable() {
      return new FormFeatures(
          true,
          Feature.enable(),
          LtftFeatures.disable()
      );
    }

    /**
     * Create a set of Form feature flags with all features enabled, as well as LTFT pilot
     * features.
     *
     * @param ltftProgrammeIds A list of IDs for LTFT qualifying programmes.
     * @return The created set of feature flags.
     */
    private static FormFeatures enableWithLtftPilot(Collection<String> ltftProgrammeIds) {
      return enable()
          .withLtft(LtftFeatures.enable(ltftProgrammeIds));
    }

    /**
     * Create a set of Form feature flags with all features disabled.
     *
     * @return The created set of feature flags.
     */
    private static FormFeatures disable() {
      return new FormFeatures(
          false,
          Feature.disable(),
          LtftFeatures.disable()
      );
    }

    /**
     * A sub-set of LTFT features.
     *
     * @param enabled              Whether LTFT is enabled.
     * @param qualifyingProgrammes A set of programmes which qualify for LTFT.
     */
    public record LtftFeatures(
        boolean enabled,
        Set<String> qualifyingProgrammes) {

      /**
       * Create a set of LTFT feature flags with all features enabled.
       *
       * @return The created set of feature flags.
       */
      private static LtftFeatures enable(Collection<String> programmeIds) {
        return new LtftFeatures(
            true,
            Set.copyOf(programmeIds)
        );
      }

      /**
       * Create a set of LTFT feature flags with all features disabled.
       *
       * @return The created set of feature flags.
       */
      private static LtftFeatures disable() {
        return new LtftFeatures(
            false,
            Set.of()
        );
      }
    }
  }
}
