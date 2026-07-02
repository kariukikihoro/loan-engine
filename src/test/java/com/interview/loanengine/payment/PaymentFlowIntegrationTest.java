package com.interview.loanengine.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.loanengine.schedule.InstallmentStatus;
import com.interview.loanengine.schedule.Schedule;
import com.interview.loanengine.schedule.ScheduleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    private String createProduct(String name) throws Exception {
        String body = """
                {"productName":"%s","productDescription":"desc","tenureInMonths":60,"interestRate":12,"firstPaymentMonth":7}
                """.formatted(name);
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

        // The event listener marked the first two installments PAID and linked them to the log.
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

        // Old installments 24..60 superseded (ADJUSTED); fresh 25..60 are PENDING.
        long adjusted = scheduleRepository
                .findByLoanIdAndStatusOrderByInstallmentNumberAsc(loanId, InstallmentStatus.ADJUSTED).size();
        long pending = scheduleRepository
                .findByLoanIdAndStatusOrderByInstallmentNumberAsc(loanId, InstallmentStatus.PENDING).size();
        assertEquals(37, adjusted); // installments 24..60
        assertEquals(23 + 36, pending); // untouched 1..23 plus recalculated 25..60
    }
}
