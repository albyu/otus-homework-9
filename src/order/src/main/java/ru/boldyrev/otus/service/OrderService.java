package ru.boldyrev.otus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.boldyrev.otus.exception.ConflictException;
import ru.boldyrev.otus.exception.NotAuthorizedException;
import ru.boldyrev.otus.exception.NotFoundException;
import ru.boldyrev.otus.exception.PaymentErrorException;
import ru.boldyrev.otus.model.entity.Order;
import ru.boldyrev.otus.model.entity.OrderItem;
import ru.boldyrev.otus.model.entity.Product;
import ru.boldyrev.otus.model.enums.OrderStatus;
import ru.boldyrev.otus.model.enums.PayResult;
import ru.boldyrev.otus.model.transfer.TransportableOrder;
import ru.boldyrev.otus.model.transfer.TransportableOrderItem;
import ru.boldyrev.otus.model.transfer.TransportablePayRequest;
import ru.boldyrev.otus.repo.OrderRepo;
import ru.boldyrev.otus.repo.ProductRepo;

import javax.transaction.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderService {

    private final OrderRepo orderRepo;
    private final ProductRepo productRepo;
    private final PayService payService;


    public Order get(String orderId) throws NotFoundException, ConflictException {
        if (orderId == null) {
            throw new ConflictException("Null orderId");
        }
        return orderRepo.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
    }

    /* Размещение нового заказа */
    @Transactional
    public TransportableOrder place(TransportableOrder tOrder, String username) throws ConflictException, NotAuthorizedException, NotFoundException {

        /* Проверим наличие orderId */
        if (tOrder.getId() == null) {
            throw new ConflictException("Null orderId");
        }

        /* Поищем существующий заказ */
        Optional<Order> existingOrder = orderRepo.findById(tOrder.getId());

        /* Добавляем новый заказ */
        if (existingOrder.isEmpty()) {
            //Размещаем новый заказ со статусом NEW и версией 0
            Order order = createOrder(tOrder, username);
            order = orderRepo.saveAndFlush(order);

            return new TransportableOrder(order);
        }

        /* Заказ уже существует, вернем его состояние as is */
        else {
            /* Этот ли пользователь создавал заказ?*/
            if (!existingOrder.get().getUsername().equals(username))
                throw new NotAuthorizedException("User not authorized");

            /* Вернем состояние заказа */
            return new TransportableOrder(existingOrder.get());
        }
    }


    private Order createOrder(TransportableOrder tOrder, String username) throws NotFoundException {
        Order order = new Order().setId(tOrder.getId()).setStatus(OrderStatus.NEW).setVersion(0L).setUsername(username);
        order.setOrderItems(new HashSet<>());
        for (TransportableOrderItem tItem : tOrder.getOrderItems()) {
            OrderItem item = new OrderItem().setQuantity(tItem.getQuantity());
            /*Поиск продукта*/
            if (tItem.getProduct() == null) throw new NotFoundException("Not found: empty product");
            if (tItem.getProduct().getId() == null) throw new NotFoundException("Not found: empty product");
            Product product = productRepo.findById(tItem.getProduct().getId()).orElseThrow(() -> new NotFoundException("Product not found"));
            item.setProduct(product);
            order.getOrderItems().add(item);
        }
        return order;
    }

    /* Изменение существующего заказа */
    @Transactional
    public TransportableOrder adjust(Order order, TransportableOrder tOrder) throws ConflictException, NotFoundException {

        /* Проверяем статус заказа */
        if (order.getStatus() == OrderStatus.IN_PROGRESS) {
            throw new ConflictException("Order already in progress");
        } else if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new ConflictException("Order already completed");
        } else if (order.getStatus() == OrderStatus.CANCELED) {
            throw new ConflictException("Order already canceled");
        } else { /* Order in status NEW, PAYMENT_REJECTED */

            /* Корректируем существующий заказ */
            order = updateOrder(order, tOrder);
            order = orderRepo.saveAndFlush(order);
            return new TransportableOrder(order);
        }
    }

    /*Корректируем существующий заказ*/
    private Order updateOrder(Order order, TransportableOrder tOrder) throws NotFoundException {

        order.setVersion(tOrder.getVersion()); /* Для контроля оптимистической блокировки*/

        /* Перебираем items tOrder, ищем, что хотим поменять */
        for (TransportableOrderItem tItem : tOrder.getOrderItems()) {
            Long tItemId = tItem.getId();
            /* Ищем в Order элемент с таким id */

            OrderItem existingItem = null;

            if (tItemId != null) {
                existingItem = order.getOrderItems().stream()
                        .filter(item -> Objects.equals(item.getId(), tItemId))
                        .findFirst().orElse(null);
            }

            if (existingItem != null) {

                /*Есть такой элемент - апдейтим его*/
                existingItem.setQuantity(tItem.getQuantity());

                if (tItem.getProduct() == null) throw new NotFoundException("Not found: empty product");
                if (tItem.getProduct().getId() == null) throw new NotFoundException("Not found: empty product");
                Product product = productRepo.findById(tItem.getProduct().getId()).orElseThrow(() -> new NotFoundException("Product not found"));

                existingItem.setProduct(product);
            } else {
                /*Нет такого элемента - добавляем новый*/
                OrderItem item = new OrderItem().setQuantity(tItem.getQuantity());

                /*Ищем и устанавливаем продукт*/
                if (tItem.getProduct() == null) throw new NotFoundException("Not found: empty product");
                if (tItem.getProduct().getId() == null) throw new NotFoundException("Not found: empty product");
                Product product = productRepo.findById(tItem.getProduct().getId()).orElseThrow(() -> new NotFoundException("Product not found"));
                item.setProduct(product);
                order.getOrderItems().add(item);
            }
        }
        /*Ищем элементы item, которых нет в tOrder */
        Iterator<OrderItem> iterator = order.getOrderItems().iterator();
        while (iterator.hasNext()) {
            OrderItem item = iterator.next();
            Long itemId = item.getId();
            if (itemId != null) { /*Нет смысла проверять только что добавленные элементы*/
                if (tOrder.getOrderItems().stream().noneMatch(ti -> Objects.equals(ti.getId(), itemId))) {
                    iterator.remove(); // Удаляем элемент из коллекции order.getOrderItems()
                }
            }
        }

        return order;
    }

    /* Передача заказа в обработку */
    @Transactional
    public TransportableOrder startProcessing(Order order, TransportableOrder tOrder) throws ConflictException, PaymentErrorException {

        /* Проверяем статус заказа */
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new ConflictException("Already completed");
        } else if (order.getStatus() == OrderStatus.IN_PROGRESS) {
            throw new ConflictException("Already in progress");
        } else if (order.getStatus() == OrderStatus.NEW || order.getStatus() == OrderStatus.PAYMENT_REJECTED) {

            /* Меняем статус и сохраняем */
            order.setStatus(OrderStatus.IN_PROGRESS);
            order.setVersion(tOrder.getVersion());/* Для контроля оптимистической блокировки*/
            order = orderRepo.saveAndFlush(order);

            /* Оплата */
            TransportablePayRequest tRequest = new TransportablePayRequest()
                    .setOrderId(order.getId()).setAmount(calculateAmount(order)).setUsername(order.getUsername());


            tRequest = payService.pay(tRequest);


            /* Меняем статус в соответствии с результатами оплаты и сохраняем */
            if (tRequest.getPayResult() == PayResult.SUCCESS) {
                order.setStatus(OrderStatus.COMPLETED);

            } else {
                order.setStatus(OrderStatus.PAYMENT_REJECTED);
            }

            order = orderRepo.saveAndFlush(order);


            return new TransportableOrder(order);

        } else /*OrderStatus.CANCELED*/ {
            /* вернем состояние as is */
            return new TransportableOrder(order);
        }
    }

    private double calculateAmount(Order order) {
        double amount = 0;
        for (OrderItem orderItem : order.getOrderItems()) {
            amount += orderItem.getQuantity() * orderItem.getProduct().getPrice();
        }
        return amount;
    }

    /* Отмена заказа*/
    @Transactional
    public TransportableOrder cancel(Order canceledOrder, TransportableOrder tOrder) throws ConflictException, NotFoundException {

        /* Проверяем статус заказа */
        if (canceledOrder.getStatus() == OrderStatus.COMPLETED) {
            throw new ConflictException("Already completed");

        } else if (canceledOrder.getStatus() == OrderStatus.IN_PROGRESS) {
            throw new ConflictException("Already in progress");

        } else if (canceledOrder.getStatus() == OrderStatus.NEW || canceledOrder.getStatus() == OrderStatus.PAYMENT_REJECTED) {

            /* Меняем статус и сохраняем */
            canceledOrder.setStatus(OrderStatus.CANCELED);
            canceledOrder.setVersion(tOrder.getVersion()); /* Для контроля оптимистической блокировки*/
            canceledOrder = orderRepo.saveAndFlush(canceledOrder);
            return new TransportableOrder(canceledOrder);

        } else /*OrderStatus.CANCELED*/ {
            /* вернем состояние as is */
            return new TransportableOrder(canceledOrder);
        }
    }


}
