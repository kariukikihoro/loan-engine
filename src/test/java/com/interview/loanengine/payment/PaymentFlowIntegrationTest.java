package com.interview.loanengine.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.loanengine.schedule.InstallmentStatus;
import com.interview.loanengine.schedule.Schedule;
import com.interview.loanengine.schedule.ScheduleRepository;
import com.interview.loanengine.transactionlogs.TransactionLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private TransactionLogRepository transactionLogRepository;

    private String createProduct(String name) throws Exception {
        return createProduct(name, 60);
    }

    private String createProduct(String name, int tenureInMonths) throws Exception {
        String body = """
                {"productName":"%s","productDescription":"desc","tenureInMonths":%d,"interestRate":12,"firstPaymentMonth":7}
                """.formatted(name, tenureInMonths);
        String response = mockMvc.perform(post("/loan-products/create")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("object").path("id").asText();
    }

    private String createLoan(String productId) throws Exception {
        String body = """
                {"loanProductId":"%s","loanAmount":1000000,"firstPaymentDate":"2024-07-24"}
                """.formatted(productId);
        String response = mockMvc.perform(post("/loans/create")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("object").path("id").asText();
    }

    @Test
    void emiPaymentPublishesEventThatMarksInstallmentsPaidAndAttachesLog() throws Exception {
        String loanId = createLoan(createProduct("Flow EMI Product"));

        String payResponse = mockMvc.perform(post("/loans/{loanId}/payments", loanId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"numberOfInstallments\":2}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String transactionId = objectMapper.readTree(payResponse).path("object").path("id").asText();

        List<Schedule> paid = scheduleRepository
                .findByLoanIdAndStatusOrderByInstallmentNumberAsc(loanId, InstallmentStatus.PAID);
        assertEquals(2, paid.size());
        assertTrue(paid.stream().allMatch(s -> s.getTransactionLog() != null
                && s.getTransactionLog().getId().equals(transactionId)));
    }

    @Test
    void reduceEmiKeepTenorPrepaymentAdjustsOldScheduleAndRecalculates() throws Exception {
        String loanId = createLoan(createProduct("Flow Prepay Product"));

        mockMvc.perform(post("/loans/{loanId}/prepayments", loanId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"option":"REDUCE_EMI_KEEP_TENOR","installmentNumber":24,"amount":200000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object.newTenor").value(36))
                .andExpect(jsonPath("$.object.newEmi").value(16112.86));

        long adjusted = scheduleRepository
                .findByLoanIdAndStatusOrderByInstallmentNumberAsc(loanId, InstallmentStatus.ADJUSTED).size();
        long pending = scheduleRepository
                .findByLoanIdAndStatusOrderByInstallmentNumberAsc(loanId, InstallmentStatus.PENDING).size();
        assertEquals(37, adjusted);
        assertEquals(23 + 36, pending);
    }

    @Test
    void reduceTenorKeepEmiPrepaymentShortensTheLoan() throws Exception {
        String loanId = createLoan(createProduct("Flow Option B Product"));

        mockMvc.perform(post("/loans/{loanId}/prepayments", loanId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"option":"REDUCE_TENOR_KEEP_EMI","installmentNumber":24,"amount":200000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object.newTenor").value(25))
                .andExpect(jsonPath("$.object.newOutstanding").value(485118.09));

        mockMvc.perform(get("/loans/find-by-id").param("loanId", loanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object.tenure").value(48));
    }

    @Test
    void advanceInstallmentsPrepaymentCoversWholeEmisAndPersistsLeftover() throws Exception {
        String loanId = createLoan(createProduct("Flow Option C Product"));

        mockMvc.perform(post("/loans/{loanId}/prepayments", loanId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"option":"ADVANCE_INSTALLMENTS","installmentNumber":24,"amount":200000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object.installmentsFullyCovered").value(8))
                .andExpect(jsonPath("$.object.nextPayableInstallment").value(32))
                .andExpect(jsonPath("$.object.remainingDueOnNextInstallment").value(200.03));

        List<Schedule> paid = scheduleRepository
                .findByLoanIdAndStatusOrderByInstallmentNumberAsc(loanId, InstallmentStatus.PAID);
        assertEquals(List.of(24, 25, 26, 27, 28, 29, 30, 31),
                paid.stream().map(Schedule::getInstallmentNumber).toList());

        Schedule next = scheduleRepository
                .findByLoanIdAndStatusOrderByInstallmentNumberAsc(loanId, InstallmentStatus.PENDING).stream()
                .filter(s -> s.getInstallmentNumber() == 32)
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("22044.42"), next.getPrepaidAmount().setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    void secondPrepaymentWorksOffTheRescheduledBalances() throws Exception {
        String loanId = createLoan(createProduct("Flow Repeat Prepay Product"));

        mockMvc.perform(post("/loans/{loanId}/prepayments", loanId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"option":"REDUCE_EMI_KEEP_TENOR","installmentNumber":24,"amount":200000}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/loans/{loanId}/prepayments", loanId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"option":"REDUCE_EMI_KEEP_TENOR","installmentNumber":30,"amount":100000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object.newTenor").value(30));
    }

    @Test
    void invalidPrepaymentRollsBackTheWholeOperation() throws Exception {
        String loanId = createLoan(createProduct("Flow Rollback Product"));

        mockMvc.perform(post("/loans/{loanId}/prepayments", loanId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"option":"REDUCE_EMI_KEEP_TENOR","installmentNumber":24,"amount":800000}
                                """))
                .andExpect(status().isBadRequest());

        assertTrue(transactionLogRepository.findAll().stream()
                .noneMatch(log -> log.getLoan().getId().equals(loanId)));
        assertEquals(60, scheduleRepository
                .findByLoanIdAndStatusOrderByInstallmentNumberAsc(loanId, InstallmentStatus.PENDING).size());
    }

    @Test
    void validationAndNotFoundErrorsReturnStructuredResponses() throws Exception {
        String loanId = createLoan(createProduct("Flow Validation Product"));

        mockMvc.perform(post("/loans/{loanId}/prepayments", loanId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"option":"REDUCE_EMI_KEEP_TENOR","installmentNumber":24,"amount":-5}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/loans/{loanId}/prepayments", "no-such-loan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"option":"REDUCE_EMI_KEEP_TENOR","installmentNumber":24,"amount":1000}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/loans/{loanId}/payments", loanId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"numberOfInstallments\":61}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/loans/{loanId}/payments", loanId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"numberOfInstallments\":2}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/loans/{loanId}/prepayments", loanId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"option":"REDUCE_EMI_KEEP_TENOR","installmentNumber":1,"amount":1000}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void payingEveryInstallmentClosesTheLoan() throws Exception {
        String loanId = createLoan(createProduct("Flow Close-out Product", 3));

        mockMvc.perform(post("/loans/{loanId}/payments", loanId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"numberOfInstallments\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object.outstandingBalance").value(0.00));

        mockMvc.perform(get("/loans/find-by-id").param("loanId", loanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object.status").value("CLOSED"));
    }
}
