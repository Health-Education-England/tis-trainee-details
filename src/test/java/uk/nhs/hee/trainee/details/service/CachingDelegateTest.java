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

package uk.nhs.hee.trainee.details.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.trainee.details.dto.enumeration.GoldGuideVersion;
import uk.nhs.hee.trainee.details.model.ConditionsOfJoining;

class CachingDelegateTest {

  private CachingDelegate delegate;

  @BeforeEach
  void setUp() {
    delegate = new CachingDelegate();
  }

  @Test
  void shouldReturnCachedConditionsOfJoining() {
    ConditionsOfJoining cachedCoj
        = new ConditionsOfJoining(Instant.now(), GoldGuideVersion.GG10, Instant.now());
    ConditionsOfJoining returnedCoj = delegate.cacheConditionsOfJoining("40", cachedCoj);
    assertThat("Unexpected Conditions of Joining.", cachedCoj, is(returnedCoj));
  }

  @Test
  void shouldGetEmptyConditionsOfJoining() {
    Optional<ConditionsOfJoining> coj = delegate.getConditionsOfJoining("40");
    assertThat("Unexpected Conditions of Joining.", coj, is(Optional.empty()));
  }
}
