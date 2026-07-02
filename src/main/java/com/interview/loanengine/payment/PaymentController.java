package com.interview.loanengine.payment;

import com.interview.loanengine.payment.dto.InstallmentPaymentRequest;
import com.interview.loanengine.payment.dto.PrepaymentRequest;
import com.interview.loanengine.utilities.ApisResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/loans/{loanId}")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Installment payments and prepayments")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Pay one or more installments")
    @ApiResponse(responseCode = "200", description = "Installment payment recorded",
            content = @Content(schema = @Schema(implementation = ApisResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "message": "Installment payment recorded",
                              "object": {
                                "id": "3d9b7a12-1f45-4a6e-8c2b-9e0d1f2a3b4c",
                                "numberOfInstallments": 2,
                                "paidInstallments": [
                                  {
                                    "installmentNumber": 1,
                                    "dueDate": "2026-08-01",
                                    "installmentAmount": 8791.59,
                                    "status": "PAID"
                                  },
                                  {
                                    "installmentNumber": 2,
                                    "dueDate": "2026-09-01",
                                    "installmentAmount": 8791.59,
                                    "status": "PAID"
                                  }
                                ],
                                "outstandingBalance": 84355.44
                              },
                              "status": 200
                            }
                            """)))
    @PostMapping("/payments")
    public ResponseEntity<ApisResponse> payInstallments(
            @Parameter(example = "8f3c0b1e-4c2a-4d9a-9b6e-2f1a7c8e5d10") @PathVariable String loanId,
            @RequestBody(content = @Content(examples = @ExampleObject(value = """
                    {
                      "numberOfInstallments": 2
                    }
                    """)))
            @Valid @org.springframework.web.bind.annotation.RequestBody InstallmentPaymentRequest request) {
        return ResponseEntity.ok(ApisResponse.of(
                "Installment payment recorded", paymentService.payInstallments(loanId, request), HttpStatus.OK.value()));
    }

    @Operation(summary = "Process a prepayment (reduce EMI, reduce tenor, or advance installments)")
    @ApiResponse(responseCode = "200", description = "Prepayment processed",
            content = @Content(schema = @Schema(implementation = ApisResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "message": "Prepayment processed",
                              "object": {
                                "id": "7c1e2d34-5a6b-4c7d-8e9f-0a1b2c3d4e5f",
                                "option": "REDUCE_EMI_KEEP_TENOR",
                                "installmentNumber": 3,
                                "amount": 20000.00,
                                "newEmi": 6875.20,
                                "newTenor": 12,
                                "newOutstanding": 72208.41,
                                "schedule": [
                                  {
                                    "installmentNumber": 3,
                                    "dueDate": "2026-10-01",
                                    "installmentAmount": 6875.20,
                                    "outstandingBalance": 66055.21,
                                    "status": "PENDING"
                                  }
                                ]
                              },
                              "status": 200
                            }
                            """)))
    @PostMapping("/prepayments")
    public ResponseEntity<ApisResponse> processPrepayment(
            @Parameter(example = "8f3c0b1e-4c2a-4d9a-9b6e-2f1a7c8e5d10") @PathVariable String loanId,
            @RequestBody(content = @Content(examples = {
                    @ExampleObject(name = "Reduce EMI", value = """
                            {
                              "option": "REDUCE_EMI_KEEP_TENOR",
                              "installmentNumber": 3,
                              "amount": 20000.00
                            }
                            """),
                    @ExampleObject(name = "Reduce Tenor", value = """
                            {
                              "option": "REDUCE_TENOR_KEEP_EMI",
                              "installmentNumber": 3,
                              "amount": 20000.00
                            }
                            """),
                    @ExampleObject(name = "Advance Installments", value = """
                            {
                              "option": "ADVANCE_INSTALLMENTS",
                              "installmentNumber": 3,
                              "amount": 20000.00
                            }
                            """)}))
            @Valid @org.springframework.web.bind.annotation.RequestBody PrepaymentRequest request) {
        return ResponseEntity.ok(ApisResponse.of(
                "Prepayment processed", paymentService.processPrepayment(loanId, request), HttpStatus.OK.value()));
    }
}
