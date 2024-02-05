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
import ru.boldyrev.otus.model.transfer.TransportablePayRequest;
import ru.boldyrev.otus.model.transfer.TransportableUser;
import ru.boldyrev.otus.service.NotificationService;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class NotificationListener {

    private final RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper;

    private final NotificationService notificationService;


    /* Получение запроса на уведомление о попытке совершения платежа */
    @RabbitListener(queues = "${application.rabbitmq.notification.accQueueName}")
    public void receiveMessageFromAccount(String tPayReqAsString) {
        TransportablePayRequest tPayReq = new TransportablePayRequest();

        try {
            /**/
            tPayReq = objectMapper.readValue(tPayReqAsString, TransportablePayRequest.class);
            log.info("Notification request for order {} received", tPayReq.getOrderId());

            notificationService.createNotification(tPayReq);

        } catch (Exception e) {
            log.error("Cannot process " + tPayReqAsString, e);
        }
    }

    /* Получение запроса о создании новой пользовательской записи */
    @RabbitListener(queues = "${application.rabbitmq.notification.authQueueName}")
    public void receiveMessageFromAuth(String tUserAsString) {
        TransportableUser tUser = new TransportableUser();

        try {
            tUser = objectMapper.readValue(tUserAsString, TransportableUser.class);
            log.info("Request for registration of user {} received", tUser.getUsername());

            notificationService.saveUser(tUser);

        } catch (Exception e) {
            log.error("Cannot process " + tUserAsString, e);
        }
    }


}
