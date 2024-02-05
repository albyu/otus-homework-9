package ru.boldyrev.otus.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TransactionType {
    CREDIT("Credit"),
    PAYMENT("Payment"),
    REVERSAL("Reversal");

    private final String name;

    TransactionType(String name) {
        this.name = name;
    }
    @JsonValue
    public String getName() {
        return name;
    }
}
