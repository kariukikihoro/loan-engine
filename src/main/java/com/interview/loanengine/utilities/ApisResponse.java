package com.interview.loanengine.utilities;

public record ApisResponse(String message, Object object, Integer status) {
    public static ApisResponse of(String message, Object object, Integer status) {
        return new ApisResponse(message, object, status);
    }
}
