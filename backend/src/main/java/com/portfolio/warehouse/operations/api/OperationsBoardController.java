package com.portfolio.warehouse.operations.api;

import com.portfolio.warehouse.operations.api.dto.OperationsBoardResponse;
import com.portfolio.warehouse.operations.service.OperationsBoardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/operations")
public class OperationsBoardController {

    private final OperationsBoardService service;

    public OperationsBoardController(
        OperationsBoardService service
    ) {
        this.service = service;
    }

    @GetMapping
    public OperationsBoardResponse board() {
        return service.board();
    }
}
