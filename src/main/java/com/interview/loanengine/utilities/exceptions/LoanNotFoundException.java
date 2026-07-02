package com.interview.loanengine.utilities.exceptions;

public class LoanNotFoundException extends RuntimeException {

    public LoanNotFoundException(String message) {
        super(message);
    }
}
