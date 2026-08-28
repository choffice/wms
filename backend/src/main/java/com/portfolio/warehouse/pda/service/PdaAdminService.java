package com.portfolio.warehouse.pda.service;

import com.portfolio.warehouse.common.exception.BusinessException;
import com.portfolio.warehouse.common.exception.NotFoundException;
import com.portfolio.warehouse.log.domain.ActivityType;
import com.portfolio.warehouse.log.service.BusinessAuditService;
import com.portfolio.warehouse.pda.api.dto.PdaResponse;
import com.portfolio.warehouse.pda.api.dto.PdaUsageResponse;
import com.portfolio.warehouse.pda.domain.*;
import com.portfolio.warehouse.pda.repository.*;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PdaAdminService {

    private final PdaDeviceRepository deviceRepository;
    private final PdaUsageHistoryRepository usageRepository;
    private final BusinessAuditService auditService;

    public PdaAdminService(
        PdaDeviceRepository deviceRepository,
        PdaUsageHistoryRepository usageRepository,
        BusinessAuditService auditService
    ) {
        this.deviceRepository = deviceRepository;
        this.usageRepository = usageRepository;
        this.auditService = auditService;
    }

    @Transactional
    public PdaResponse create(Integer deviceNumber) {
        requirePositiveNumber(deviceNumber);

        if (deviceRepository.existsByDeviceNumber(deviceNumber)) {
            throw new BusinessException(
                "PDA_NUMBER_DUPLICATED",
                "이미 등록된 PDA 번호입니다."
            );
        }

        PdaDevice device =
            deviceRepository.save(
                new PdaDevice(deviceNumber)
            );

        auditService.record(
            ActivityType.PDA_CREATE,
            "PDA " + deviceNumber,
            "PDA 기기 등록",
            "PDA_DEVICE",
            device.getId()
        );

        return PdaResponse.from(device);
    }

    @Transactional(readOnly = true)
    public List<PdaResponse> findAll() {
        return deviceRepository.findAll().stream()
            .sorted(
                Comparator.comparing(
                    (PdaDevice device) ->
                        device.getDeviceNumber()
                )
            )
            .map(PdaResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PdaUsageResponse> usageHistory(
        Long deviceId
    ) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new NotFoundException(
                "PDA_NOT_FOUND",
                "PDA 기기를 찾을 수 없습니다."
            );
        }

        return usageRepository
            .findAllByPdaDeviceIdOrderByAssignedAtDesc(
                deviceId
            )
            .stream()
            .map(PdaUsageResponse::from)
            .toList();
    }

    @Transactional
    public PdaResponse changeNumber(
        Long deviceId,
        Integer newNumber
    ) {
        requirePositiveNumber(newNumber);

        PdaDevice device = findForUpdate(deviceId);
        int before = device.getDeviceNumber();

        if (before == newNumber) {
            return PdaResponse.from(device);
        }

        if (deviceRepository.existsByDeviceNumber(newNumber)) {
            throw new BusinessException(
                "PDA_NUMBER_DUPLICATED",
                "이미 사용 중인 PDA 번호입니다. 두 기기의 번호를 교체하려면 번호 맞교환 기능을 사용해주세요."
            );
        }

        device.changeDeviceNumber(newNumber);

        auditService.record(
            ActivityType.PDA_NUMBER_CHANGE,
            "PDA #" + device.getId(),
            "표시번호 변경 · "
                + before
                + " → "
                + newNumber,
            "PDA_DEVICE",
            device.getId()
        );

        return PdaResponse.from(device);
    }

    @Transactional
    public PdaResponse changeStatus(
        Long deviceId,
        PdaStatus status
    ) {
        PdaDevice device = findForUpdate(deviceId);
        PdaStatus before = device.getStatus();

        if (before == status) {
            return PdaResponse.from(device);
        }

        if (
            usageRepository
                .findFirstByPdaDeviceIdAndReleasedAtIsNull(
                    deviceId
                )
                .isPresent()
                && status != PdaStatus.IN_USE
                && status != PdaStatus.LOST
        ) {
            throw new BusinessException(
                "PDA_CURRENTLY_IN_USE",
                "현재 사용 중인 PDA는 LOST 표시를 제외하면 반납 처리 후 상태를 변경해주세요."
            );
        }

        device.changeStatus(status);

        auditService.record(
            ActivityType.PDA_STATUS_CHANGE,
            "PDA " + device.getDeviceNumber(),
            "상태 변경 · "
                + before
                + " → "
                + status,
            "PDA_DEVICE",
            device.getId()
        );

        return PdaResponse.from(device);
    }

    @Transactional
    public void deleteOrRetire(Long deviceId) {
        PdaDevice device = findForUpdate(deviceId);

        if (
            usageRepository
                .findFirstByPdaDeviceIdAndReleasedAtIsNull(
                    deviceId
                )
                .isPresent()
        ) {
            throw new BusinessException(
                "PDA_CURRENTLY_IN_USE",
                "현재 사용 중인 PDA는 삭제할 수 없습니다."
            );
        }

        int number = device.getDeviceNumber();

        if (usageRepository.existsByPdaDeviceId(deviceId)) {
            device.retire();

            auditService.record(
                ActivityType.PDA_RETIRE,
                "PDA " + number,
                "사용이력이 존재하여 삭제 대신 RETIRED 처리",
                "PDA_DEVICE",
                device.getId()
            );
            return;
        }

        auditService.record(
            ActivityType.PDA_DELETE,
            "PDA " + number,
            "사용이력 없는 PDA 물리 삭제",
            "PDA_DEVICE",
            device.getId()
        );

        deviceRepository.delete(device);
    }

    @Transactional
    public List<PdaResponse> swapNumbers(
        Long firstId,
        Long secondId
    ) {
        if (firstId.equals(secondId)) {
            throw new BusinessException(
                "PDA_SWAP_SAME_DEVICE",
                "서로 다른 두 기기를 선택해주세요."
            );
        }

        long lowerId = Math.min(firstId, secondId);
        long higherId = Math.max(firstId, secondId);

        PdaDevice lower = findForUpdate(lowerId);
        PdaDevice higher = findForUpdate(higherId);

        PdaDevice first =
            lower.getId().equals(firstId)
                ? lower
                : higher;

        PdaDevice second =
            lower.getId().equals(secondId)
                ? lower
                : higher;

        int firstNumber = first.getDeviceNumber();
        int secondNumber = second.getDeviceNumber();

        int temporaryNumber =
            temporaryNumber(
                first.getId(),
                second.getId()
            );

        while (
            deviceRepository
                .existsByDeviceNumber(temporaryNumber)
        ) {
            temporaryNumber--;
        }

        first.changeDeviceNumber(temporaryNumber);
        deviceRepository.flush();

        second.changeDeviceNumber(firstNumber);
        deviceRepository.flush();

        first.changeDeviceNumber(secondNumber);
        deviceRepository.flush();

        auditService.record(
            ActivityType.PDA_NUMBER_SWAP,
            "PDA 번호 맞교환",
            "#"
                + first.getId()
                + " "
                + firstNumber
                + " → "
                + secondNumber
                + " / #"
                + second.getId()
                + " "
                + secondNumber
                + " → "
                + firstNumber,
            "PDA_DEVICE",
            first.getId()
        );

        return List.of(
            PdaResponse.from(first),
            PdaResponse.from(second)
        );
    }

    private int temporaryNumber(Long a, Long b) {
        long raw =
            -1_000_000_000L - a - b;

        if (raw < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE + 10_000;
        }

        return (int) raw;
    }

    private void requirePositiveNumber(
        Integer deviceNumber
    ) {
        if (
            deviceNumber == null
                || deviceNumber < 1
        ) {
            throw new BusinessException(
                "INVALID_PDA_NUMBER",
                "PDA 번호는 1 이상이어야 합니다."
            );
        }
    }

    private PdaDevice findForUpdate(Long deviceId) {
        return deviceRepository
            .findByIdForUpdate(deviceId)
            .orElseThrow(() -> new NotFoundException(
                "PDA_NOT_FOUND",
                "PDA 기기를 찾을 수 없습니다."
            ));
    }
}
