package com.interview.loanengine.loan;

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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Loan origination, retrieval and repayment schedules")
public class LoanController {

    private final LoanService loanService;

    @Operation(summary = "Create a loan from a loan product")
    @ApiResponse(responseCode = "201", description = "Loan created",
            content = @Content(schema = @Schema(implementation = ApisResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "message": "Loan created",
                              "object": {
                                "id": "8f3c0b1e-4c2a-4d9a-9b6e-2f1a7c8e5d10",
                                "loanProductId": "b1a2c3d4-e5f6-7890-abcd-ef1234567890",
                                "loanAmount": 100000.00,
                                "equatedMonthlyInstallment": 8791.59,
                                "tenure": 12,
                                "outstandingBalance": 100000.00,
                                "schedule": [
                                  {
                                    "installmentNumber": 1,
                                    "dueDate": "2026-08-01",
                                    "principal": 7791.59,
                                    "interest": 1000.00,
                                    "installmentAmount": 8791.59,
                                    "outstandingBalance": 92208.41,
                                    "status": "PENDING"
                                  }
                                ]
                              },
                              "status": 201
                            }
                            """)))
    @PostMapping("/create")
    public ResponseEntity<ApisResponse> createLoan(
            @RequestBody(content = @Content(examples = @ExampleObject(value = """
                    {
                      "loanProductId": "b1a2c3d4-e5f6-7890-abcd-ef1234567890",
                      "loanAmount": 100000.00,
                      "firstPaymentDate": "2026-08-01"
                    }
                    """)))
            @Valid @org.springframework.web.bind.annotation.RequestBody LoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApisResponse.of(
                "Loan created", loanService.createLoan(request), HttpStatus.CREATED.value()));
    }

    @Operation(summary = "Find a loan by its id")
    @ApiResponse(responseCode = "200", description = "Loan retrieved",
            content = @Content(schema = @Schema(implementation = ApisResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "message": "Loan retrieved",
                              "object": {
                                "id": "8f3c0b1e-4c2a-4d9a-9b6e-2f1a7c8e5d10",
                                "loanProductId": "b1a2c3d4-e5f6-7890-abcd-ef1234567890",
                                "loanAmount": 100000.00,
                                "equatedMonthlyInstallment": 8791.59,
                                "tenure": 12,
                                "outstandingBalance": 92208.41
                              },
                              "status": 200
                            }
                            """)))
    @GetMapping("/find-by-id")
    public ResponseEntity<ApisResponse> findLoanById(
            @Parameter(example = "8f3c0b1e-4c2a-4d9a-9b6e-2f1a7c8e5d10") @RequestParam String loanId) {
        return ResponseEntity.ok(ApisResponse.of(
                "Loan retrieved", loanService.findLoanById(loanId), HttpStatus.OK.value()));
    }

    @Operation(summary = "Get the repayment schedule for a loan")
    @ApiResponse(responseCode = "200", description = "Repayment schedule retrieved",
            content = @Content(schema = @Schema(implementation = ApisResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "message": "Repayment schedule retrieved",
                              "object": [
                                {
                                  "installmentNumber": 1,
                                  "dueDate": "2026-08-01",
                                  "principal": 7791.59,
                                  "interest": 1000.00,
                                  "installmentAmount": 8791.59,
                                  "outstandingBalance": 92208.41,
                                  "status": "PENDING"
                                }
                              ],
                              "status": 200
                            }
                            """)))
    @GetMapping("/loan-schedule")
    public ResponseEntity<ApisResponse> getLoanSchedules(
            @Parameter(example = "8f3c0b1e-4c2a-4d9a-9b6e-2f1a7c8e5d10") @RequestParam String loanId) {
        return ResponseEntity.ok(ApisResponse.of(
                "Repayment schedule retrieved", loanService.findLoanSchedules(loanId), HttpStatus.OK.value()));
    }

    @Operation(summary = "Search loans by tenure and loaned amount range")
    @ApiResponse(responseCode = "200", description = "Loans retrieved",
            content = @Content(schema = @Schema(implementation = ApisResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "message": "Loans retrieved",
                              "object": [
                                {
                                  "id": "8f3c0b1e-4c2a-4d9a-9b6e-2f1a7c8e5d10",
                                  "loanAmount": 100000.00,
                                  "tenure": 12,
                                  "outstandingBalance": 92208.41
                                }
                              ],
                              "status": 200
                            }
                            """)))
    @GetMapping("/search")
    public ResponseEntity<ApisResponse> searchLoans(
            @Parameter(example = "12") @RequestParam(required = false) Integer tenure,
            @Parameter(example = "50000") @RequestParam(required = false) BigDecimal loanedAmountFrom,
            @Parameter(example = "150000") @RequestParam(required = false) BigDecimal loanedAmountTo) {
        return ResponseEntity.ok(ApisResponse.of("Loans retrieved",
                loanService.searchLoans(tenure, loanedAmountFrom, loanedAmountTo), HttpStatus.OK.value()));
    }
}
