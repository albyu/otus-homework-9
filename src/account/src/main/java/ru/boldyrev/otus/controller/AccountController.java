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
import ru.boldyrev.otus.model.entity.PayRequest;
import ru.boldyrev.otus.model.enums.PayResult;
import ru.boldyrev.otus.model.transfer.TransportableAccount;
import ru.boldyrev.otus.model.transfer.TransportableCreditRequest;
import ru.boldyrev.otus.model.transfer.TransportablePayRequest;
import ru.boldyrev.otus.model.transfer.TransportableTransaction;
import ru.boldyrev.otus.service.AccountService;
import ru.boldyrev.otus.service.CheckAuthService;

import javax.persistence.EntityExistsException;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Api(tags = "Операции со счетом")
public class AccountController {


    private final AccountService accountService;

    private final CheckAuthService checkAuthService;


    /*Получить текущий статус заказа*/
    @ApiOperation(value = "Получить текущий остаток на счете")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = TransportableAccount.class),
            @ApiResponse(code = 401, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Account not found")
    })
    @GetMapping(value = "/get", produces = "application/json")
    public ResponseEntity<TransportableAccount> accountGet(@RequestParam(name = "username") String username, @CookieValue(value = "session_id", required = false) String sessionId) throws NotFoundException, NotAuthorizedException, AuthErrorException {
        checkAuthService.checkAuth(sessionId, username);
        TransportableAccount account = accountService.getTransportableAccountByUsername(username);
        return ResponseEntity.status(HttpStatus.OK).body(account);
    }

    @ApiOperation(value = "Получить список операций по счету")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = TransportableTransaction.class),
            @ApiResponse(code = 401, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Account not found")
    })
    @GetMapping(value = "/trxn/get", produces = "application/json")
    public ResponseEntity<List<TransportableTransaction>> trxnsGet(@RequestParam(name = "username") String username, @CookieValue(value = "session_id", required = false) String sessionId) throws NotFoundException, NotAuthorizedException, AuthErrorException {
        checkAuthService.checkAuth(sessionId, username);
        List<TransportableTransaction> trxns = accountService.getTrxnsByUsername(username);
        return ResponseEntity.status(HttpStatus.OK).body(trxns);
    }

    @ApiOperation(value = "Инициировать пополнение счета")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = TransportableAccount.class),
            @ApiResponse(code = 401, message = "Not authorized"),
            @ApiResponse(code = 403, message = "External decline"),
            @ApiResponse(code = 404, message = "Account not found")
    })
    @PostMapping(value = "/credit", produces = "application/json")
    public ResponseEntity<TransportableAccount> creditPost(@RequestBody TransportableCreditRequest creditRequest, @CookieValue(value = "session_id", required = false) String sessionId) throws NotFoundException, ExternalDeclineException, NotAuthorizedException, AuthErrorException {
        log.info("Session_id = {}", sessionId);
        checkAuthService.checkAuth(sessionId, creditRequest.getUsername());
        TransportableAccount accountAfterCredit = accountService.creditAccountByUsername(creditRequest);
        return ResponseEntity.status(HttpStatus.OK).body(accountAfterCredit);
    }

    @ApiOperation(value = "Внутренний endpoint: провести списание со счета")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = TransportablePayRequest.class),
            @ApiResponse(code = 401, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Account not found"),
            @ApiResponse(code = 409, message = "Not Sufficient Funds")

    })
    @PostMapping(value = "/pay", produces = "application/json")
    public ResponseEntity<TransportablePayRequest> payPost(@RequestBody TransportablePayRequest payRequest) {
        TransportablePayRequest payReqAfterCredit = accountService.debitAccountByUsername(payRequest);

        if (payReqAfterCredit.getPayResult() == PayResult.SUCCESS)
            return ResponseEntity.status(HttpStatus.OK).body(payReqAfterCredit);

        else /*EXTERNAL DECLINE, NOT_SUFFICIENT_FUNDS, NO_ACCOUNT*/
            return ResponseEntity.status(HttpStatus.CONFLICT).body(payReqAfterCredit);
    }

    @ApiOperation(value = "Получить список запросов на списание по счету")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = TransportableAccount.class),
            @ApiResponse(code = 401, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Account not found")
    })
    @GetMapping(value = "/payreq/get", produces = "application/json")
    public ResponseEntity<List<TransportablePayRequest>> payreqGet(@RequestParam(name = "username") String username, @CookieValue(value = "session_id", required = false) String sessionId) throws NotFoundException, NotAuthorizedException, AuthErrorException {
        checkAuthService.checkAuth(sessionId, username);
        List<TransportablePayRequest> payRequests = accountService.getPayReqsByUsername(username);
        return ResponseEntity.status(HttpStatus.OK).body(payRequests);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>> accountExceptionProcessing(ConflictException conflictException) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("errorReason", conflictException.getMessage()));
    }

    @ExceptionHandler(NotAuthorizedException.class)
    public ResponseEntity<Map<String, String>> accountExceptionProcessing(NotAuthorizedException notAuthorizedException) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("errorReason", notAuthorizedException.getMessage()));
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

    @ExceptionHandler(AuthErrorException.class)
    public ResponseEntity<Map<String, String>> accountExceptionProcessing(AuthErrorException authErrorException) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("errorReason", authErrorException.getMessage()));
    }


}
