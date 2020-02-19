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

package uk.nhs.hee.trainee.details.repository;

import java.util.Optional;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.nhs.hee.trainee.details.TestConfig;
import uk.nhs.hee.trainee.details.model.ContactDetails;

@Ignore("Current requires a local DB instance, ignore until in-memory test DB is set up")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfig.class)
public class ContactDetailsRepositoryTest {

  @Autowired
  private ContactDetailsRepository repository;

  private ContactDetails contactDetails;

  /**
   * Set up before each test.
   */
  @Before
  @Transactional
  public void setUp() {
    contactDetails = new ContactDetails();
    contactDetails.setId("1");
    contactDetails.setTraineeTisId("1111");
    contactDetails.setSurname("Test");
    repository.save(contactDetails);
  }

  @After
  @Transactional
  public void tearDown() {
    repository.delete(contactDetails);
  }

  @Test
  @Transactional
  public void findContactDetailsById() {
    Optional<ContactDetails> result = repository.findById("1");
    Assert.assertTrue(result.isPresent());
    ContactDetails contactDetails = result.get();
    Assert.assertEquals(contactDetails.getId(), "1s");
    Assert.assertEquals(contactDetails.getSurname(), "Test");
  }

  @Test
  @Transactional
  public void shouldReturnContactDetailsByTraineeTisId() {
    ContactDetails contactDetails = repository.findByTraineeTisId("1111");
    Assert.assertEquals(contactDetails.getId(), "1");
    Assert.assertEquals(contactDetails.getSurname(), "Test");
  }
}
