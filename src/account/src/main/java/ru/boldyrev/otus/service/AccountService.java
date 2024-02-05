package ru.boldyrev.otus.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.boldyrev.otus.exception.ExternalDeclineException;
import ru.boldyrev.otus.exception.NotFoundException;
import ru.boldyrev.otus.listener.AccountListener;
import ru.boldyrev.otus.model.entity.Account;
import ru.boldyrev.otus.model.entity.PayRequest;
import ru.boldyrev.otus.model.entity.Transaction;
import ru.boldyrev.otus.model.enums.PayResult;
import ru.boldyrev.otus.model.enums.TransactionType;
import ru.boldyrev.otus.model.transfer.*;
import ru.boldyrev.otus.repo.AccountRepo;
import ru.boldyrev.otus.repo.PayRequestRepo;
import ru.boldyrev.otus.repo.TransactionRepo;

import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {
    private final TransactionRepo transactionRepo;
    private final PayRequestRepo payRequestRepo;
    private final AccountRepo accountRepo;

    private final AccountListener accountListener;

    @Autowired
    @Lazy
    public AccountService(TransactionRepo transactionRepo, PayRequestRepo payRequestRepo, AccountRepo accountRepo, AccountListener accountListener) {
        this.transactionRepo = transactionRepo;
        this.payRequestRepo = payRequestRepo;
        this.accountRepo = accountRepo;
        this.accountListener = accountListener;
    }


    public TransportableAccount getTransportableAccountByUsername(String username) throws NotFoundException {
        Account account = accountRepo.findByUserName(username).orElseThrow(() -> new NotFoundException("Account not found"));
        return new TransportableAccount(account);
    }

    public Optional<Account> getAccountByUsernameNoException(String username) {
        return accountRepo.findByUserName(username);
    }

    public List<TransportableTransaction> getTrxnsByUsername(String username) throws NotFoundException {
        Account account = accountRepo.findByUserName(username).orElseThrow(() -> new NotFoundException("Account not found"));
        List<Transaction> trxns = transactionRepo.findByAccountId(account.getId());
        List<TransportableTransaction> transportableTrxns = trxns.stream()
                .map(TransportableTransaction::new)
                .toList();
        return transportableTrxns;
    }

    public List<TransportablePayRequest> getPayReqsByUsername(String username) throws NotFoundException {
        List<PayRequest> payReqs = payRequestRepo.findByUsername(username);
        List<TransportablePayRequest> transportablePayReqs = payReqs.stream()
                .map(TransportablePayRequest::new)
                .toList();
        return transportablePayReqs;
    }


    @Transactional
    public TransportableAccount creditAccountByUsername(TransportableCreditRequest creditRequest) throws NotFoundException, ExternalDeclineException {
        Account account = accountRepo.findByUserName(creditRequest.getUsername()).orElseThrow(() -> new NotFoundException("Account not found"));
        /* External Credit here */

        /*делаем транзакцию и сохраняем ее*/
        Transaction creditTrxn = new Transaction()
                .setAccount(account)
                .setTransactionType(TransactionType.CREDIT)
                .setAmount(creditRequest.getAmount())
                .setTimestamp(Timestamp.valueOf(LocalDateTime.now()));
        transactionRepo.saveAndFlush(creditTrxn);

        /*увеличиваем остаток на счете и сохраняем*/
        account.setBalance(account.getBalance() + creditRequest.getAmount());
        accountRepo.saveAndFlush(account);

        return new TransportableAccount(account);
    }

    @Transactional
    public void saveAccount(Account account) {
        accountRepo.saveAndFlush(account);
    }

    @Transactional
    public TransportablePayRequest debitAccountByUsername(TransportablePayRequest tPayReq) {
        PayRequest payReq = new PayRequest()
                .setUsername(tPayReq.getUsername())
                .setOrderId(tPayReq.getOrderId())
                .setAmount(tPayReq.getAmount())
                .setTimestamp(tPayReq.getTimestamp() != null ? tPayReq.getTimestamp() : Timestamp.valueOf(LocalDateTime.now()));


        Optional<Account> optAccount = accountRepo.findByUserName(tPayReq.getUsername());

        if (optAccount.isEmpty()) {
            payReq.setPayResult(PayResult.NO_ACCOUNT);
            payRequestRepo.saveAndFlush(payReq);

            return new TransportablePayRequest(payReq);

        } else {
            Account account = optAccount.get();
            if (account.getBalance() >= tPayReq.getAmount()) {
                /*Дебетуем счет*/
                account.setBalance(account.getBalance() - payReq.getAmount());

                /*Создаем транзакцию*/
                Transaction trxn = new Transaction()
                        .setAccount(account)
                        .setTransactionType(TransactionType.PAYMENT)
                        .setTimestamp(Timestamp.valueOf(LocalDateTime.now()))
                        .setOrderId(tPayReq.getOrderId())
                        .setAmount(tPayReq.getAmount());

                trxn = transactionRepo.saveAndFlush(trxn);

                /*Обновляем account*/

                account = accountRepo.saveAndFlush(account);

                /*Дополняем PayRequest*/
                payReq.setTransaction(trxn).setPayResult(PayResult.SUCCESS);


            } else {
                /*Недостаточно средств на счете*/
                payReq.setPayResult(PayResult.NOT_SUFFICIENT_FUNDS);
            }

            /* сохраняем PayRequest */
            payReq = payRequestRepo.saveAndFlush(payReq);

            /*  отправляем уведомление */
            tPayReq = new TransportablePayRequest(payReq);

            accountListener.sendNotification(tPayReq);

            /* */
            return tPayReq;

        }
    }
}
