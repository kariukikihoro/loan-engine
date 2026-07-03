package com.interview.loanengine.loanproduct;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/loan-products")
@RequiredArgsConstructor
@Tag(name = "Loan Products", description = "Loan product catalogue")
public class LoanProductController {

    private final LoanProductService loanProductService;

    @Operation(summary = "Create a loan product")
    @ApiResponse(responseCode = "201", description = "Loan product created",
            content = @Content(schema = @Schema(implementation = ApisResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "message": "Loan product created",
                              "object": {
                                "id": "prod-1",
                                "productName": "Personal Loan",
                                "productDescription": "Unsecured personal loan",
                                "tenureInMonths": 12,
                                "interestRate": 12.0,
                                "firstPaymentMonth": 1
                              },
                              "status": 201
                            }
                            """)))
    @PostMapping("/create")
    public ResponseEntity<ApisResponse> create(
            @RequestBody(content = @Content(examples = @ExampleObject(value = """
                    {
                      "productName": "Personal Loan",
                      "productDescription": "Unsecured personal loan",
                      "tenureInMonths": 12,
                      "interestRate": 12.0,
                      "firstPaymentMonth": 1
                    }
                    """)))
            @Valid @org.springframework.web.bind.annotation.RequestBody LoanProductRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApisResponse.of(
                "Loan product created", loanProductService.createLoanProduct(request), HttpStatus.CREATED.value()));
    }

    @Operation(summary = "Find a loan product by id")
    @ApiResponse(responseCode = "200", description = "Loan product retrieved",
            content = @Content(schema = @Schema(implementation = ApisResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "message": "Loan product retrieved",
                              "object": {
                                "id": "prod-1",
                                "productName": "Personal Loan",
                                "productDescription": "Unsecured personal loan",
                                "tenureInMonths": 12,
                                "interestRate": 12.0,
                                "firstPaymentMonth": 1
                              },
                              "status": 200
                            }
                            """)))
    @GetMapping("/find-by-id")
    public ResponseEntity<ApisResponse> get(
            @Parameter(example = "prod-1") @RequestParam String id) {

        return ResponseEntity.ok(ApisResponse.of(
                "Loan product retrieved", loanProductService.findLoanProductById(id), HttpStatus.OK.value()));
    }

    @Operation(summary = "Search loan products (paged)")
    @ApiResponse(responseCode = "200", description = "Loan products retrieved",
            content = @Content(schema = @Schema(implementation = ApisResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "message": "Loan products retrieved",
                              "object": {
                                "content": [
                                  {
                                    "id": "prod-1",
                                    "productName": "Personal Loan",
                                    "tenureInMonths": 12,
                                    "interestRate": 12.0
                                  }
                                ],
                                "totalElements": 1,
                                "totalPages": 1,
                                "number": 0,
                                "size": 10
                              },
                              "status": 200
                            }
                            """)))
    @GetMapping("/search")
    public ResponseEntity<ApisResponse> search(
            @Parameter(example = "Personal") @RequestParam(required = false) String name,
            @Parameter(example = "5") @RequestParam(required = false) BigDecimal interestFrom,
            @Parameter(example = "20") @RequestParam(required = false) BigDecimal interestTo,
            @Parameter(example = "6") @RequestParam(required = false) Integer tenureFrom,
            @Parameter(example = "24") @RequestParam(required = false) Integer tenureTo,
            @Parameter(example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(example = "10") @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(ApisResponse.of("Loan products retrieved",
                loanProductService.searchLoanProducts(name, interestFrom, interestTo, tenureFrom, tenureTo,
                        PageRequest.of(page, size)),
                HttpStatus.OK.value()));
    }
}
