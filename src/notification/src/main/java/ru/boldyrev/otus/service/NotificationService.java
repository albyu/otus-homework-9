package ru.boldyrev.otus.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.boldyrev.otus.exception.NotFoundException;
import ru.boldyrev.otus.model.entity.Notification;
import ru.boldyrev.otus.model.entity.User;
import ru.boldyrev.otus.model.enums.PayResult;
import ru.boldyrev.otus.model.transfer.TransportableNotification;
import ru.boldyrev.otus.model.transfer.TransportablePayRequest;
import ru.boldyrev.otus.model.transfer.TransportableUser;
import ru.boldyrev.otus.repo.NotificationRepo;
import ru.boldyrev.otus.repo.UserRepo;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {
    private final UserRepo userRepo;

    private final NotificationRepo notificationRepo;



    @Autowired
    public NotificationService(UserRepo userRepo, NotificationRepo notificationRepo) {
        this.userRepo = userRepo;
        this.notificationRepo = notificationRepo;


    }

    private String formatMessage(Timestamp ts) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        Date date = new Date(ts.getTime());
        return dateFormat.format(date);

    }

    public void createNotification(TransportablePayRequest tPayReq) {
        String notificationText;
        if (tPayReq.getPayResult() == PayResult.SUCCESS) { /* Письмо счастья */
            notificationText =
                    String.format("Удачно оплачен заказ %s на сумму %f, дата/время: %s. Ссылка на операцию биллинга: %d",
                            tPayReq.getOrderId(), tPayReq.getAmount(), formatMessage(tPayReq.getTimestamp()), tPayReq.getTransactionId());
        } else { /* Письмо горя */
            notificationText =
                    String.format("Ошибка оплаты заказа %s на сумму %f. Причина: %s, дата/время: %s",
                            tPayReq.getOrderId(), tPayReq.getAmount(),
                            tPayReq.getPayResult().getName(),
                            formatMessage(tPayReq.getTimestamp()));
        }
        /* Ищем пользователя, чтобы достать e-mail*/
        Optional<User> optionalUser = userRepo.findByUsername(tPayReq.getUsername());
        if (optionalUser.isPresent()) { /* Пользователь существует */
            Notification note = new Notification()
                    .setUsername(tPayReq.getUsername())
                    .setTimestamp(Timestamp.valueOf(LocalDateTime.now()))
                    .setAmount(tPayReq.getAmount())
                    .setEmail(optionalUser.get().getEmail())
                    .setOrderId(tPayReq.getOrderId())
                    .setPayResult(tPayReq.getPayResult())
                    .setText(notificationText)
                    .setIsDeadNotification(Boolean.FALSE);
            notificationRepo.saveAndFlush(note);
            /* Тут отправка e-mail*/

        } else { /*Пользователь не существует*/
            Notification deadNote = new Notification()
                    .setUsername(tPayReq.getUsername())
                    .setTimestamp(Timestamp.valueOf(LocalDateTime.now()))
                    .setAmount(tPayReq.getAmount())
                    .setOrderId(tPayReq.getOrderId())
                    .setPayResult(tPayReq.getPayResult())
                    .setText(notificationText)
                    .setIsDeadNotification(Boolean.TRUE);
        }
    }

    public void saveUser(TransportableUser tUser) {

        User user = userRepo.findByUsername(tUser.getUsername())  /* Существующий пользователь */
                .orElse(new User().setUsername(tUser.getUsername())); /* Новый пользователь */
        /* e-mail в любом случае обновляем */
        user.setEmail(tUser.getEmail());
        userRepo.saveAndFlush(user);
    }

    public List<TransportableNotification> getNotifications(String username) throws NotFoundException {
        if (userRepo.existsById(username)) {
            return notificationRepo.findByUsername(username).stream().map(TransportableNotification::new).toList();
        } else throw new NotFoundException("User not found");
    }
}
