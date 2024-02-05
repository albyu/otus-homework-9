package ru.boldyrev.otus.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.boldyrev.otus.model.entity.Account;
import ru.boldyrev.otus.model.transfer.*;
import ru.boldyrev.otus.service.AccountService;

import java.util.Optional;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class AccountListener {


    private final RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper;

    private final AccountService accountService;


    @Value("${application.rabbitmq.notification.exchangeName}")
    private String notificationExchange;


    public void sendNotification(TransportablePayRequest tPayRequest)  {
        String ttAsString = null;
        try {
            ttAsString = objectMapper.writeValueAsString(tPayRequest);
            rabbitTemplate.convertAndSend(notificationExchange, "account", ttAsString);
        } catch (JsonProcessingException e) {
            log.error("Cannot send notify for payRequest.order = {}, username = {}", tPayRequest.getOrderId(), tPayRequest.getUsername(), e);
            log.error("Error: ", e);
        }

    }


    /*Получение запроса на подтверждение заказа*/
    @RabbitListener(queues = "${application.rabbitmq.account.queueName}")
    public void receiveAccountOpeningRequest(String requestAsString) {
        TransportableUser transportableRequest = new TransportableUser();
        try {
            transportableRequest = objectMapper.readValue(requestAsString, TransportableUser.class);
            log.info("request for opening for user {} received", transportableRequest.getUsername());

            //Проверим, есть ли счет для такого юзера
            Optional<Account> existingAccount = accountService.getAccountByUsernameNoException(transportableRequest.getUsername());

            //счета нет, надо открыть
            if (existingAccount.isEmpty()) {
                Account account = new Account().setBalance(0L).setUserName(transportableRequest.getUsername());
                accountService.saveAccount(account);
            }

        } catch (Exception e) {
            log.error("Cannot process {}", requestAsString);
            log.error("Error: ", e);
        }
    }
}
