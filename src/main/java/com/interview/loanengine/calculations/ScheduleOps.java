package com.interview.loanengine.calculations;

import com.interview.loanengine.loan.Loan;
import com.interview.loanengine.schedule.InstallmentStatus;
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
     * Option A: Reduce EMI, Keep Tenor. All apply* methods take the loan's active schedule and
     * look installments up by number, so they stay correct across repeated prepayments.
     */
    public static RescheduleResult applyReduceEmiKeepTenor(Loan loan, List<Schedule> baseSchedule,
                                                           int prepaymentInstallmentNumber, BigDecimal prepaymentAmount) {

        validatePrepayment(baseSchedule, prepaymentInstallmentNumber, prepaymentAmount);
        int lastInstallmentNumber = lastInstallmentNumber(baseSchedule);

        BigDecimal monthlyRate = convertToMonthlyRate(loan.getLoanProduct().getInterestRate());
        BigDecimal outstanding = outstandingBefore(loan, baseSchedule, prepaymentInstallmentNumber);

        if (prepaymentAmount.compareTo(outstanding) >= 0) {
            throw new InvalidPrepaymentException("Prepayment amount " + prepaymentAmount
                    + " must be less than the outstanding principal " + outstanding
                    + "; use full early settlement to close the loan.");
        }

        BigDecimal newPrincipal = scale(outstanding.subtract(prepaymentAmount));
        int newTenor = lastInstallmentNumber - prepaymentInstallmentNumber;
        BigDecimal newEmi = calculateEquatedMonthlyInstallment(newPrincipal, monthlyRate, newTenor);

        List<Schedule> reschedule = new ArrayList<>(newTenor);
        BigDecimal opening = newPrincipal;
        BigDecimal runningAmount = BigDecimal.ZERO;
        BigDecimal runningPrincipal = BigDecimal.ZERO;

        for (int i = 0; i < newTenor; i++) {
            int installmentNumber = prepaymentInstallmentNumber + 1 + i;
            LocalDate scheduleDate = scheduleDateFor(baseSchedule, installmentNumber);
            boolean lastInstallment = (i == newTenor - 1);

            BigDecimal interest = calculateInterest(opening, monthlyRate);
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

        validatePrepayment(baseSchedule, prepaymentInstallmentNumber, prepaymentAmount);

        BigDecimal monthlyRate = convertToMonthlyRate(loan.getLoanProduct().getInterestRate());
        BigDecimal outstanding = outstandingBefore(loan, baseSchedule, prepaymentInstallmentNumber);

        if (prepaymentAmount.compareTo(outstanding) >= 0) {
            throw new InvalidPrepaymentException("Prepayment amount " + prepaymentAmount
                    + " must be less than the outstanding principal " + outstanding
                    + "; use full early settlement to close the loan.");
        }

        BigDecimal newPrincipal = scale(outstanding.subtract(prepaymentAmount));
        BigDecimal emi = loan.getEquatedMonthlyInstallment();

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

        validatePrepayment(baseSchedule, prepaymentInstallmentNumber, prepaymentAmount);
        int lastInstallmentNumber = lastInstallmentNumber(baseSchedule);

        BigDecimal outstanding = outstandingBefore(loan, baseSchedule, prepaymentInstallmentNumber);
        BigDecimal emi = loan.getEquatedMonthlyInstallment();

        int remainingInstallments = lastInstallmentNumber - prepaymentInstallmentNumber + 1;

        BigDecimal totalRemainingDue = baseSchedule.stream()
                .filter(s -> s.getInstallmentNumber() >= prepaymentInstallmentNumber)
                .map(Schedule::getEmiAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (prepaymentAmount.compareTo(totalRemainingDue) > 0) {
            throw new InvalidPrepaymentException("Prepayment amount " + prepaymentAmount
                    + " exceeds the total remaining obligations " + scale(totalRemainingDue)
                    + "; use full early settlement to close the loan.");
        }

        int fullyCovered = prepaymentAmount.divideToIntegralValue(emi).intValue();
        if (fullyCovered > remainingInstallments) {
            fullyCovered = remainingInstallments;
        }

        BigDecimal appliedToWholeInstallments = emi.multiply(BigDecimal.valueOf(fullyCovered));
        BigDecimal remainingPrepayment = scale(prepaymentAmount.subtract(appliedToWholeInstallments));
        int nextPayableInstallmentNumber = prepaymentInstallmentNumber + fullyCovered;

        BigDecimal remainingDueOnNextInstallment = null;
        if (remainingPrepayment.signum() > 0 && nextPayableInstallmentNumber <= lastInstallmentNumber) {
            remainingDueOnNextInstallment = scale(emi.subtract(remainingPrepayment));
        }

        return new AdvanceInstallmentResult(outstanding, prepaymentAmount, fullyCovered,
                nextPayableInstallmentNumber, remainingDueOnNextInstallment);
    }

    private static void validatePrepayment(List<Schedule> baseSchedule,
                                           int prepaymentInstallmentNumber, BigDecimal prepaymentAmount) {
        if (prepaymentAmount == null || prepaymentAmount.signum() <= 0) {
            throw new InvalidPrepaymentException("Prepayment amount must be greater than zero");
        }
        if (baseSchedule == null || baseSchedule.isEmpty()) {
            throw new InvalidPrepaymentException("A repayment schedule is required to process a prepayment");
        }
        int lastInstallmentNumber = lastInstallmentNumber(baseSchedule);
        if (prepaymentInstallmentNumber < 1 || prepaymentInstallmentNumber >= lastInstallmentNumber) {
            throw new InvalidPrepaymentException("Prepayment installment number must be between 1 and "
                    + (lastInstallmentNumber - 1));
        }
        Schedule target = findByNumber(baseSchedule, prepaymentInstallmentNumber);
        if (target != null && target.getStatus() == InstallmentStatus.PAID) {
            throw new InvalidPrepaymentException("Installment " + prepaymentInstallmentNumber
                    + " has already been paid; prepay at a later installment.");
        }
    }

    /**
     * Principal still owed entering the given installment, resolved by installment number since
     * a rescheduled active schedule can skip the superseded prepayment installment.
     */
    private static BigDecimal outstandingBefore(Loan loan, List<Schedule> baseSchedule, int installmentNumber) {
        Schedule previous = null;
        for (Schedule schedule : baseSchedule) {
            if (schedule.getInstallmentNumber() < installmentNumber) {
                previous = schedule;
            }
        }
        if (previous == null) {
            return scale(loan.getLoanedAmount());
        }
        BigDecimal principalBalance = previous.getPrincipalBalance();
        if (principalBalance == null) {
            principalBalance = loan.getLoanedAmount().subtract(previous.getPrincipalRunningBalance());
        }
        return scale(principalBalance);
    }

    /**
     * Scheduled date for an installment number, extrapolating monthly past the end of the schedule.
     */
    private static LocalDate scheduleDateFor(List<Schedule> baseSchedule, int installmentNumber) {
        Schedule match = findByNumber(baseSchedule, installmentNumber);
        if (match != null) {
            return match.getScheduledDate();
        }
        Schedule last = baseSchedule.get(baseSchedule.size() - 1);
        return last.getScheduledDate().plusMonths(installmentNumber - last.getInstallmentNumber());
    }

    private static Schedule findByNumber(List<Schedule> baseSchedule, int installmentNumber) {
        return baseSchedule.stream()
                .filter(s -> s.getInstallmentNumber() == installmentNumber)
                .findFirst()
                .orElse(null);
    }

    private static int lastInstallmentNumber(List<Schedule> baseSchedule) {
        return baseSchedule.get(baseSchedule.size() - 1).getInstallmentNumber();
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
