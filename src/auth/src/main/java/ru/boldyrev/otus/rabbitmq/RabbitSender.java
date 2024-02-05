package ru.boldyrev.otus.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.boldyrev.otus.model.transfer.TransportableUser;


@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class RabbitSender {


    private final RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper;


    @Value("${application.rabbitmq.notification.exchangeName}")
    private String notificationExchange;

    @Value("${application.rabbitmq.account.exchangeName}")
    private String accountExchange;

    private final String notificationAuthRouteKey = "auth";

    private final String accountAuthRouteKey = "";





    public void sendRequest(TransportableUser tUser, String exchange, String routekey)  {
        String tUserAsString = null;
        try {
            tUserAsString = objectMapper.writeValueAsString(tUser);
            rabbitTemplate.convertAndSend(exchange, routekey, tUserAsString);
        } catch (JsonProcessingException e) {
            log.error("Cannot send request for notification for username = {}",  tUser.getUsername(), e);
            log.error("Error: ", e);
        }
    }


    public void sendAccountRequest(TransportableUser tUser) {
        sendRequest(tUser, accountExchange, accountAuthRouteKey);
    }

    public void sendNotificationRequest(TransportableUser tUser) {
        sendRequest(tUser, notificationExchange, notificationAuthRouteKey);

    }
}
