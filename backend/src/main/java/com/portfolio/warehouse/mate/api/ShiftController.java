package com.portfolio.warehouse.mate.api;

import com.portfolio.warehouse.mate.service.ShiftService;
import com.portfolio.warehouse.mate.api.dto.TodayShiftResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mate/shift")
public class ShiftController {

    private final ShiftService service;

    public ShiftController(ShiftService service) {
        this.service = service;
    }

@GetMapping("/today")
public TodayShiftResponse today() {
    return service.today();
}

    @PostMapping("/end")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void endShift() {
        service.endShift();
    }
}
