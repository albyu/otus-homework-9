package ru.boldyrev.otus.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ExternalDeclineException extends Throwable {
    private final String message;

}
