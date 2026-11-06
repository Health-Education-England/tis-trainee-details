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

package uk.nhs.hee.trainee.details.migration;

import com.mongodb.client.result.DeleteResult;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * Delete all CCT calculations saved by the legacy calculator.
 */
@Slf4j
@ChangeUnit(id = "deleteLegacyCctCalculations", order = "5")
public class DeleteLegacyCctCalculations {

  private static final String CCT_COLLECTION = "CctCalculation";
  private final MongoTemplate mongoTemplate;

  public DeleteLegacyCctCalculations(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  /**
   * Delete all CCT calculations without programme details.
   */
  @Execution
  public void migrate() {
    Query query = Query.query(Criteria.where("programmeDetails").exists(false));
    DeleteResult result = mongoTemplate.remove(query, CCT_COLLECTION);
    log.info("Deleted {} legacy CCT calculations.", result.getDeletedCount());
  }

  /**
   * Do not attempt rollback, the deleted calculations cannot be restored.
   */
  @RollbackExecution
  public void rollback() {
    log.warn("Rollback requested but not available for 'deleteLegacyCctCalculations' migration.");
  }
}
