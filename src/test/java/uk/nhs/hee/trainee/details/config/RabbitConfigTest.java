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
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test class for the Rabbit configuration.
 * TODO: check that these are actually meaningful tests
 */
class RabbitConfigTest {

  private static final String RABBIT_QUEUE_NAME = "coj.queue";
  private static final String RABBIT_EXCHANGE = "exchange";
  private static final String RABBIT_ROUTING_KEY = "routing.key";

  @Autowired
  private ConnectionFactory connectionFactory;

  private RabbitConfig rabbitConfig;

  @BeforeEach
  void setUp() {
    rabbitConfig = new RabbitConfig(RABBIT_QUEUE_NAME, RABBIT_EXCHANGE, RABBIT_ROUTING_KEY);
  }

  @Test
  void shouldBuildQueue() {
    Queue cojQueue = rabbitConfig.cojSignedQueue();
    assertThat("Unexpected CoJ queue name", cojQueue.getName(), is(RABBIT_QUEUE_NAME));
    assertThat("Unexpected CoJ queue durability", cojQueue.isDurable(), is(true));
  }

  @Test
  void shouldBuildExchange() {
    DirectExchange exchange = rabbitConfig.exchange();
    assertThat("Unexpected exchange name", exchange.getName(), is(RABBIT_EXCHANGE));
  }

  @Test
  void shouldBuildBinding() {
    Binding binding
        = rabbitConfig.cojBinding(rabbitConfig.cojSignedQueue(), rabbitConfig.exchange());
    assertThat("Unexpected binding exchange",
        binding.getExchange(), is(RABBIT_EXCHANGE));
    assertThat("Unexpected binding destination",
        binding.getDestination(), is(RABBIT_QUEUE_NAME));
    assertThat("Unexpected binding routing key",
        binding.getRoutingKey(), is(RABBIT_ROUTING_KEY));
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
