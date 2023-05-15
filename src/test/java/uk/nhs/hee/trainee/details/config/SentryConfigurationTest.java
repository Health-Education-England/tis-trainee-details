/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
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

package uk.nhs.hee.trainee.details.config;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import io.sentry.Sentry;
import io.sentry.protocol.Contexts;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.trainee.details.config.EcsMetadataConfiguration.EcsMetadata;

class SentryConfigurationTest {

  @Test
  void shouldNotAddEcsMetadataToScopeWhenNotAvailable() {
    SentryConfiguration configuration = new SentryConfiguration(Optional.empty());

    configuration.configureScope();

    Sentry.withScope(scope -> {
      Contexts contexts = scope.getContexts();
      Object context = contexts.get("EcsMetadata");
      assertThat("Unexpected Sentry context.", context, nullValue());
    });
  }

  @Test
  void shouldAddEcsMetadataToScopeWhenAvailable() {
    EcsMetadata ecsMetadata = new EcsMetadata(null, null);
    SentryConfiguration configuration = new SentryConfiguration(Optional.of(ecsMetadata));

    configuration.configureScope();

    Sentry.withScope(scope -> {
      Contexts contexts = scope.getContexts();
      Object context = contexts.get("EcsMetadata");
      assertThat("Unexpected Sentry context.", context, sameInstance(ecsMetadata));
    });
  }
}
