package com.sunmoon.platform.transport.http;

import com.sunmoon.platform.domain.businessday.BusinessDay;
import com.sunmoon.platform.domain.businessday.BusinessDayService;
import com.sunmoon.platform.domain.businessday.OpenCloseRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 개점 / 마감. Pressed on a POS terminal, which reaches this through the
 * Device Server; BO reads the same state to show which shops are trading.
 */
@RestController
@RequestMapping("/business-days")
@Tag(name = "Business days", description = "매장 개점/마감과 영업일자")
public class BusinessDayController {

    private final BusinessDayService service;

    public BusinessDayController(BusinessDayService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "영업 중인 매장 목록", description = "BO의 매장 영업 상태 화면이 읽는 것입니다.")
    public List<BusinessDay> open() {
        return service.allOpen();
    }

    @GetMapping("/{storeId}")
    @Operation(summary = "매장의 현재 영업 상태",
            description = "닫혀 있으면 open=false. needsClosing은 영업일자가 이미 지난 날짜라는 뜻입니다.")
    public Map<String, Object> status(@PathVariable String storeId) {
        LocalDate today = service.today();
        return service.current(storeId)
                .<Map<String, Object>>map(day -> Map.of(
                        "storeId", storeId,
                        "open", true,
                        "businessDate", day.businessDate().toString(),
                        "openedBy", day.openedBy(),
                        "needsClosing", day.isStale(today)))
                .orElseGet(() -> Map.of("storeId", storeId, "open", false, "needsClosing", false));
    }

    @PostMapping("/open")
    @Operation(summary = "개점",
            description = "영업일자가 지난 채로 열려 있었다면 그 날을 먼저 마감하고 오늘을 엽니다 — 응답의 "
                    + "rolled=true와 closedDate가 그 사실을 알려줍니다.")
    @ApiResponse(responseCode = "201", description = "Opened")
    @ApiResponse(responseCode = "409", description = "Already open on today's 영업일자")
    public ResponseEntity<?> openStore(@Valid @RequestBody OpenCloseRequest request) {
        BusinessDayService.OpenResult result = service.open(request.storeId(), request.deviceId());
        return switch (result.outcome()) {
            case OPENED -> ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "day", result.day(), "rolled", false));
            case ROLLED -> ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "day", result.day(), "rolled", true, "closedDate", result.closedDate().toString()));
            case ALREADY_OPEN -> ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Already open: " + request.storeId(),
                    "businessDate", result.day().businessDate().toString()));
        };
    }

    @PostMapping("/close")
    @Operation(summary = "마감")
    @ApiResponse(responseCode = "200", description = "Closed")
    @ApiResponse(responseCode = "409", description = "That store was not open")
    public ResponseEntity<?> closeStore(@Valid @RequestBody OpenCloseRequest request) {
        return service.close(request.storeId(), request.deviceId())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Not open: " + request.storeId())));
    }
}
