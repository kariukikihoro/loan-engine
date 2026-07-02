package com.interview.loanengine.calculations;

import com.interview.loanengine.loan.Loan;
import com.interview.loanengine.schedule.Schedule;
import com.interview.loanengine.utilities.exceptions.InvalidPrepaymentException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.interview.loanengine.calculations.LoanCalculations.calculateEquatedMonthlyInstallment;
import static com.interview.loanengine.calculations.LoanCalculations.calculateInterest;
import static com.interview.loanengine.calculations.LoanCalculations.calculatePrincipalAmount;
import static com.interview.loanengine.calculations.LoanCalculations.calculatePrincipalRunningBalance;
import static com.interview.loanengine.calculations.LoanCalculations.calculateRunningBalance;
import static com.interview.loanengine.calculations.LoanCalculations.convertToMonthlyRate;
import static com.interview.loanengine.calculations.LoanCalculations.scale;

public final class ScheduleOps {

    private ScheduleOps() {
    }


    public static List<Schedule> generateSchedule(Loan loan, LocalDate firstPaymentDate) {
        int tenor = loan.getLoanProduct().getTenureInMonths();
        List<Schedule> schedules = new ArrayList<>(tenor);
        Schedule previous = null;
        for (int i = 0; i < tenor; i++) {
            Schedule current = populateScheduleValues(firstPaymentDate.plusMonths(i), previous, loan);
            schedules.add(current);
            previous = current;
        }
        return schedules;
    }

    /**
     * Option A: Reduce EMI, Keep Tenor.
     */
    public static RescheduleResult applyReduceEmiKeepTenor(Loan loan, List<Schedule> baseSchedule,
                                                           int prepaymentInstallmentNumber, BigDecimal prepaymentAmount) {

        int originalTenor = loan.getLoanProduct().getTenureInMonths();

        validatePrepayment(originalTenor, baseSchedule, prepaymentInstallmentNumber, prepaymentAmount);

        BigDecimal monthlyRate = convertToMonthlyRate(loan.getLoanProduct().getInterestRate());
        BigDecimal outstanding = outstandingBefore(loan, baseSchedule, prepaymentInstallmentNumber);

        if (prepaymentAmount.compareTo(outstanding) >= 0) {
            throw new InvalidPrepaymentException("Prepayment amount " + prepaymentAmount
                    + " must be less than the outstanding principal " + outstanding
                    + "; use full early settlement to close the loan.");
        }

        BigDecimal newPrincipal = scale(outstanding.subtract(prepaymentAmount));
        int newTenor = originalTenor - prepaymentInstallmentNumber;
        BigDecimal newEmi = calculateEquatedMonthlyInstallment(newPrincipal, monthlyRate, newTenor);

        List<Schedule> reschedule = new ArrayList<>(newTenor);
        BigDecimal opening = newPrincipal;
        BigDecimal runningAmount = BigDecimal.ZERO;
        BigDecimal runningPrincipal = BigDecimal.ZERO;

        for (int i = 0; i < newTenor; i++) {
            int installmentNumber = prepaymentInstallmentNumber + 1 + i;
            LocalDate scheduleDate = baseSchedule.get(installmentNumber - 1).getScheduledDate();
            boolean lastInstallment = (i == newTenor - 1);

            BigDecimal interest = calculateInterest(opening, monthlyRate);
            // Clear any sub-cent residual on the final installment so the loan settles to zero.
            BigDecimal principal = lastInstallment ? opening : calculatePrincipalAmount(newEmi, interest);
            BigDecimal emi = lastInstallment ? scale(principal.add(interest)) : newEmi;
            BigDecimal closing = lastInstallment ? scale(BigDecimal.ZERO) : scale(opening.subtract(principal));

            runningAmount = calculateRunningBalance(runningAmount, emi);
            runningPrincipal = calculatePrincipalRunningBalance(runningPrincipal, principal);

            reschedule.add(Schedule.builder()
                    .installmentNumber(installmentNumber)
                    .scheduledDate(scheduleDate)
                    .principalAmount(principal)
                    .interest(interest)
                    .emiAmount(emi)
                    .runningBalance(runningAmount)
                    .principalRunningBalance(runningPrincipal)
                    .principalBalance(closing)
                    .loan(loan)
                    .build());

            opening = closing;
        }

        return new RescheduleResult(outstanding, newPrincipal, newEmi, newTenor, reschedule);
    }

    /**
     * Option B: Reduce Tenor, Keep EMI.
     */
    public static RescheduleResult applyReduceTenorKeepEmi(Loan loan, List<Schedule> baseSchedule,
                                                           int prepaymentInstallmentNumber, BigDecimal prepaymentAmount) {

        int originalTenor = loan.getLoanProduct().getTenureInMonths();
        validatePrepayment(originalTenor, baseSchedule, prepaymentInstallmentNumber, prepaymentAmount);

        BigDecimal monthlyRate = convertToMonthlyRate(loan.getLoanProduct().getInterestRate());
        BigDecimal outstanding = outstandingBefore(loan, baseSchedule, prepaymentInstallmentNumber);

        if (prepaymentAmount.compareTo(outstanding) >= 0) {
            throw new InvalidPrepaymentException("Prepayment amount " + prepaymentAmount
                    + " must be less than the outstanding principal " + outstanding
                    + "; use full early settlement to close the loan.");
        }

        BigDecimal newPrincipal = scale(outstanding.subtract(prepaymentAmount));
        BigDecimal emi = loan.getEquatedMonthlyInstallment();   // EMI is kept unchanged

        List<Schedule> reschedule = new ArrayList<>();
        BigDecimal opening = newPrincipal;
        BigDecimal runningAmount = BigDecimal.ZERO;
        BigDecimal runningPrincipal = BigDecimal.ZERO;
        int installmentNumber = prepaymentInstallmentNumber;

        while (opening.signum() > 0) {
            installmentNumber++;
            LocalDate scheduleDate = scheduleDateFor(baseSchedule, installmentNumber);

            BigDecimal interest = calculateInterest(opening, monthlyRate);

            boolean settles = opening.add(interest).compareTo(emi) <= 0;

            BigDecimal principal = settles ? opening : calculatePrincipalAmount(emi, interest);

            BigDecimal amount = settles ? scale(principal.add(interest)) : emi;
            BigDecimal closing = settles ? scale(BigDecimal.ZERO) : scale(opening.subtract(principal));

            runningAmount = calculateRunningBalance(runningAmount, amount);
            runningPrincipal = calculatePrincipalRunningBalance(runningPrincipal, principal);

            reschedule.add(Schedule.builder()
                    .installmentNumber(installmentNumber)
                    .scheduledDate(scheduleDate)
                    .principalAmount(principal)
                    .interest(interest)
                    .emiAmount(amount)
                    .runningBalance(runningAmount)
                    .principalRunningBalance(runningPrincipal)
                    .principalBalance(closing)
                    .loan(loan)
                    .build());

            opening = closing;
        }

        return new RescheduleResult(outstanding, newPrincipal, emi, reschedule.size(), reschedule);
    }

    /**
     *  Option C: Advance Installments (No Recalculation).
     */
    public static AdvanceInstallmentResult applyAdvanceInstallments(Loan loan, List<Schedule> baseSchedule,
                                                                    int prepaymentInstallmentNumber, BigDecimal prepaymentAmount) {

        int originalTenor = loan.getLoanProduct().getTenureInMonths();
        validatePrepayment(originalTenor, baseSchedule, prepaymentInstallmentNumber, prepaymentAmount);

        BigDecimal outstanding = outstandingBefore(loan, baseSchedule, prepaymentInstallmentNumber);
        BigDecimal emi = loan.getEquatedMonthlyInstallment();

        // Installments still due fromLoan the prepayment installment onwards (inclusive).
        int remainingInstallments = originalTenor - prepaymentInstallmentNumber + 1;

        int fullyCovered = prepaymentAmount.divideToIntegralValue(emi).intValue();
        if (fullyCovered > remainingInstallments) {
            fullyCovered = remainingInstallments;
        }

        BigDecimal appliedToWholeInstallments = emi.multiply(BigDecimal.valueOf(fullyCovered));
        BigDecimal remainingPrepayment = scale(prepaymentAmount.subtract(appliedToWholeInstallments));
        int nextPayableInstallmentNumber = prepaymentInstallmentNumber + fullyCovered;

        BigDecimal remainingDueOnNextInstallment = null;
        if (remainingPrepayment.signum() > 0 && nextPayableInstallmentNumber <= originalTenor) {
            remainingDueOnNextInstallment = scale(emi.subtract(remainingPrepayment));
        }

        return new AdvanceInstallmentResult(outstanding, prepaymentAmount, fullyCovered,
                nextPayableInstallmentNumber, remainingDueOnNextInstallment);
    }

    private static void validatePrepayment(int originalTenor, List<Schedule> baseSchedule,
                                           int prepaymentInstallmentNumber, BigDecimal prepaymentAmount) {
        if (prepaymentAmount == null || prepaymentAmount.signum() <= 0) {
            throw new InvalidPrepaymentException("Prepayment amount must be greater than zero");
        }
        if (prepaymentInstallmentNumber < 1 || prepaymentInstallmentNumber >= originalTenor) {
            throw new InvalidPrepaymentException("Prepayment installment number must be between 1 and "
                    + (originalTenor - 1));
        }
        if (baseSchedule == null || baseSchedule.size() < originalTenor) {
            throw new InvalidPrepaymentException("A complete base repayment schedule is required to process a prepayment");
        }
    }

    /**
     * Principal still owed at the moment of the given installment (all prior installments assumed paid).
     */
    private static BigDecimal outstandingBefore(Loan loan, List<Schedule> baseSchedule, int installmentNumber) {
        if (installmentNumber == 1) {
            return scale(loan.getLoanedAmount());
        }
        Schedule previous = baseSchedule.get(installmentNumber - 2);
        BigDecimal principalBalance = previous.getPrincipalBalance();
        if (principalBalance == null) {
            principalBalance = loan.getLoanedAmount().subtract(previous.getPrincipalRunningBalance());
        }
        return scale(principalBalance);
    }

    /**
     * Scheduled date for an installment number
     */
    private static LocalDate scheduleDateFor(List<Schedule> baseSchedule, int installmentNumber) {
        if (installmentNumber <= baseSchedule.size()) {
            return baseSchedule.get(installmentNumber - 1).getScheduledDate();
        }
        LocalDate lastDate = baseSchedule.get(baseSchedule.size() - 1).getScheduledDate();
        return lastDate.plusMonths(installmentNumber - baseSchedule.size());
    }

    private static Schedule populateScheduleValues(LocalDate scheduleDate, Schedule previousSchedule, Loan loan) {

        BigDecimal monthlyRate = convertToMonthlyRate(loan.getLoanProduct().getInterestRate());

        BigDecimal openingBalance = previousSchedule == null
                ? loan.getLoanedAmount()
                : previousSchedule.getPrincipalBalance();

        BigDecimal scheduleInterest = calculateInterest(openingBalance, monthlyRate);
        BigDecimal emiAmount = loan.getEquatedMonthlyInstallment();
        BigDecimal principalAmount = calculatePrincipalAmount(emiAmount, scheduleInterest);

        int installmentNumber = previousSchedule == null ? 1 : previousSchedule.getInstallmentNumber() + 1;
        BigDecimal previousRunningBalance = previousSchedule == null ? BigDecimal.ZERO : previousSchedule.getRunningBalance();
        BigDecimal previousPrincipalRunningBalance = previousSchedule == null
                ? BigDecimal.ZERO : previousSchedule.getPrincipalRunningBalance();

        return Schedule.builder()
                .installmentNumber(installmentNumber)
                .scheduledDate(scheduleDate)
                .principalAmount(principalAmount)
                .interest(scheduleInterest)
                .emiAmount(emiAmount)
                .runningBalance(calculateRunningBalance(previousRunningBalance, emiAmount))
                .principalRunningBalance(calculatePrincipalRunningBalance(previousPrincipalRunningBalance, principalAmount))
                .principalBalance(scale(openingBalance.subtract(principalAmount)))
                .loan(loan)
                .build();
    }
}
