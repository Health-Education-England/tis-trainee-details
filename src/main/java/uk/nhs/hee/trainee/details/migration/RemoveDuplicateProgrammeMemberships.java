/*
 * The MIT License (MIT)
 *
 * Copyright 2022 Crown Copyright (Health Education England)
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

import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.client.result.UpdateResult;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Delete duplicated trainee profile programme memberships caused by programme membership
 * refactoring.
 */
@Slf4j
@ChangeUnit(id = "deleteDuplicateTraineeProfileProgrammeMemberships", order = "3")
public class RemoveDuplicateProgrammeMemberships {
  private static final String PROFILE_COLLECTION = "TraineeProfile";
  private final MongoTemplate mongoTemplate;

  public RemoveDuplicateProgrammeMemberships(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  /**
   * Remove Trainee Profile Programme Memberships with an old (comma-delimited list of numbers) ID.
   */
  @Execution
  public void migrate() {
    Update update =
        new Update().pull("programmeMemberships",
            new BasicDBObject("tisId", new BasicDBObject("$regex", "^[0-9,]+$")));
    try {
      UpdateResult result = mongoTemplate.updateMulti(new Query(), update, PROFILE_COLLECTION);
      log.info("Deprecated programme memberships deleted on {} trainees",
          result.getModifiedCount());
    } catch (MongoException me) {
      log.error("Unable to delete deprecated programme memberships due to an error: {} ",
          me.toString());
    }
  }

  /**
   * Do not attempt rollback, the collection should be left as-is.
   */
  @RollbackExecution
  public void rollback() {
    log.warn("Rollback requested but not available "
        + "for 'deleteDuplicateTraineeProfileProgrammeMemberships' migration.");
  }
}
