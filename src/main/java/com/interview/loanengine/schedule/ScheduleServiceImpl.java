package com.interview.loanengine.schedule;

import com.interview.loanengine.utilities.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ScheduleResponse> searchSchedules(String loanId, String loanProductId, Pageable pageable) {
        return PageResponse.from(
                scheduleRepository.searchByLoanAndProduct(loanId, loanProductId, pageable), ScheduleResponse::from);
    }
}
