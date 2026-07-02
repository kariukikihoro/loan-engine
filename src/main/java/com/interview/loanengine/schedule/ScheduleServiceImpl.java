package com.interview.loanengine.schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> searchSchedules(String loanId, String loanProductId, Pageable pageable) {
        return scheduleRepository.searchByLoanAndProduct(loanId, loanProductId, pageable)
                .map(ScheduleResponse::from)
                .toList();
    }
}
