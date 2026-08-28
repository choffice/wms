package com.portfolio.warehouse.notice.api;

import com.portfolio.warehouse.notice.api.dto.*;
import com.portfolio.warehouse.notice.service.NoticeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class NoticeController {

    private final NoticeService service;

    public NoticeController(NoticeService service) {
        this.service = service;
    }

    @GetMapping("/api/mate/notices")
    public List<NoticeResponse> visibleForMate() {
        return service.visibleList();
    }

    @GetMapping("/api/admin/notices")
    public List<NoticeResponse> adminList() {
        return service.adminList();
    }

    @PostMapping("/api/admin/notices")
    @ResponseStatus(HttpStatus.CREATED)
    public NoticeResponse create(@Valid @RequestBody NoticeRequest request) {
        return service.create(request);
    }

    @PutMapping("/api/admin/notices/{id}")
    public NoticeResponse update(
        @PathVariable Long id,
        @Valid @RequestBody NoticeRequest request
    ) {
        return service.update(id, request);
    }

    @PutMapping("/api/admin/notices/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@Valid @RequestBody List<NoticeOrderItem> items) {
        service.reorder(items);
    }

    @DeleteMapping("/api/admin/notices/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @DeleteMapping("/api/admin/notices")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAll() {
        service.deleteAll();
    }
}
