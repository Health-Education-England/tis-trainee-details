/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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

package uk.nhs.hee.trainee.details.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.data.domain.Sort.Direction.ASC;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.nhs.hee.trainee.details.DockerImageNames;
import uk.nhs.hee.trainee.details.model.CctCalculation;

@DataMongoTest
@Testcontainers(disabledWithoutDocker = true)
class CctCalculationRepositoryIntegrationTest {

  @Container
  @ServiceConnection
  private static final MongoDBContainer mongoContainer = new MongoDBContainer(
      DockerImageNames.MONGO);

  @Autowired
  private MongoTemplate template;

  @Test
  void shouldHaveIndexes() {
    IndexOperations indexOps = template.indexOps(CctCalculation.class);

    assertThat("Unexpected index count.", indexOps.getIndexInfo().size(), is(3));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      _id_                   | _id
      traineeId              | traineeId
      programmeMembership.id | programmeMembership.id
      """)
  void shouldHaveIndex(String name, String key) {
    IndexOperations indexOps = template.indexOps(CctCalculation.class);

    IndexInfo indexInfo = indexOps.getIndexInfo().stream()
        .filter(idx -> idx.getName().equals(name))
        .findAny()
        .orElseThrow(() -> new AssertionError("Expected index not found."));

    assertThat("Unexpected index name.", indexInfo.getName(), is(name));
    assertThat("Unexpected index hashed flag.", indexInfo.isHashed(), is(false));
    assertThat("Unexpected index hidden flag.", indexInfo.isHidden(), is(false));
    assertThat("Unexpected index sparse flag.", indexInfo.isSparse(), is(false));
    assertThat("Unexpected index unique flag.", indexInfo.isUnique(), is(false));
    assertThat("Unexpected index wildcard flag.", indexInfo.isWildcard(), is(false));

    List<IndexField> indexFields = indexInfo.getIndexFields();
    assertThat("Unexpected index field count.", indexFields.size(), is(1));

    IndexField indexField = indexFields.get(0);
    assertThat("Unexpected index field key.", indexField.getKey(), is(key));
    assertThat("Unexpected index field direction.", indexField.getDirection(), is(ASC));
  }
}
