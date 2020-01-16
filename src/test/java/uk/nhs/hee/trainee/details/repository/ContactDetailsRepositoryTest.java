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
