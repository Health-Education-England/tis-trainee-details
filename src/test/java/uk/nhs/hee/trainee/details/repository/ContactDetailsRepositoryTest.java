package uk.nhs.trainee.details.repository;

import com.transformuk.hee.tis.tcs.service.TestConfig;
import uk.nhs.hee.trainee.details.model.ContactDetails;
import uk.nhs.hee.trainee.details.repository.ContactDetailsRepository;
import java.util.Optional;
import javax.persistence.EntityManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfig.class)
public class ContactDetailsRepositoryTest {

  @Autowired
  private ContactDetailsRepository contactDetailsRepository;

  @Autowired
  private EntityManager entityManager;

  private ContactDetails contactDetails;

  @Before
  @Transactional
  public void setup() {
    contactDetails = new ContactDetails();
    contactDetails.setId("1");
    contactDetails.setSurname("Test");
    entityManager.persist(contactDetails);
  }

  @After
  @Transactional
  public void tearDown() {
    entityManager.remove(contactDetails);
  }

  @Test
  @Transactional
  public void findContactDetailsById() {
    Optional <ContactDetails> result = contactDetailsRepository.findById("1");
    Assert.assertTrue(result.isPresent());
    ContactDetails contactDetails = result.get();
    Assert.assertEquals(contactDetails.getId(), "1");
    Assert.assertEquals(contactDetails.getSurname(), "Test");
  }
}
