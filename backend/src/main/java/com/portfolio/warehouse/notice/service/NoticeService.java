package com.portfolio.warehouse.notice.service;

import com.portfolio.warehouse.auth.service.CurrentUserService;
import com.portfolio.warehouse.common.exception.NotFoundException;
import com.portfolio.warehouse.common.event.OperationalEventService;
import com.portfolio.warehouse.log.domain.ActivityType;
import com.portfolio.warehouse.notice.api.dto.*;
import com.portfolio.warehouse.notice.domain.Notice;
import com.portfolio.warehouse.notice.repository.NoticeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoticeService {

    private final NoticeRepository repository;
    private final CurrentUserService currentUserService;
    private final OperationalEventService eventService;

    public NoticeService(
        NoticeRepository repository,
        CurrentUserService currentUserService,
        OperationalEventService eventService
    ) {
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.eventService = eventService;
    }

    @Transactional
    public NoticeResponse create(NoticeRequest request) {
        int order = repository.findAllByDeletedAtIsNullOrderByImportantDescDisplayOrderAscUpdatedAtDesc().size();

        Notice notice = repository.save(
            new Notice(
                request.content().trim(),
                request.visible(),
                request.important(),
                order,
                currentUserService.account()
            )
        );

        eventService.publish(
            ActivityType.NOTICE_CHANGE,
            currentUserService.account(),
            "전체",
            "공지사항 등록",
            "NOTICE",
            notice.getId(),
            true,
            true
        );

        return NoticeResponse.from(notice);
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> adminList() {
        return repository.findAllByDeletedAtIsNullOrderByImportantDescDisplayOrderAscUpdatedAtDesc()
            .stream()
            .map(NoticeResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> visibleList() {
        return repository.findAllByDeletedAtIsNullAndVisibleTrueOrderByImportantDescDisplayOrderAscUpdatedAtDesc()
            .stream()
            .map(NoticeResponse::from)
            .toList();
    }

    @Transactional
    public NoticeResponse update(Long id, NoticeRequest request) {
        Notice notice = find(id);
        notice.update(request.content().trim(), request.visible(), request.important());

        eventService.publish(
            ActivityType.NOTICE_CHANGE,
            currentUserService.account(),
            "전체",
            "공지사항 수정",
            "NOTICE",
            notice.getId(),
            true,
            true
        );

        return NoticeResponse.from(notice);
    }

    @Transactional
    public void reorder(List<NoticeOrderItem> items) {
        for (NoticeOrderItem item : items) {
            find(item.id()).changeOrder(item.displayOrder());
        }
    }

    @Transactional
    public void delete(Long id) {
        find(id).delete();
    }

    @Transactional
    public void deleteAll() {
        repository.findAllByDeletedAtIsNullOrderByImportantDescDisplayOrderAscUpdatedAtDesc()
            .forEach(Notice::delete);
    }

    private Notice find(Long id) {
        Notice notice = repository.findById(id)
            .orElseThrow(() -> new NotFoundException("NOTICE_NOT_FOUND", "공지사항을 찾을 수 없습니다."));

        if (notice.getDeletedAt() != null) {
            throw new NotFoundException("NOTICE_NOT_FOUND", "삭제된 공지사항입니다.");
        }

        return notice;
    }
}
