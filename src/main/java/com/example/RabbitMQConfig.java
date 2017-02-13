package com.example;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by priyankak on 2/8/17.
 */

@EnableRabbit
public class RabbitMQConfig {

    @Value("${rabbitMQHost}")
    private String host;

    @Value("${rabbitMQUsername}")
    private String username;

    @Value("${rabbitMQPassword}")
    private String password;


    @Bean
    public ConnectionFactory rabbitConnectionFactory() throws URISyntaxException {
        CachingConnectionFactory cf = new CachingConnectionFactory();
        cf.setUri(new URI(String.format("amqp://%s:%s@%s", username, password, host)));
        return cf;
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
        simpleRetryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(simpleRetryPolicy);
        //retryTemplate.setBackOffPolicy(backOffPolicy());
        return retryTemplate;
    }

    @Bean
    public RabbitTemplate rabbitTemplate() throws URISyntaxException {
        RabbitTemplate template = new RabbitTemplate(rabbitConnectionFactory());
        template.setMandatory(true);
        template.setChannelTransacted(true);
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        template.setMessageConverter(converter);
        template.setRetryTemplate(retryTemplate());
        return template;
    }
}
