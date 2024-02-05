package ru.boldyrev.otus.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BusinessError {
    public static int GENERAL_ERROR = 1;
    public static int NOT_FOUND_ERROR = 2;

    int code;
    String message;
}
