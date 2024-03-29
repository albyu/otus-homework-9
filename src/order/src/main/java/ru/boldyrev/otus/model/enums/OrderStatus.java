package ru.boldyrev.otus.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;


public enum OrderStatus {
    NEW("NEW"),
    IN_PROGRESS("IN PROGRESS"),
    COMPLETED("COMPLETED"),
    CANCELED("CANCELED"),
    PAYMENT_REJECTED("PAYMENT_REJECTED");

    private final String name;

    OrderStatus(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return this.name;
    }

}

