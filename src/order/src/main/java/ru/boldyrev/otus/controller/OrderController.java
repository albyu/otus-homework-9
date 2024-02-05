package ru.boldyrev.otus.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.boldyrev.otus.exception.*;
import ru.boldyrev.otus.model.entity.Order;
import ru.boldyrev.otus.model.transfer.TransportableOrder;
import ru.boldyrev.otus.service.CheckAuthService;
import ru.boldyrev.otus.service.OrderService;

import javax.persistence.EntityExistsException;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Api(tags = "Обработка заказа")
public class OrderController {

    private final OrderService orderService;
    private final CheckAuthService checkAuthService;


    /*Создать заказ*/
    @ApiOperation(value = "Создание заказа", notes = "Идемпотентный метод, если заказ уже существует, то возвращается в ответе as is")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = Order.class),
            @ApiResponse(code = 401, message = "User not authorized"),
            @ApiResponse(code = 404, message = "Product not found"),
            @ApiResponse(code = 409, message = "Inappropriate state of object for performing operation (see details)"),
            @ApiResponse(code = 500, message = "Authentication error")
    })
    @PostMapping(value = "/place", consumes = "application/json", produces = "application/json")
    public ResponseEntity<TransportableOrder> orderPlace(@RequestBody TransportableOrder tOrder, @CookieValue(value = "session_id", required = false) String sessionId) throws ConflictException, NotAuthorizedException, NotFoundException, AuthErrorException {
        String username = checkAuthService.checkAuth(sessionId);

        TransportableOrder placedTOrder = orderService.place(tOrder, username);
        return ResponseEntity.status(HttpStatus.OK).body(placedTOrder);
    }

    /*Изменить заказ*/
    @ApiOperation(value = "Изменение заказа", notes = "Применимо для заказов в статусе NEW, использует оптимистические блокировки через поле version")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = Order.class),
            @ApiResponse(code = 401, message = "User not authorized"),
            @ApiResponse(code = 404, message = "Order not found"),
            @ApiResponse(code = 409, message = "Inappropriate state of object for performing operation (see details)"),
            @ApiResponse(code = 500, message = "Authentication error")
    })
    @PutMapping(value = "/adjust", consumes = "application/json", produces = "application/json")
    public ResponseEntity<TransportableOrder> orderAdjust(@RequestBody TransportableOrder tOrder, @CookieValue(value = "session_id", required = false) String sessionId) throws ConflictException, NotFoundException, NotAuthorizedException, AuthErrorException {

        Order existingOrder = orderService.get(tOrder.getId());
        checkAuthService.checkAuth(sessionId, existingOrder.getUsername());

        TransportableOrder placedOrder = orderService.adjust(existingOrder, tOrder);
        return ResponseEntity.status(HttpStatus.OK).body(placedOrder);
    }

    /*Начать обработку заказа*/
    @ApiOperation(value = "Передать заказ в обработку", notes = "Применимо для заказов в статусе NEW, для заказов в статусе IN_PROGRESS, COMPLETED просто возвращает состояние заказа as is. Для заказа в статусе CANCELED возвращает ошибку. Использует оптимистические блокировки через поле version")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = Order.class),
            @ApiResponse(code = 401, message = "User not authorized"),
            @ApiResponse(code = 404, message = "Order not found"),
            @ApiResponse(code = 409, message = "Inappropriate state of object for performing operation (see details)"),
            @ApiResponse(code = 500, message = "Payment cannot be proceed"),
            @ApiResponse(code = 500, message = "Authentication error")
    })
    @PutMapping(value = "/process", consumes = "application/json", produces = "application/json")
    public ResponseEntity<TransportableOrder> orderProcess(@RequestBody TransportableOrder tOrder, @CookieValue(value = "session_id", required = false) String sessionId) throws ConflictException, NotFoundException, NotAuthorizedException, PaymentErrorException, AuthErrorException {

        Order existingOrder = orderService.get(tOrder.getId());
        checkAuthService.checkAuth(sessionId, existingOrder.getUsername());

        TransportableOrder processedOrder = orderService.startProcessing(existingOrder, tOrder);
        return ResponseEntity.status(HttpStatus.OK).body(processedOrder);
    }

    /*Отменить заказ*/
    @ApiOperation(value = "Отменить заказ", notes = "Применимо для заказов в статусе NEW, IN_PROGRESS. Для заказа в статусе CANCELED просто возвращает состояние. Для заказа в статусе CANCELED возвращает ошибку. Использует оптимистические блокировки через поле version")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = Order.class),
            @ApiResponse(code = 401, message = "User not authorized"),
            @ApiResponse(code = 404, message = "Order not found"),
            @ApiResponse(code = 409, message = "Inappropriate state of object for performing operation (see details)"),
            @ApiResponse(code = 500, message = "Authentication error")
    })
    @PutMapping(value = "/cancel", consumes = "application/json", produces = "application/json")
    public ResponseEntity<TransportableOrder> orderCancel(@RequestBody TransportableOrder tOrder, @CookieValue(value = "session_id", required = false) String sessionId) throws ConflictException, NotFoundException, NotAuthorizedException, AuthErrorException {

        Order existingOrder = orderService.get(tOrder.getId());
        checkAuthService.checkAuth(sessionId, existingOrder.getUsername());

        TransportableOrder canceledOrder = orderService.cancel(existingOrder, tOrder);
        return ResponseEntity.status(HttpStatus.OK).body(canceledOrder);
    }


    /*Получить текущий статус заказа*/
    @ApiOperation(value = "Получить текущее состояние заказа")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = Order.class),
            @ApiResponse(code = 401, message = "User not authorized"),
            @ApiResponse(code = 404, message = "Order not found"),
            @ApiResponse(code = 500, message = "Authentication error")
    })
    @GetMapping(value = "/get", produces = "application/json")
    public ResponseEntity<Order> orderGet(@RequestParam(name = "orderId") String orderId, @CookieValue(value = "session_id", required = false) String sessionId) throws NotFoundException, NotAuthorizedException, ConflictException, AuthErrorException {
        Order order = orderService.get(orderId);
        checkAuthService.checkAuth(sessionId, order.getUsername());

        return ResponseEntity.status(HttpStatus.OK).body(order);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>> accountExceptionProcessing(ConflictException conflictException) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("errorReason", conflictException.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> accountExceptionProcessing(NotFoundException conflictException) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("errorReason", conflictException.getMessage()));
    }

    @ExceptionHandler(EntityExistsException.class)
    public ResponseEntity<Map<String, String>> accountExceptionProcessing(EntityExistsException orderException) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("errorReason", "Cannot change object, try to refresh with /order/get"));
    }

    /*Перехватываем тут, потому что при перехвате внутри @Transactional прокси-класс кидает свое исключение и все равно перехватывать тут еще раз*/
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> accountExceptionProcessing(OptimisticLockingFailureException orderException) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("errorReason", "Object was changed by another transaction, try to refresh with /order/get"));
    }

    @ExceptionHandler(NotAuthorizedException.class)
    public ResponseEntity<Map<String, String>> accountExceptionProcessing(NotAuthorizedException notAuthorizedException) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("errorReason", notAuthorizedException.getMessage()));
    }

    @ExceptionHandler(PaymentErrorException.class)
    public ResponseEntity<Map<String, String>> accountExceptionProcessing(PaymentErrorException paymentErrorException) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("errorReason", paymentErrorException.getMessage()));
    }

    @ExceptionHandler(AuthErrorException.class)
    public ResponseEntity<Map<String, String>> accountExceptionProcessing(AuthErrorException authErrorException) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("errorReason", authErrorException.getMessage()));
    }


}
