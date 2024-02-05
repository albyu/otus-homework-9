package ru.boldyrev.otus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.boldyrev.otus.exception.ConflictException;
import ru.boldyrev.otus.exception.PaymentErrorException;
import ru.boldyrev.otus.model.transfer.TransportablePayRequest;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PayService {

    private final ObjectMapper objectMapper;

    @Value("${application.account-url}")
    String accountUrl;


    public TransportablePayRequest pay(TransportablePayRequest payRequest) throws PaymentErrorException {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<TransportablePayRequest> request = new HttpEntity<>(payRequest);
        try {
            ResponseEntity<TransportablePayRequest> response = restTemplate.postForEntity(accountUrl, request, TransportablePayRequest.class);
            return response.getBody();
        } catch (HttpClientErrorException.Conflict e) { /*Все ответы, кроме SUCCESS*/
            String responseBody = e.getResponseBodyAsString();
            TransportablePayRequest conflictResponse = null;
            try {
                conflictResponse = objectMapper.readValue(responseBody, TransportablePayRequest.class);
            } catch (JsonProcessingException ex) {
                log.error("Error while paying order {}", payRequest.getOrderId(), ex);
                throw new PaymentErrorException("Payment cannot be proceeded");
            }
            return conflictResponse;
        } catch (Exception e) {
            log.error("Error while paying order {}", payRequest.getOrderId(), e);
            throw new PaymentErrorException("Payment cannot be proceeded");
        }

    }


}

