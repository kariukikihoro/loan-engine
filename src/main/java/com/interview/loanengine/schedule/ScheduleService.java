package com.interview.loanengine.schedule;

import com.interview.loanengine.utilities.PageResponse;
import org.springframework.data.domain.Pageable;

public interface ScheduleService {

    PageResponse<ScheduleResponse> searchSchedules(String loanId, String loanProductId, Pageable pageable);
}
