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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.nhs.hee.trainee.details.config.MongoConfiguration;
import uk.nhs.hee.trainee.details.dto.enumeration.GoldGuideVersion;
import uk.nhs.hee.trainee.details.model.ConditionsOfJoining;

// TODO: reconfigure testcontainers
@SpringBootTest(properties = "embedded.containers.enabled=true")
@ActiveProfiles({"test", "redis"})
@Testcontainers(disabledWithoutDocker = true)
class CachingDelegateIntegrationTest {

  @MockBean
  private MongoConfiguration mongoConfiguration;

  @Autowired
  private CachingDelegate delegate;

  @Value("${application.timezone")
  private ZoneId timezone;

  @Test
  void shouldReturnEmptyConditionsOfJoiningWhenNotCached() {
    String key = UUID.randomUUID().toString();

    Optional<ConditionsOfJoining> cachedOptional = delegate.getConditionsOfJoining(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.empty()));
  }

  @Test
  void shouldReturnCachedConditionsOfJoiningAfterCaching() {
    String key = UUID.randomUUID().toString();

    ConditionsOfJoining coj
        = new ConditionsOfJoining(Instant.now(), GoldGuideVersion.GG9, Instant.now());
    ConditionsOfJoining cachedCoj = delegate.cacheConditionsOfJoining(key, coj);
    assertThat("Unexpected cached value.", cachedCoj, is(coj));
  }

  @Test
  void shouldGetCachedConditionsOfJoiningWhenCached() {
    String key = UUID.randomUUID().toString();

    ConditionsOfJoining coj
        = new ConditionsOfJoining(Instant.now(), GoldGuideVersion.GG9, Instant.now());
    delegate.cacheConditionsOfJoining(key, coj);

    Optional<ConditionsOfJoining> cachedOptional = delegate.getConditionsOfJoining(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.of(coj)));
  }

  @Test
  void shouldRemoveConditionsOfJoiningWhenRetrieved() {
    String key = UUID.randomUUID().toString();

    ConditionsOfJoining coj
        = new ConditionsOfJoining(Instant.now(), GoldGuideVersion.GG9, Instant.now());
    delegate.cacheConditionsOfJoining(key, coj);

    // Ignore this result, the cached value should be evicted.
    delegate.getConditionsOfJoining(key);

    Optional<ConditionsOfJoining> cachedOptional = delegate.getConditionsOfJoining(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.empty()));
  }

  @Test
  void shouldReturnEmptyJobCompletionWhenNotCached() {
    String key = UUID.randomUUID().toString();

    Optional<LocalDate> cachedOptional = delegate.getLastJobCompletion(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.empty()));
  }

  @Test
  void shouldReturnCachedJobCompletionAfterCaching() {
    String key = UUID.randomUUID().toString();

    LocalDate cachedCompletion = delegate.cacheJobCompletion(key);
    assertThat("Unexpected cached value.", cachedCompletion, is(LocalDate.now(timezone)));
  }

  @Test
  void shouldGetCachedJobCompletionWhenCached() {
    String key = UUID.randomUUID().toString();

    delegate.cacheJobCompletion(key);

    Optional<LocalDate> cachedOptional = delegate.getLastJobCompletion(key);
    assertThat("Unexpected cached value.", cachedOptional,
        is(Optional.of(LocalDate.now(timezone))));
  }
}
