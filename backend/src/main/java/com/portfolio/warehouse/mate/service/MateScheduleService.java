package com.portfolio.warehouse.mate.service;

import com.portfolio.warehouse.common.exception.BusinessException;
import com.portfolio.warehouse.common.exception.NotFoundException;
import com.portfolio.warehouse.log.domain.ActivityType;
import com.portfolio.warehouse.log.service.BusinessAuditService;
import com.portfolio.warehouse.mate.api.dto.ScheduleItemRequest;
import com.portfolio.warehouse.mate.api.dto.ScheduleResponse;
import com.portfolio.warehouse.mate.domain.*;
import com.portfolio.warehouse.mate.repository.*;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MateScheduleService {

    private final MateRepository mateRepository;
    private final MateWorkScheduleRepository scheduleRepository;
    private final BusinessAuditService auditService;

    public MateScheduleService(
        MateRepository mateRepository,
        MateWorkScheduleRepository scheduleRepository,
        BusinessAuditService auditService
    ) {
        this.mateRepository = mateRepository;
        this.scheduleRepository = scheduleRepository;
        this.auditService = auditService;
    }

    @Transactional
    public List<ScheduleResponse> replace(
        Long mateId,
        List<ScheduleItemRequest> requests
    ) {
        Mate mate = mateRepository.findById(mateId)
            .orElseThrow(() -> new NotFoundException(
                "MATE_NOT_FOUND",
                "MATE를 찾을 수 없습니다."
            ));

        for (ScheduleItemRequest request : requests) {
            if (request.endTime().equals(request.startTime())) {
                throw new BusinessException(
                    "INVALID_WORK_TIME",
                    "근무 시작시간과 종료시간은 같을 수 없습니다."
                );
            }
        }

        String before =
            scheduleRepository
                .findAllByMateIdOrderByDayOfWeekAsc(mateId)
                .stream()
                .map(this::summary)
                .sorted()
                .collect(Collectors.joining(" | "));

        scheduleRepository.deleteAllByMateId(mateId);

        List<MateWorkSchedule> schedules =
            requests.stream()
                .map(request ->
                    new MateWorkSchedule(
                        mate,
                        request.dayOfWeek(),
                        request.scheduleType(),
                        request.shiftType(),
                        request.startTime(),
                        request.endTime()
                    )
                )
                .toList();

        List<MateWorkSchedule> saved =
            scheduleRepository.saveAll(schedules);

        String after =
            saved.stream()
                .map(this::summary)
                .sorted()
                .collect(Collectors.joining(" | "));

        if (!before.equals(after)) {
            auditService.record(
                ActivityType.MATE_SCHEDULE_CHANGE,
                mate.getNickname(),
                "기본 근무스케줄 변경 · "
                    + value(before)
                    + " → "
                    + value(after),
                "MATE_SCHEDULE",
                mate.getId()
            );
        }

        return saved.stream()
            .map(ScheduleResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> findAll(
        Long mateId
    ) {
        if (!mateRepository.existsById(mateId)) {
            throw new NotFoundException(
                "MATE_NOT_FOUND",
                "MATE를 찾을 수 없습니다."
            );
        }

        return scheduleRepository
            .findAllByMateIdOrderByDayOfWeekAsc(
                mateId
            )
            .stream()
            .map(ScheduleResponse::from)
            .toList();
    }

    private String summary(
        MateWorkSchedule schedule
    ) {
        boolean overnight =
            schedule.getEndTime()
                .isBefore(
                    schedule.getStartTime()
                );

        return schedule.getDayOfWeek()
            + ":"
            + schedule.getShiftType()
            + " "
            + schedule.getStartTime()
            + "~"
            + schedule.getEndTime()
            + (
                overnight
                    ? "(익일)"
                    : ""
            );
    }

    private String value(String text) {
        return text == null || text.isBlank()
            ? "없음"
            : text;
    }
}
