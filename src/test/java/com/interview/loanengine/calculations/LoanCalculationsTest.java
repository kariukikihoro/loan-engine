package com.interview.loanengine.calculations;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoanCalculationsTest {

    @Test
    void convertsAnnualRateToMonthlyFraction() {
        BigDecimal monthlyRate = LoanCalculations.convertToMonthlyRate(new BigDecimal("12"));

        assertEquals(new BigDecimal("0.0100000000"), monthlyRate);
    }

    @Test
    void calculatesEmiForSampleLoan() {
        BigDecimal monthlyRate = LoanCalculations.convertToMonthlyRate(new BigDecimal("12"));

        BigDecimal emi = LoanCalculations.calculateEquatedMonthlyInstallment(
                new BigDecimal("1000000"), monthlyRate, 60);

        assertEquals(new BigDecimal("22244.45"), LoanCalculations.toMonetaryValue(emi));
    }

    @Test
    void calculatesEmiWithZeroRateAsEvenSplit() {
        BigDecimal emi = LoanCalculations.calculateEquatedMonthlyInstallment(
                new BigDecimal("1200000"), BigDecimal.ZERO, 12);

        assertEquals(new BigDecimal("100000.00"), LoanCalculations.toMonetaryValue(emi));
    }

    @Test
    void rejectsNonPositiveTenor() {
        BigDecimal monthlyRate = LoanCalculations.convertToMonthlyRate(new BigDecimal("12"));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> LoanCalculations.calculateEquatedMonthlyInstallment(new BigDecimal("100000"), monthlyRate, 0));
    }

    @Test
    void roundsMonetaryValuesHalfUpToTwoDecimalPlaces() {
        BigDecimal rounded = LoanCalculations.toMonetaryValue(new BigDecimal("12244.4549999999"));

        assertEquals(new BigDecimal("12244.45"), rounded);
        assertEquals(2, rounded.scale());
        assertEquals(RoundingMode.HALF_UP, LoanCalculations.ROUNDING);
    }
}