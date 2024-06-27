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

package uk.nhs.hee.trainee.details.migration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mongodb.MongoException;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

class RemoveStoredNtnsTest {

  private static final String PROFILE_COLLECTION = "TraineeProfile";
  private RemoveStoredNtns migration;
  private MongoTemplate template;

  @BeforeEach
  void setUp() {
    template = mock(MongoTemplate.class);
    migration = new RemoveStoredNtns(template);
  }

  @Test
  void shouldRemoveStoredNtns() {
    ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.captor();
    ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.captor();
    UpdateResult result = mock(UpdateResult.class);

    when(template.updateMulti(queryCaptor.capture(), updateCaptor.capture(),
        eq(PROFILE_COLLECTION))).thenReturn(result);
    when(result.getModifiedCount()).thenReturn(1L);

    migration.migrate();

    Document query = queryCaptor.getValue().getQueryObject();
    assertThat("Unexpected query size.", query.size(), is(1));
    assertThat("Unexpected query key.", query.keySet(), hasItem("programmeMemberships.ntn"));

    Document criteria = query.get("programmeMemberships.ntn", Document.class);
    assertThat("Unexpected criteria size.", criteria.size(), is(1));
    assertThat("Unexpected criteria key.", criteria.keySet(), hasItem("$exists"));
    assertThat("Unexpected criteria value.", criteria.get("$exists"), is(true));

    Document update = updateCaptor.getValue().getUpdateObject();
    assertThat("Unexpected unset size.", update.size(), is(1));
    assertThat("Unexpected unset key.", update.keySet(), hasItem("$unset"));

    Document unset = update.get("$unset", Document.class);
    assertThat("Unexpected unset size.", unset.size(), is(1));
    assertThat("Unexpected unset key.", unset.keySet(), hasItem("programmeMemberships.$[].ntn"));
    assertThat("Unexpected unset value.", unset.get("programmeMemberships.$[].ntn"), is(1));
  }

  @Test
  void shouldCatchMongoExceptionNotThrowIt() {
    when(template.updateMulti(any(), any(), (String) any()))
        .thenThrow(new MongoException("exception"));
    Assertions.assertDoesNotThrow(() -> migration.migrate());
  }

  @Test
  void shouldNotAttemptRollback() {
    migration.rollback();
    verifyNoInteractions(template);
  }
}
