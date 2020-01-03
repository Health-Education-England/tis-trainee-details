package uk.nhs.hee.trainee.details.service.impl;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import uk.nhs.hee.trainee.details.service.ContactDetailsService;
import uk.nhs.hee.trainee.details.repository.ContactDetailsRepository;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RunWith(MockitoJUnitRunner.class)
public class ContactDetailsServiceImplTest {

  @Mock
  private ContactDetailsRepository contactDetailsRepository;
  @InjectMocks
  private ContactDetailsService service;
}
