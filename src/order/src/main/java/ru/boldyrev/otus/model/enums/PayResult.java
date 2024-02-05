package ru.boldyrev.otus.model.enums;



import com.fasterxml.jackson.annotation.JsonValue;

public enum PayResult {
    SUCCESS("Success"),
    NOT_SUFFICIENT_FUNDS("Not sufficient funds"),
    NO_ACCOUNT("No account");

    private final String name;

    PayResult(String name) {
        this.name = name;
    }
    @JsonValue
    public String getName() {
        return name;
    }
}

