package com.portfolio.warehouse.mate.service;

import com.portfolio.warehouse.auth.domain.UserAccount;
import com.portfolio.warehouse.auth.domain.UserRole;
import com.portfolio.warehouse.auth.repository.UserAccountRepository;
import com.portfolio.warehouse.auth.service.EmployeeNumberService;
import com.portfolio.warehouse.common.exception.NotFoundException;
import com.portfolio.warehouse.log.domain.ActivityType;
import com.portfolio.warehouse.log.service.BusinessAuditService;
import com.portfolio.warehouse.mate.api.dto.MateCreateRequest;
import com.portfolio.warehouse.mate.api.dto.MateResponse;
import com.portfolio.warehouse.mate.domain.Mate;
import com.portfolio.warehouse.mate.repository.MateRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MateAdminService {

    private final MateRepository mateRepository;
    private final UserAccountRepository accountRepository;
    private final EmployeeNumberService employeeNumberService;
    private final PasswordEncoder passwordEncoder;
    private final BusinessAuditService auditService;

    public MateAdminService(
        MateRepository mateRepository,
        UserAccountRepository accountRepository,
        EmployeeNumberService employeeNumberService,
        PasswordEncoder passwordEncoder,
        BusinessAuditService auditService
    ) {
        this.mateRepository = mateRepository;
        this.accountRepository = accountRepository;
        this.employeeNumberService = employeeNumberService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public MateResponse create(MateCreateRequest request) {
        String employeeNo =
            employeeNumberService.issue(UserRole.MATE);

        UserAccount account = new UserAccount(
            employeeNo,
            passwordEncoder.encode(request.password()),
            UserRole.MATE
        );
        accountRepository.save(account);

        Mate mate = new Mate(
            account,
            employeeNo,
            request.name().trim(),
            request.nickname().trim(),
            request.joinedAt() == null
                ? LocalDate.now()
                : request.joinedAt()
        );

        Mate saved = mateRepository.save(mate);

        auditService.record(
            ActivityType.MATE_CREATE,
            saved.getNickname(),
            "MATE 등록 · "
                + saved.getEmployeeNo()
                + " / "
                + saved.getName(),
            "MATE",
            saved.getId()
        );

        return MateResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<MateResponse> findAll() {
        return mateRepository.findAll().stream()
            .map(MateResponse::from)
            .toList();
    }

    @Transactional
    public MateResponse changeNickname(
        Long mateId,
        String nickname
    ) {
        Mate mate = findMate(mateId);
        String before = mate.getNickname();
        String after = nickname.trim();

        if (before.equals(after)) {
            return MateResponse.from(mate);
        }

        mate.changeNickname(after);

        auditService.record(
            ActivityType.MATE_NICKNAME_CHANGE,
            mate.getEmployeeNo(),
            "운영 별명 변경 · "
                + before
                + " → "
                + after,
            "MATE",
            mate.getId()
        );

        return MateResponse.from(mate);
    }

    @Transactional
    public MateResponse deactivate(Long mateId) {
        Mate mate = findMate(mateId);

        if (!mate.isActive()) {
            return MateResponse.from(mate);
        }

        mate.deactivate();
        mate.getAccount().disable();

        auditService.record(
            ActivityType.MATE_ACTIVE_CHANGE,
            mate.getEmployeeNo(),
            "MATE 비활성 처리",
            "MATE",
            mate.getId()
        );

        return MateResponse.from(mate);
    }

    @Transactional
    public MateResponse reactivate(Long mateId) {
        Mate mate = findMate(mateId);

        if (mate.isActive()) {
            return MateResponse.from(mate);
        }

        mate.reactivate();
        mate.getAccount().enable();

        auditService.record(
            ActivityType.MATE_ACTIVE_CHANGE,
            mate.getEmployeeNo(),
            "MATE 재활성 처리",
            "MATE",
            mate.getId()
        );

        return MateResponse.from(mate);
    }

    private Mate findMate(Long mateId) {
        return mateRepository.findById(mateId)
            .orElseThrow(() -> new NotFoundException(
                "MATE_NOT_FOUND",
                "MATE를 찾을 수 없습니다."
            ));
    }
}
