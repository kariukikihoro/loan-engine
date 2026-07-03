package com.interview.loanengine.schedule;

import com.interview.loanengine.utilities.ApisResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedules", description = "Loan repayment schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Operation(summary = "Search schedules (paged)")
    @ApiResponse(responseCode = "200", description = "Schedules retrieved",
            content = @Content(schema = @Schema(implementation = ApisResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "message": "Schedules retrieved",
                              "object": {
                                "content": [
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
                                "totalElements": 12,
                                "totalPages": 2,
                                "number": 0,
                                "size": 10
                              },
                              "status": 200
                            }
                            """)))
    @GetMapping("/search")
    public ResponseEntity<ApisResponse> search(
            @Parameter(example = "8f3c0b1e-4c2a-4d9a-9b6e-2f1a7c8e5d10") @RequestParam(required = false) String loanId,
            @Parameter(example = "prod-1") @RequestParam(required = false) String loanProductId,
            @Parameter(example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(example = "10") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApisResponse.of("Schedules retrieved",
                scheduleService.searchSchedules(loanId, loanProductId, PageRequest.of(page, size)), HttpStatus.OK.value()));
    }
}
