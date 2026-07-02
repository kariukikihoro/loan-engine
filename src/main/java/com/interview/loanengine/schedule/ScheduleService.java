package com.interview.loanengine.schedule;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ScheduleService {

    List<ScheduleResponse> searchSchedules(String loanId, String loanProductId, Pageable pageable);
}
