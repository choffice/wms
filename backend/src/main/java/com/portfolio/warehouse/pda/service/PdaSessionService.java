package com.portfolio.warehouse.pda.service;

import com.portfolio.warehouse.common.exception.BusinessException;
import com.portfolio.warehouse.common.exception.NotFoundException;
import com.portfolio.warehouse.auth.service.CurrentUserService;
import com.portfolio.warehouse.common.event.OperationalEventService;
import com.portfolio.warehouse.log.domain.ActivityType;
import com.portfolio.warehouse.mate.domain.Mate;
import com.portfolio.warehouse.mate.domain.MateStatus;
import com.portfolio.warehouse.mate.repository.MateRepository;
import com.portfolio.warehouse.pda.api.dto.PdaUsageResponse;
import com.portfolio.warehouse.pda.domain.*;
import com.portfolio.warehouse.pda.repository.PdaDeviceRepository;
import com.portfolio.warehouse.pda.repository.PdaUsageHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PdaSessionService {

    private final PdaDeviceRepository deviceRepository;
    private final PdaUsageHistoryRepository usageRepository;
    private final MateRepository mateRepository;
    private final OperationalEventService eventService;
    private final CurrentUserService currentUserService;

    public PdaSessionService(
        PdaDeviceRepository deviceRepository,
        PdaUsageHistoryRepository usageRepository,
        MateRepository mateRepository,
        OperationalEventService eventService,
        CurrentUserService currentUserService
    ) {
        this.deviceRepository = deviceRepository;
        this.usageRepository = usageRepository;
        this.mateRepository = mateRepository;
        this.eventService = eventService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public PdaUsageResponse allocate(Integer deviceNumber, String employeeNo) {
        PdaDevice device = deviceRepository.findByDeviceNumberForUpdate(deviceNumber)
            .orElseThrow(() -> new NotFoundException("PDA_NOT_FOUND", "등록된 PDA 번호가 아닙니다."));

        Mate mate = mateRepository.findByEmployeeNoForUpdate(employeeNo)
            .orElseThrow(() -> new NotFoundException("MATE_NOT_FOUND", "MATE를 찾을 수 없습니다."));

        if (!mate.isActive() || !mate.getAccount().isEnabled()) {
            throw new BusinessException("MATE_INACTIVE", "비활성 상태의 MATE입니다.");
        }

        if (!device.isActive()
            || device.getStatus() == PdaStatus.LOST
            || device.getStatus() == PdaStatus.INSPECTION
            || device.getStatus() == PdaStatus.RETIRED) {
            throw new BusinessException("PDA_UNAVAILABLE", "현재 사용할 수 없는 PDA입니다.");
        }

        if (usageRepository.findFirstByPdaDeviceIdAndReleasedAtIsNull(device.getId()).isPresent()) {
            throw new BusinessException("PDA_ALREADY_ASSIGNED", "이미 다른 MATE가 사용 중인 PDA입니다.");
        }

        if (usageRepository.findFirstByMateIdAndReleasedAtIsNull(mate.getId()).isPresent()) {
            throw new BusinessException(
                "MATE_ALREADY_HAS_PDA",
                "현재 PDA를 반납한 뒤 다른 PDA를 선택해주세요."
            );
        }

        PdaUsageHistory usage = usageRepository.save(new PdaUsageHistory(device, mate));
        device.changeStatus(PdaStatus.IN_USE);

        if (mate.getCurrentStatus() == MateStatus.OFF_DUTY) {
            mate.changeStatus(MateStatus.AVAILABLE, "대기");
        }

        eventService.publish(
            ActivityType.PDA_ASSIGN,
            mate.getAccount(),
            "관리자",
            "PDA " + device.getDeviceNumber() + " 사용 시작",
            "PDA_USAGE",
            usage.getId(),
            true,
            false
        );

        return PdaUsageResponse.from(usage);
    }

    @Transactional
    public PdaUsageResponse release(Long usageId, PdaReleaseReason reason) {
        PdaUsageHistory usage = usageRepository.findById(usageId)
            .orElseThrow(() -> new NotFoundException(
                "PDA_USAGE_NOT_FOUND",
                "PDA 사용 이력을 찾을 수 없습니다."
            ));

        if (!usage.isActiveUsage()) {
            return PdaUsageResponse.from(usage);
        }

        PdaDevice device = deviceRepository.findByIdForUpdate(usage.getPdaDevice().getId())
            .orElseThrow(() -> new NotFoundException("PDA_NOT_FOUND", "PDA 기기를 찾을 수 없습니다."));

        PdaReleaseReason resolved = reason == null ? PdaReleaseReason.RETURNED : reason;
        usage.release(resolved);

        if (device.getStatus() == PdaStatus.IN_USE) {
            device.changeStatus(PdaStatus.AVAILABLE);
        }

        eventService.publish(
            ActivityType.PDA_RETURN,
            usage.getMate().getAccount(),
            "관리자",
            "PDA " + device.getDeviceNumber() + " 반납",
            "PDA_USAGE",
            usage.getId(),
            true,
            true
        );

        return PdaUsageResponse.from(usage);
    }

    @Transactional
    public PdaUsageResponse releaseByAdmin(
        Long usageId,
        PdaReleaseReason reason
    ) {
        PdaUsageHistory usage = usageRepository.findById(usageId)
            .orElseThrow(() -> new NotFoundException(
                "PDA_USAGE_NOT_FOUND",
                "PDA 사용 이력을 찾을 수 없습니다."
            ));

        if (!usage.isActiveUsage()) {
            return PdaUsageResponse.from(usage);
        }

        PdaDevice device = deviceRepository
            .findByIdForUpdate(
                usage.getPdaDevice().getId()
            )
            .orElseThrow(() -> new NotFoundException(
                "PDA_NOT_FOUND",
                "PDA 기기를 찾을 수 없습니다."
            ));

        PdaReleaseReason resolved =
            reason == null
                ? PdaReleaseReason.ADMIN_RELEASE
                : reason;

        usage.release(resolved);

        if (device.getStatus() == PdaStatus.IN_USE) {
            device.changeStatus(PdaStatus.AVAILABLE);
        }

        eventService.publish(
            ActivityType.PDA_RETURN,
            currentUserService.account(),
            usage.getMate().getNickname(),
            "관리자 PDA 회수 · PDA "
                + device.getDeviceNumber(),
            "PDA_USAGE",
            usage.getId(),
            true,
            true
        );

        return PdaUsageResponse.from(usage);
    }

    @Transactional(readOnly = true)
    public PdaUsageResponse currentUsageForMate(Long mateId) {
        return usageRepository.findFirstByMateIdAndReleasedAtIsNull(mateId)
            .map(PdaUsageResponse::from)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public PdaUsageHistory activeUsageForMate(Long mateId) {
        return usageRepository.findFirstByMateIdAndReleasedAtIsNull(mateId)
            .orElseThrow(() -> new BusinessException(
                "PDA_REQUIRED",
                "업무를 시작하려면 먼저 PDA에 로그인해야 합니다."
            ));
    }
}
