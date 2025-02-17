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

package uk.nhs.hee.trainee.details.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import uk.nhs.hee.trainee.details.model.UuidIdentifiedRecord;

class MongoConfigurationTest {

  private MongoConfiguration configuration;

  @BeforeEach
  void setUp() {
    configuration = new MongoConfiguration();
  }

  @Test
  void shouldSetUuidIdentifierBeforeConvertWhenEmpty() {
    UuidIdentifiedStub before = new UuidIdentifiedStub(null);

    BeforeConvertCallback<UuidIdentifiedStub> callback = configuration.beforeConvertCallback();
    UuidIdentifiedStub after = callback.onBeforeConvert(new UuidIdentifiedStub(null), "");

    assertThat("Unexpected entity ID.", before.id(), nullValue());
    assertThat("Unexpected entity ID.", after.id(), notNullValue());
    assertThat("Unexpected entity.", after, not(sameInstance(before)));
  }

  @Test
  void shouldNotSetUuidIdentifierBeforeConvertWhenPopulated() {
    UUID id = UUID.randomUUID();
    UuidIdentifiedStub before = new UuidIdentifiedStub(id);

    BeforeConvertCallback<UuidIdentifiedStub> callback = configuration.beforeConvertCallback();
    UuidIdentifiedStub after = callback.onBeforeConvert(before, "");

    assertThat("Unexpected entity ID.", before.id(), is(id));
    assertThat("Unexpected entity ID.", after.id(), is(id));
    assertThat("Unexpected entity.", after, sameInstance(before));
  }

  /**
   * A stub UUID identified entity
   *
   * @param id The id of the entity, may be null.
   */
  private record UuidIdentifiedStub(UUID id) implements UuidIdentifiedRecord<UuidIdentifiedStub> {

    @Override
    public UUID id() {
      return id;
    }

    @Override
    public UuidIdentifiedStub withId(UUID id) {
      return new UuidIdentifiedStub(id);
    }
  }
}
