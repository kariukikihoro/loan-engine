package com.interview.loanengine.calculations;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class LoanCalculations {

    public static final int CALC_SCALE = 10;
    public static final int MONEY_SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final BigDecimal MONTHS_PER_YEAR_X_100 = new BigDecimal("1200");

    private LoanCalculations() {}

    /**
     * Round a monetary amount to 2 dp, HALF_UP.
     */
    public static BigDecimal toMonetaryValue(BigDecimal value) {
        return value.setScale(MONEY_SCALE, ROUNDING);
    }

    /**
     * Monthly periodic rate as a fraction, e.g. 12.0 (% p.a.) -> 0.0100000000.
     */
    public static BigDecimal convertToMonthlyRate(BigDecimal annualRatePercent) {
        return annualRatePercent.divide(MONTHS_PER_YEAR_X_100, CALC_SCALE, ROUNDING);
    }

    /**
     * Equated Monthly Installment for an annuity loan (full internal precision).
     * EMI = P * r * (1+r)^n / ((1+r)^n - 1)
     */
    public static BigDecimal calculateEquatedMonthlyInstallment(BigDecimal principal, BigDecimal monthlyRate, int tenorMonths) {

        if (tenorMonths <= 0) {
            throw new IllegalArgumentException("tenorMonths must be positive");
        }
        if (monthlyRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(tenorMonths), CALC_SCALE, ROUNDING);
        }
        BigDecimal factor = pow1p(monthlyRate, tenorMonths);           // (1+r)^n
        return principal.multiply(monthlyRate).multiply(factor, MC)
                .divide(factor.subtract(BigDecimal.ONE), CALC_SCALE, ROUNDING);
    }

    /**
     * Outstanding principal after exactly {@code installments} payments of {@code emi}.
     */
    public static BigDecimal balanceAfter(BigDecimal principal, BigDecimal monthlyRate,
                                          BigDecimal emi, int installments) {
        BigDecimal balance = principal;
        for (int i = 1; i <= installments; i++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(CALC_SCALE, ROUNDING);
            BigDecimal principalComponent = emi.subtract(interest);
            balance = balance.subtract(principalComponent);
        }
        return balance;
    }

    /**
     * Build a full amortization schedule for {@code tenorMonths} installments, numbering
     * rows from {@code firstInstallmentNo}. Balances are carried at full precision; the
     * caller decides where to round for storage/display.
     */
    public static List<ScheduleRow> amortize(BigDecimal principal, BigDecimal monthlyRate,
                                             BigDecimal emi, int tenorMonths, int firstInstallmentNo) {
        List<ScheduleRow> rows = new ArrayList<>(tenorMonths);
        BigDecimal opening = principal;
        for (int k = 0; k < tenorMonths; k++) {
            int installmentNo = firstInstallmentNo + k;
            BigDecimal interest = opening.multiply(monthlyRate).setScale(CALC_SCALE, ROUNDING);
            BigDecimal principalComponent;
            BigDecimal payment;
            if (k == tenorMonths - 1) {
                // Final installment clears any sub-cent residual exactly.
                principalComponent = opening;
                payment = principalComponent.add(interest);
            } else {
                principalComponent = emi.subtract(interest);
                payment = emi;
            }
            BigDecimal closing = opening.subtract(principalComponent);
            rows.add(new ScheduleRow(installmentNo, opening, payment, principalComponent, interest, closing));
            opening = closing;
        }
        return rows;
    }

    private static BigDecimal pow1p(BigDecimal rate, int n) {
        BigDecimal base = BigDecimal.ONE.add(rate);
        BigDecimal result = BigDecimal.ONE;
        for (int i = 0; i < n; i++) {
            result = result.multiply(base, MC);
        }
        return result;
    }
}