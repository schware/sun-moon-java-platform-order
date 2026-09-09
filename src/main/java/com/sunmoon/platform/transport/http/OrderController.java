package com.sunmoon.platform.transport.http;

import com.sunmoon.platform.domain.order.ChangeStatusRequest;
import com.sunmoon.platform.domain.order.CreateOrderRequest;
import com.sunmoon.platform.domain.order.IllegalOrderTransitionException;
import com.sunmoon.platform.domain.order.Order;
import com.sunmoon.platform.domain.order.OrderService;
import com.sunmoon.platform.domain.order.OrderStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "주문 접수와 상태 전이")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "주문 생성", description = "Places an order in PLACED. This is what the order channel calls.")
    @ApiResponse(responseCode = "201", description = "Placed")
    @ApiResponse(responseCode = "400", description = "Validation failed (blank customerId, non-positive amount)")
    public ResponseEntity<Order> create(@Valid @RequestBody CreateOrderRequest request) {
        Order placed = orderService.place(request.storeId(), request.customerId(), request.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body(placed);
    }

    @GetMapping
    @Operation(summary = "주문 목록",
            description = "Optionally filtered by status — this is how a POS terminal asks what it can accept.")
    public List<Order> list(@RequestParam(required = false) OrderStatus status) {
        return orderService.list(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "주문 단건 조회")
    @ApiResponse(responseCode = "404", description = "No such order")
    public ResponseEntity<?> read(@PathVariable long id) {
        return orderService.read(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("No such order: " + id)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "상태 전이",
            description = "The only way an order changes state. Illegal moves are refused with 409 rather than "
                    + "silently applied — PLACED→ACCEPTED→PRODUCED→DELIVERING→COMPLETED, or PLACED→REJECTED.")
    @ApiResponse(responseCode = "200", description = "Moved")
    @ApiResponse(responseCode = "404", description = "No such order")
    @ApiResponse(responseCode = "409", description = "That move is not allowed from the current status")
    public Order changeStatus(@PathVariable long id, @Valid @RequestBody ChangeStatusRequest request) {
        return orderService.moveTo(id, request.status(), request.deviceId());
    }

    @ExceptionHandler(IllegalOrderTransitionException.class)
    ResponseEntity<Map<String, String>> onIllegalTransition(IllegalOrderTransitionException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error(e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Map<String, String>> onMissing(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
    }

    private static Map<String, String> error(String message) {
        return Map.of("error", message);
    }
}
