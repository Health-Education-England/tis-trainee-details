/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
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

package uk.nhs.hee.trainee.details.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.nhs.hee.trainee.details.dto.enumeration.Status;

class ProgrammeMembershipTest {

  @ParameterizedTest
  @CsvSource({", 1970-01-01", "1970-01-01,", ","})
  void shouldReturnNullWhenDatesNull(LocalDate startDate, LocalDate endDate) {
    var programmeMembership = new ProgrammeMembership();
    programmeMembership.setStartDate(startDate);
    programmeMembership.setEndDate(endDate);

    Status status = programmeMembership.getStatus();
    assertThat("Unexpected status.", status, nullValue());
  }

  @Test
  void shouldReturnPastWhenNowAfterStartDateAndAfterEndDate() {
    var programmeMembership = new ProgrammeMembership();
    programmeMembership.setStartDate(LocalDate.now().minusYears(2));
    programmeMembership.setEndDate(LocalDate.now().minusYears(1));

    Status status = programmeMembership.getStatus();
    assertThat("Unexpected status.", status, is(Status.PAST));
  }

  @Test
  void shouldReturnCurrentWhenNowAfterStartDateAndBeforeEndDate() {
    var programmeMembership = new ProgrammeMembership();
    programmeMembership.setStartDate(LocalDate.now().minusYears(1));
    programmeMembership.setEndDate(LocalDate.now().plusYears(1));

    Status status = programmeMembership.getStatus();
    assertThat("Unexpected status.", status, is(Status.CURRENT));
  }

  @Test
  void shouldReturnFutureWhenNowBeforeStartDateAndBeforeEndDate() {
    var programmeMembership = new ProgrammeMembership();
    programmeMembership.setStartDate(LocalDate.now().plusYears(1));
    programmeMembership.setEndDate(LocalDate.now().plusYears(2));

    Status status = programmeMembership.getStatus();
    assertThat("Unexpected status.", status, is(Status.FUTURE));
  }
}
