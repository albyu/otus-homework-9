package ru.boldyrev.otus.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PaymentErrorException extends Exception {
    private final String message;
}
