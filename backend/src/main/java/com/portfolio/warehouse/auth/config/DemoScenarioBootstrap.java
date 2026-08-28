package com.portfolio.warehouse.auth.config;

import com.portfolio.warehouse.auth.domain.*;
import com.portfolio.warehouse.auth.repository.UserAccountRepository;
import com.portfolio.warehouse.auth.service.EmployeeNumberService;
import com.portfolio.warehouse.issue.domain.*;
import com.portfolio.warehouse.issue.repository.*;
import com.portfolio.warehouse.location.domain.*;
import com.portfolio.warehouse.location.repository.LocationRepository;
import com.portfolio.warehouse.mate.domain.*;
import com.portfolio.warehouse.mate.repository.*;
import com.portfolio.warehouse.pda.domain.PdaDevice;
import com.portfolio.warehouse.pda.repository.PdaDeviceRepository;
import com.portfolio.warehouse.work.domain.*;
import com.portfolio.warehouse.work.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(3)
public class DemoScenarioBootstrap
    implements ApplicationRunner {

    private final UserAccountRepository accountRepository;
    private final EmployeeNumberService employeeNumberService;
    private final PasswordEncoder passwordEncoder;
    private final MateRepository mateRepository;
    private final MateWorkScheduleRepository scheduleRepository;
    private final MateStatusHistoryRepository statusHistoryRepository;
    private final PdaDeviceRepository pdaRepository;
    private final LocationRepository locationRepository;
    private final WorkTypeRepository workTypeRepository;
    private final IssueTypeRepository issueTypeRepository;
    private final WorkAssignmentRepository assignmentRepository;
    private final WorkAssignmentHistoryRepository assignmentHistoryRepository;
    private final WorkProgressRepository progressRepository;
    private final SpecialIssueRepository issueRepository;
    private final SpecialIssueHistoryRepository issueHistoryRepository;

    @Value("${warehouse.demo-scenario.enabled:false}")
    private boolean enabled;

    @Value("${warehouse.demo-scenario.mate-password:mate1234}")
    private String matePassword;

    public DemoScenarioBootstrap(
        UserAccountRepository accountRepository,
        EmployeeNumberService employeeNumberService,
        PasswordEncoder passwordEncoder,
        MateRepository mateRepository,
        MateWorkScheduleRepository scheduleRepository,
        MateStatusHistoryRepository statusHistoryRepository,
        PdaDeviceRepository pdaRepository,
        LocationRepository locationRepository,
        WorkTypeRepository workTypeRepository,
        IssueTypeRepository issueTypeRepository,
        WorkAssignmentRepository assignmentRepository,
        WorkAssignmentHistoryRepository assignmentHistoryRepository,
        WorkProgressRepository progressRepository,
        SpecialIssueRepository issueRepository,
        SpecialIssueHistoryRepository issueHistoryRepository
    ) {
        this.accountRepository = accountRepository;
        this.employeeNumberService = employeeNumberService;
        this.passwordEncoder = passwordEncoder;
        this.mateRepository = mateRepository;
        this.scheduleRepository = scheduleRepository;
        this.statusHistoryRepository =
            statusHistoryRepository;
        this.pdaRepository = pdaRepository;
        this.locationRepository = locationRepository;
        this.workTypeRepository = workTypeRepository;
        this.issueTypeRepository = issueTypeRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentHistoryRepository =
            assignmentHistoryRepository;
        this.progressRepository = progressRepository;
        this.issueRepository = issueRepository;
        this.issueHistoryRepository =
            issueHistoryRepository;
    }

    @Override
    @Transactional
    public void run(
        ApplicationArguments args
    ) {
        if (!enabled) {
            return;
        }

        // 기존 사용자 데이터가 있는 DB에는 Demo 인력을 섞지 않는다.
        // 포트폴리오 시연용 빈 DB에서만 한 번 생성한다.
        if (mateRepository.count() > 0) {
            return;
        }

        UserAccount admin =
            accountRepository
                .findFirstByRole(UserRole.ADMIN)
                .orElseThrow(() ->
                    new IllegalStateException(
                        "Demo admin bootstrap must run first."
                    )
                );

        Location a01 =
            location(
                null,
                "A01",
                "A01",
                1,
                1,
                NonFoodCategory.GENERAL
            );

        Location a0101 =
            location(
                a01,
                "01",
                "A01-01",
                2,
                1,
                NonFoodCategory.GENERAL
            );

        Location a0102 =
            location(
                a01,
                "02",
                "A01-02",
                2,
                1,
                NonFoodCategory.GENERAL
            );

        Location a0103 =
            location(
                a01,
                "03",
                "A01-03",
                2,
                1,
                NonFoodCategory.GENERAL
            );

        Location b01 =
            location(
                null,
                "B01",
                "B01",
                1,
                1,
                NonFoodCategory.HYGIENE
            );

        Location b0101 =
            location(
                b01,
                "01",
                "B01-01",
                2,
                1,
                NonFoodCategory.HYGIENE
            );

        Location b0102 =
            location(
                b01,
                "02",
                "B01-02",
                2,
                1,
                NonFoodCategory.HYGIENE
            );

        WorkType inventory =
            workType(
                "재고조사",
                "로케이션별 실물 재고 확인"
            );

        WorkType replenish =
            workType(
                "진열보충",
                "지정 구역 상품 보충 및 위치 확인"
            );

        IssueType relocateIssue =
            issueType(
                "재배치 확인",
                true,
                false,
                false
            );

        IssueType stockIssue =
            issueType(
                "재고 불일치",
                true,
                true,
                true
            );

        Mate aMate =
            mate(
                "김현장",
                "A구역"
            );

        Mate bMate =
            mate(
                "이검수",
                "B구역"
            );

        Mate supportMate =
            mate(
                "박지원",
                "지원"
            );

        seedDaySchedule(aMate);
        seedDaySchedule(bMate);
        seedDaySchedule(supportMate);

        pda(31);
        pda(32);
        pda(33);

        WorkAssignment first =
            assignmentRepository.save(
                new WorkAssignment(
                    inventory,
                    a01,
                    a0101,
                    aMate,
                    admin
                )
            );

        first.start();
        first.updateLastCompletedLocation(
            a0102
        );

        assignmentHistoryRepository.save(
            new WorkAssignmentHistory(
                first,
                WorkAssignmentActionType.ASSIGN,
                null,
                aMate,
                admin,
                "Demo 시나리오 초기 배정"
            )
        );

        progressRepository.save(
            new WorkProgress(
                first,
                aMate,
                admin,
                a0102,
                null,
                false,
                "Demo 마지막 수행위치"
            )
        );

        WorkAssignment second =
            assignmentRepository.save(
                new WorkAssignment(
                    replenish,
                    b01,
                    b0101,
                    bMate,
                    admin
                )
            );

        assignmentHistoryRepository.save(
            new WorkAssignmentHistory(
                second,
                WorkAssignmentActionType.ASSIGN,
                null,
                bMate,
                admin,
                "Demo 시나리오 초기 배정"
            )
        );

        SpecialIssue firstIssue =
            issueRepository.save(
                new SpecialIssue(
                    relocateIssue,
                    aMate,
                    null,
                    first,
                    a0103,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    "A01-03 상품 위치 재확인 필요"
                )
            );

        issueHistoryRepository.save(
            new SpecialIssueHistory(
                firstIssue,
                SpecialIssueHistoryAction.CREATE,
                null,
                null,
                admin,
                "Demo 특이사항"
            )
        );

        SpecialIssue secondIssue =
            issueRepository.save(
                new SpecialIssue(
                    stockIssue,
                    bMate,
                    supportMate,
                    second,
                    b0102,
                    "DEMO-1001",
                    3,
                    7,
                    10,
                    0,
                    false,
                    "실물수량과 MMS 수량 차이 확인"
                )
            );

        secondIssue.confirm();

        issueHistoryRepository.save(
            new SpecialIssueHistory(
                secondIssue,
                SpecialIssueHistoryAction.CREATE,
                null,
                supportMate,
                admin,
                "Demo 특이사항"
            )
        );

        issueHistoryRepository.save(
            new SpecialIssueHistory(
                secondIssue,
                SpecialIssueHistoryAction.CONFIRM,
                supportMate,
                supportMate,
                admin,
                "Demo 확인 처리"
            )
        );
    }

    private Mate mate(
        String name,
        String nickname
    ) {
        String employeeNo =
            employeeNumberService.issue(
                UserRole.MATE
            );

        UserAccount account =
            accountRepository.save(
                new UserAccount(
                    employeeNo,
                    passwordEncoder.encode(
                        matePassword
                    ),
                    UserRole.MATE
                )
            );

        Mate mate =
            new Mate(
                account,
                employeeNo,
                name,
                nickname,
                LocalDate.now()
            );

        mate.changeStatus(
            MateStatus.AVAILABLE,
            "대기"
        );

        Mate saved =
            mateRepository.save(mate);

        statusHistoryRepository.save(
            new MateStatusHistory(
                saved,
                MateStatus.AVAILABLE,
                "대기"
            )
        );

        return saved;
    }

    private void seedDaySchedule(
        Mate mate
    ) {
        List<MateWorkSchedule> rows =
            Arrays.stream(DayOfWeek.values())
                .filter(day ->
                    day != DayOfWeek.SATURDAY
                        && day != DayOfWeek.SUNDAY
                )
                .map(day ->
                    new MateWorkSchedule(
                        mate,
                        day,
                        ScheduleType.WEEKDAY,
                        ShiftType.DAY,
                        LocalTime.of(8, 0),
                        LocalTime.of(18, 0)
                    )
                )
                .toList();

        scheduleRepository.saveAll(rows);
    }

    private PdaDevice pda(
        int number
    ) {
        return pdaRepository
            .findByDeviceNumber(number)
            .orElseGet(() ->
                pdaRepository.save(
                    new PdaDevice(number)
                )
            );
    }

    private WorkType workType(
        String name,
        String description
    ) {
        return workTypeRepository
            .findByName(name)
            .orElseGet(() ->
                workTypeRepository.save(
                    new WorkType(
                        name,
                        description
                    )
                )
            );
    }

    private IssueType issueType(
        String name,
        boolean requireLocation,
        boolean requireProductCode,
        boolean requireQuantity
    ) {
        return issueTypeRepository
            .findByName(name)
            .orElseGet(() ->
                issueTypeRepository.save(
                    new IssueType(
                        name,
                        requireLocation,
                        requireProductCode,
                        requireQuantity
                    )
                )
            );
    }

    private Location location(
        Location parent,
        String segment,
        String fullCode,
        int depth,
        Integer floor,
        NonFoodCategory category
    ) {
        return locationRepository
            .findByFullCode(fullCode)
            .orElseGet(() -> {
                Location created =
                    new Location(
                        parent,
                        segment,
                        fullCode,
                        depth
                    );

                created.updateMetadata(
                    floor,
                    LocationFoodType.NON_FOOD,
                    Set.of(category)
                );

                return locationRepository.save(
                    created
                );
            });
    }
}
