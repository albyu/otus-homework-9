package ru.boldyrev.otus.exception;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


@Getter
@RequiredArgsConstructor
public class NotFoundException extends Exception{

    private final String message;

}
