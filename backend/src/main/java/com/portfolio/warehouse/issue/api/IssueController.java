package com.portfolio.warehouse.issue.api;

import com.portfolio.warehouse.issue.api.dto.*;
import com.portfolio.warehouse.issue.service.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class IssueController {

    private final IssueTypeService typeService;
    private final SpecialIssueService issueService;
    private final IssueBulkActionService bulkActionService;

    public IssueController(
        IssueTypeService typeService,
        SpecialIssueService issueService,
        IssueBulkActionService bulkActionService
    ) {
        this.typeService = typeService;
        this.issueService = issueService;
        this.bulkActionService = bulkActionService;
    }

    @PostMapping("/api/admin/issue-types")
    @ResponseStatus(HttpStatus.CREATED)
    public IssueTypeResponse createType(
        @Valid @RequestBody IssueTypeRequest request
    ) {
        return typeService.create(request);
    }

    @GetMapping("/api/admin/issue-types")
    public List<IssueTypeResponse> types() {
        return typeService.findAll();
    }

    @PutMapping("/api/admin/issue-types/{id}")
    public IssueTypeResponse updateType(
        @PathVariable Long id,
        @Valid @RequestBody IssueTypeRequest request
    ) {
        return typeService.update(id, request);
    }

    @PostMapping("/api/admin/issue-types/{id}/deactivate")
    public IssueTypeResponse deactivateType(@PathVariable Long id) {
        return typeService.deactivate(id);
    }

    @PostMapping("/api/mate/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public SpecialIssueResponse createIssue(
        @Valid @RequestBody SpecialIssueCreateRequest request
    ) {
        return issueService.create(request);
    }

    @GetMapping("/api/admin/issues/main")
    public List<SpecialIssueResponse> mainIssues() {
        return issueService.mainUnconfirmed();
    }

    @GetMapping("/api/admin/issues/board")
    public List<SpecialIssueResponse> board() {
        return issueService.board();
    }


    @PostMapping("/api/admin/issues/bulk-confirm")
    public BulkIssueActionResult bulkConfirm(
        @Valid @RequestBody
        BulkIssueStatusRequest request
    ) {
        return bulkActionService.bulkConfirm(request);
    }

    @PostMapping("/api/admin/issues/bulk-resolve")
    public BulkIssueActionResult bulkResolve(
        @Valid @RequestBody
        BulkIssueStatusRequest request
    ) {
        return bulkActionService.bulkResolve(request);
    }

    @PostMapping("/api/admin/issues/bulk-responsible")
    public BulkIssueActionResult bulkResponsible(
        @Valid @RequestBody
        BulkIssueResponsibleRequest request
    ) {
        return bulkActionService
            .bulkAssignResponsible(request);
    }

    @GetMapping("/api/admin/issues/{id}")
    public SpecialIssueResponse detail(@PathVariable Long id) {
        return issueService.detail(id);
    }

    @PatchMapping("/api/admin/issues/{id}/responsible")
    public SpecialIssueResponse assignResponsible(
        @PathVariable Long id,
        @Valid @RequestBody SpecialIssueResponsibleRequest request
    ) {
        return issueService.assignResponsible(id, request);
    }

    @GetMapping("/api/admin/issues/{id}/history")
    public List<SpecialIssueHistoryResponse> history(
        @PathVariable Long id
    ) {
        return issueService.history(id);
    }


    @PostMapping("/api/admin/issues/{id}/confirm")
    public SpecialIssueResponse confirm(@PathVariable Long id) {
        return issueService.confirm(id);
    }

    @PostMapping("/api/admin/issues/{id}/resolve")
    public SpecialIssueResponse resolve(@PathVariable Long id) {
        return issueService.resolve(id);
    }

    @DeleteMapping("/api/admin/issues/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        issueService.delete(id);
    }
}
