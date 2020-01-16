package uk.nhs.hee.trainee.details.resource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import uk.nhs.hee.trainee.details.TisTraineeDetailsApplication;
import uk.nhs.hee.trainee.details.api.ContactDetailsResource;
import uk.nhs.hee.trainee.details.model.ContactDetails;
import uk.nhs.hee.trainee.details.repository.ContactDetailsRepository;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TisTraineeDetailsApplication.class)
public class ContactDetailsResourceIntTest {

  private static final String DEFAULT_SURNAME = "defaultSurname";
  private static final String DEFAULT_FORENAMES = "defaultForenames";
  private static final String DEFAULT_KNOWN_AS = "defaultKnownAs";
  private static final String DEFAULT_MAIDEN_NAME = "defaultMaidenName";
  private static final String DEFAULT_TITLE = "defaultTitle";
  private static final String DEFAULT_CONTACT_PHONE_NR_1 = "0123456789";
  private static final String DEFAULT_CONTACT_PHONE_NR_2 = "9876543210";
  private static final String DEFAULT_EMAIL = "defaultEmail@test.com";
  private static final String DEFAULT_ADDRESS1 = "defaultAddress1";
  private static final String DEFAULT_ADDRESS2 = "defaultAddress2";
  private static final String DEFAULT_ADDRESS3 = "defaultAddress3";
  private static final String DEFAULT_ADDRESS4 = "defaultAddress4";
  private static final String DEFAULT_POST_CODE = "defaultPostCode";
  @Autowired
  ContactDetailsResource contactDetailsResource;
  @Autowired
  private MappingJackson2HttpMessageConverter jacksonMessageConverter;
  @Autowired
  private PageableHandlerMethodArgumentResolver pageableArgumentResolver;
  @Autowired
  private ContactDetailsRepository repository;

  private MockMvc mvc;

  private ContactDetails contactDetails;

  /**
   * Create a {@link ContactDetails} entity with default values for all fields.
   * @return The create {@code ContactDetails}.
   */
  public static ContactDetails createEntity() {
    return new ContactDetails()
        .id("101")
        .traineeTisId("2222")
        .surname(DEFAULT_SURNAME)
        .forenames(DEFAULT_FORENAMES)
        .knownAs(DEFAULT_KNOWN_AS)
        .maidenName(DEFAULT_MAIDEN_NAME)
        .title(DEFAULT_TITLE)
        .telephoneNumber(DEFAULT_CONTACT_PHONE_NR_1)
        .mobileNumber(DEFAULT_CONTACT_PHONE_NR_2)
        .email(DEFAULT_EMAIL)
        .address1(DEFAULT_ADDRESS1)
        .address2(DEFAULT_ADDRESS2)
        .address3(DEFAULT_ADDRESS3)
        .address4(DEFAULT_ADDRESS4)
        .postCode(DEFAULT_POST_CODE);
  }

  /**
   * Set up mocks before each test.
   */
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.mvc = MockMvcBuilders.standaloneSetup(contactDetailsResource)
        .setCustomArgumentResolvers(pageableArgumentResolver)
        .setMessageConverters(jacksonMessageConverter).build();

    contactDetails = createEntity();
  }

  @Test
  @Transactional
  public void getContactDetailsById() throws Exception {
    // Given
    repository.save(contactDetails);

    // When and Then
    mvc.perform(get("/api/contactdetails/{id}", contactDetails.getId())
        .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(contactDetails.getId()))
        .andExpect(jsonPath("$.traineeTisId").value(contactDetails.getTraineeTisId()))
        .andExpect(jsonPath("$.surname").value(DEFAULT_SURNAME))
        .andExpect(jsonPath("$.forenames").value(DEFAULT_FORENAMES))
        .andExpect(jsonPath("$.knownAs").value(DEFAULT_KNOWN_AS))
        .andExpect(jsonPath("$.maidenName").value(DEFAULT_MAIDEN_NAME))
        .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
        .andExpect(jsonPath("$.telephoneNumber").value(DEFAULT_CONTACT_PHONE_NR_1))
        .andExpect(jsonPath("$.mobileNumber").value(DEFAULT_CONTACT_PHONE_NR_2))
        .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
        .andExpect(jsonPath("$.address1").value(DEFAULT_ADDRESS1))
        .andExpect(jsonPath("$.address2").value(DEFAULT_ADDRESS2))
        .andExpect(jsonPath("$.address3").value(DEFAULT_ADDRESS3))
        .andExpect(jsonPath("$.address4").value(DEFAULT_ADDRESS4))
        .andExpect(jsonPath("$.postCode").value(DEFAULT_POST_CODE));
  }
}
