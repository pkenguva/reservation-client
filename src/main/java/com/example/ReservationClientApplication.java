package com.example;

import com.google.common.collect.Lists;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import lombok.extern.log4j.Log4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.hateoas.Resources;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@IntegrationComponentScan
@EnableBinding (ReservationChannels.class)
@EnableFeignClients
@EnableZuulProxy
@EnableCircuitBreaker
@EnableDiscoveryClient
@SpringBootApplication
public class ReservationClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservationClientApplication.class, args);
	}
}

interface ReservationChannels {

	@Output
	MessageChannel output();
}

@FeignClient("reservation-service")
interface ReservationReader {

	@RequestMapping(method = RequestMethod.GET, path = "/reservations")
	Resources<Reservation> read();

}

@MessagingGateway
interface  ReservationWriter {

	@Gateway(requestChannel = "output")
	void write(String rn);
}

class Reservation {

	private String reservationName;

	public String getReservationName() {
		return reservationName;
	}

	public void setReservationName(String reservationName) {
		this.reservationName = reservationName;
	}
}

@RestController
@Log4j
@RequestMapping("/reservations")
class ReservationApiGateway {

	private final ReservationReader reader;
	private final ReservationWriter writer;
	private final RabbitTemplate rabbitTemplate;

	@Autowired
	public ReservationApiGateway(ReservationReader reader, ReservationWriter writer, RabbitTemplate rabbitTemplate) {
		this.reader = reader;
		this.writer = writer;
		this.rabbitTemplate = rabbitTemplate;
	}

	@RequestMapping(method = RequestMethod.POST)
	public void write(@RequestBody Reservation reservation) {
		log.info("Posting the name");
		writer.write(reservation.getReservationName());
//		MessagePostProcessor postProcessor = messageProcessor -> {
//			messageProcessor.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
//			return messageProcessor;
//		};
		//rabbitTemplate.convertAndSend("reservations", "routing.key", reservation.getReservationName(), postProcessor);

	}

	public List<String> fallback() {
		return Lists.newArrayList();
	}

	@HystrixCommand(fallbackMethod = "fallback")
	@RequestMapping(method = RequestMethod.GET, path = "/names")
	public List<String> names() {
		log.info("Getting the names..");
		return this.reader.read()
				.getContent()
				.stream()
				.map(Reservation::getReservationName)
				.collect(Collectors.toList());
	}

}