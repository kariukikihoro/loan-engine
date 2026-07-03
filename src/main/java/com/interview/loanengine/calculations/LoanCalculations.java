package com.interview.loanengine.calculations;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class LoanCalculations {

    public static final int CALC_SCALE = 10;
    public static final int MONEY_SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final BigDecimal MONTHS_PER_YEAR_X_100 = new BigDecimal("1200");

    private LoanCalculations() {
    }

    /**
     * Round a monetary amount to 2 dp, HALF_UP. Use only for presentation.
     */
    public static BigDecimal toMonetaryValue(BigDecimal value) {
        return value.setScale(MONEY_SCALE, ROUNDING);
    }

    /**
     * Round to the internal calculation scale so intermediate balances keep full precision
     * (matching the reference schedule) without unbounded expansion.
     * Package-visible so {@link ScheduleOps} can reuse it without re-deriving scale logic.
     */
    static BigDecimal scale(BigDecimal value) {
        return value.setScale(CALC_SCALE, ROUNDING);
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
        BigDecimal factor = pow1p(monthlyRate, tenorMonths);
        return principal.multiply(monthlyRate).multiply(factor, MC)
                .divide(factor.subtract(BigDecimal.ONE), CALC_SCALE, ROUNDING);
    }

    static BigDecimal calculateInterest(BigDecimal runningBalance, BigDecimal monthlyRate) {
        return scale(runningBalance.multiply(monthlyRate));
    }

    static BigDecimal calculatePrincipalAmount(BigDecimal emiAmount, BigDecimal interest) {
        return scale(emiAmount.subtract(interest));
    }

    static BigDecimal calculateRunningBalance(BigDecimal previousRunningBalance, BigDecimal emiAmount) {
        return scale(previousRunningBalance.add(emiAmount));
    }

    static BigDecimal calculatePrincipalRunningBalance(BigDecimal previousPrincipalRunningBalance, BigDecimal principalAmount) {
        return scale(previousPrincipalRunningBalance.add(principalAmount));
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