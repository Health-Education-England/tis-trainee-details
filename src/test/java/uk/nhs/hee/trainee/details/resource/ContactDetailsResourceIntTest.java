package uk.nhs.hee.trainee.details.resource;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import uk.nhs.hee.trainee.details.api.ContactDetailsResource;
import uk.nhs.hee.trainee.details.TisTraineeDetailsApplication;
import uk.nhs.hee.trainee.details.model.ContactDetails;
import uk.nhs.hee.trainee.details.repository.ContactDetailsRepository;
import javax.persistence.EntityManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TisTraineeDetailsApplication.class)
public class ContactDetailsResourceIntTest {

  private static final String DEFAULT_SURNAME = "AAAAAAAAAA";
  private static final String DEFAULT_LEGAL_SURNAME = "AAAAAAAAAA";
  private static final String DEFAULT_FORENAMES = "AAAAAAAAAA";
  private static final String DEFAULT_LEGAL_FORENAMES = "AAAAAAAAAA";
  private static final String DEFAULT_KNOWN_AS = "AAAAAAAAAA";
  private static final String DEFAULT_MAIDEN_NAME = "AAAAAAAAAA";
  private static final String DEFAULT_INITIALS = "AAAAAAAAAA";
  private static final String DEFAULT_TITLE = "AAAAAAAAAA";
  private static final String DEFAULT_CONTACT_PHONE_NR_1 = "080808080";
  private static final String DEFAULT_CONTACT_PHONE_NR_2 = "080808080";
  private static final String DEFAULT_EMAIL = "AAAAAAAAAA@test.com";
  private static final String DEFAULT_WORK_EMAIL = "AAAAAAAAAA@test.com";
  private static final String DEFAULT_ADDRESS1 = "AAAAAAAAAA";
  private static final String DEFAULT_ADDRESS2 = "BBBBBBBBBB";
  private static final String DEFAULT_ADDRESS3 = "CCCCCCCCCC";
  private static final String DEFAULT_ADDRESS4 = "DDDDDDDDDD";
  private static final String DEFAULT_POST_CODE = "AAAAAAAAAA";

  @Autowired
  private MappingJackson2HttpMessageConverter jacksonMessageConverter;

  @Autowired
  private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

  @Autowired
  ContactDetailsResource contactDetailsResource;

  @Autowired
  private ContactDetailsRepository contactDetailsRepository;

  @Autowired
  private EntityManager em;

  private MockMvc mvc;

  private ContactDetails contactDetails;

  public static ContactDetails createEntity(EntityManager em) {
    ContactDetails contactDetails = new ContactDetails()
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
    return contactDetails;
  }

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.mvc = MockMvcBuilders.standaloneSetup(contactDetailsResource)
        .setCustomArgumentResolvers(pageableArgumentResolver)
        .setMessageConverters(jacksonMessageConverter).build();
  }

  @Before
  public void initTest() {
    contactDetails = createEntity(em);
  }

  @Test
  @Transactional
  public void getContactDetailsById() throws Exception {
    // Given
    contactDetailsRepository.save(contactDetails);

    // When and Then
    mvc.perform(get("/api/contactdetails/{id}", contactDetails.getId())
        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(contactDetails.getId()))
        .andExpect(jsonPath("$.traineeTisId").value(contactDetails.getTraineeTisId()))
        .andExpect(jsonPath("$.surname").value(DEFAULT_SURNAME.toString()))
        .andExpect(jsonPath("$.forenames").value(DEFAULT_FORENAMES.toString()))
        .andExpect(jsonPath("$.knownAs").value(DEFAULT_KNOWN_AS.toString()))
        .andExpect(jsonPath("$.maidenName").value(DEFAULT_MAIDEN_NAME.toString()))
        .andExpect(jsonPath("$.title").value(DEFAULT_TITLE.toString()))
        .andExpect(jsonPath("$.telephoneNumber").value(DEFAULT_CONTACT_PHONE_NR_1.toString()))
        .andExpect(jsonPath("$.mobileNumber").value(DEFAULT_CONTACT_PHONE_NR_2.toString()))
        .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL.toString()))
        .andExpect(jsonPath("$.address1").value(DEFAULT_ADDRESS1.toString()))
        .andExpect(jsonPath("$.address2").value(DEFAULT_ADDRESS2.toString()))
        .andExpect(jsonPath("$.address3").value(DEFAULT_ADDRESS3.toString()))
        .andExpect(jsonPath("$.address4").value(DEFAULT_ADDRESS4.toString()))
        .andExpect(jsonPath("$.postCode").value(DEFAULT_POST_CODE.toString()));
  }
}
