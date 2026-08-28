package com.portfolio.warehouse.shiftclose.api;

import com.portfolio.warehouse.shiftclose.api.dto.ShiftClosePreviewResponse;
import com.portfolio.warehouse.shiftclose.service.ShiftClosePreviewService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/shift-close")
public class ShiftCloseController {

    private final ShiftClosePreviewService service;

    public ShiftCloseController(
        ShiftClosePreviewService service
    ) {
        this.service = service;
    }

    @GetMapping("/preview")
    public ShiftClosePreviewResponse preview() {
        return service.preview();
    }
}
