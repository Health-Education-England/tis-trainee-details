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

package uk.nhs.hee.trainee.details.config;

import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import uk.nhs.hee.trainee.details.model.CctCalculation;
import uk.nhs.hee.trainee.details.model.CctCalculation.CctChange;
import uk.nhs.hee.trainee.details.model.UuidIdentifiedRecord;

/**
 * Configuration for the Mongo database.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfiguration {

  /**
   * Populate the UUID-based ID field before conversion.
   *
   * @param <T> The type of the entity, extending {@link UuidIdentifiedRecord}.
   * @return The updated entity.
   */
  @Bean
  public <T extends UuidIdentifiedRecord<T>> BeforeConvertCallback<T> populateUuidBeforeConvert() {
    return (entity, collection) -> {
      if (entity.id() == null) {
        entity = entity.withId(UUID.randomUUID());
      }
      return entity;
    };
  }

  /**
   * Populate the {@link CctChange} IDs before conversion.
   *
   * @return The updated {@link CctCalculation}.
   */
  @Bean
  public BeforeConvertCallback<CctCalculation> populateChangeUuidBeforeConvert() {
    return (entity, collection) -> {

      if (entity.changes() != null) {
        List<CctChange> changes = entity.changes().stream()
            .map(change -> change.id() != null ? change : change.withId(UUID.randomUUID()))
            .toList();
        entity = entity.withChanges(changes);
      }

      return entity;
    };
  }
}
