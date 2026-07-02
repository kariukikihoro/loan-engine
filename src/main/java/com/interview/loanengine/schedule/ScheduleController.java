package com.interview.loanengine.schedule;

import com.interview.loanengine.utilities.ApisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/search")
    public ResponseEntity<ApisResponse> search(@RequestParam(required = false) String loanId,
                                               @RequestParam(required = false) String loanProductId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApisResponse.of("Schedules retrieved",
                scheduleService.searchSchedules(loanId, loanProductId, PageRequest.of(page, size)), HttpStatus.OK.value()));
    }
}
