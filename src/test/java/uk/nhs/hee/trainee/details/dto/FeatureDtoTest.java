/*
 * The MIT License (MIT)
 *
 * Copyright 2026 Crown Copyright (Health Education England)
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

import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class FeaturesDtoTest {

  @Test
  void emailUpdateShouldBeEnabledWhenAllFeaturesEnabled() {
    FeaturesDto features = FeaturesDto.enable();
    assertThat("emailUpdate should be enabled when all features are enabled.",
        features.details().profile().emailUpdate().enabled(), is(true));
  }

  @Test
  void emailUpdateShouldBeDisabledWhenAllFeaturesDisabled() {
    FeaturesDto features = FeaturesDto.disable();
    assertThat("emailUpdate should be disabled when all features are disabled.",
        features.details().profile().emailUpdate().enabled(), is(false));
  }

  @Test
  void emailUpdateShouldBeDisabledInReadOnlyMode() {
    FeaturesDto features = FeaturesDto.readOnly();
    assertThat("emailUpdate should be disabled in read-only mode.",
        features.details().profile().emailUpdate().enabled(), is(false));
  }

  @Test
  void emailUpdateShouldReflectCustomProfileFeatures() {
    FeaturesDto.Feature enabled = new FeaturesDto.Feature(true);
    FeaturesDto.Feature disabled = new FeaturesDto.Feature(false);

    FeaturesDto.DetailsFeatures.ProfileFeatures profileFeatures =
        new FeaturesDto.DetailsFeatures.ProfileFeatures(true, enabled, disabled);

    assertThat("emailUpdate should be enabled.",
        profileFeatures.emailUpdate().enabled(), is(true));
    assertThat("gmcUpdate should be disabled.",
        profileFeatures.gmcUpdate().enabled(), is(false));
  }
}
