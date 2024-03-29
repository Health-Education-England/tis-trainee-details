/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
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

package uk.nhs.hee.trainee.details.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test class for the Rabbit configuration.
 */
class RabbitConfigTest {

  @Autowired
  private ConnectionFactory connectionFactory;

  private RabbitConfig rabbitConfig;

  @BeforeEach
  void setUp() {
    rabbitConfig = new RabbitConfig();
  }

  @Test
  void shouldBuildMessageConverter() {
    MessageConverter messageConverter = rabbitConfig.jsonMessageConverter();
    String converterName = messageConverter.getClass().getName();
    assertThat("Unexpected message converter type", converterName,
        is(Jackson2JsonMessageConverter.class.getName()));
  }

  @Test
  void shouldBuildRabbitTemplate() {
    RabbitTemplate rabbitTemplate = rabbitConfig.rabbitTemplate(connectionFactory);
    String converterName = rabbitTemplate.getMessageConverter().getClass().getName();
    assertThat("Unexpected rabbit template converter", converterName,
        is(Jackson2JsonMessageConverter.class.getName()));
    assertThat("Unexpected rabbit template channel transaction mode",
        rabbitTemplate.isChannelTransacted(), is(false));
  }
}
