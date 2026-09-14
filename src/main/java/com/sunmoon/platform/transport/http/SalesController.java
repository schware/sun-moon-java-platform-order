package com.sunmoon.platform.transport.http;

import com.sunmoon.platform.domain.order.Order;
import com.sunmoon.platform.domain.sales.SalesService;
import com.sunmoon.platform.domain.sales.SalesSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 일자별 매출 조회. Read-only, and read by BO — the 매출 자체는 여기서
 * 쌓이고 BO는 보여주기만 합니다.
 *
 * <p>Separate from {@link OrderController} because the questions are
 * different: that one is a terminal asking what it can act on right now,
 * this one is an operator asking what a day came to. Same table, opposite
 * access pattern.
 */
@RestController
@RequestMapping("/sales")
@Tag(name = "Sales", description = "일자별 매출 조회")
public class SalesController {

    private final SalesService service;

    public SalesController(SalesService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "일자별 매출 집계",
            description = "영업일자 × 매장으로 묶어 건수와 금액을 돌려줍니다. from/to를 비우면 "
                    + "오늘까지의 최근 2주입니다. 금액은 수락된 주문만 — 거절/미응답은 건수로만 보입니다.")
    public List<SalesSummary> summarize(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String storeId) {
        return service.summarize(from, to, storeId);
    }

    @GetMapping("/orders")
    @Operation(summary = "그 날의 주문 목록", description = "집계 한 줄을 눌렀을 때 보이는 내역입니다.")
    public List<Order> detail(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestParam(required = false) String storeId) {
        return service.detail(businessDate, storeId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> onBadRange(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }
}
