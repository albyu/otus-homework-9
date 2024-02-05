package ru.boldyrev.otus.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AuthErrorException  extends Exception {
    private final String message;
}