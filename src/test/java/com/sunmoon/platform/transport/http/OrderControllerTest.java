package com.sunmoon.platform.transport.http;

import com.sunmoon.platform.domain.order.IllegalOrderTransitionException;
import com.sunmoon.platform.domain.order.Order;
import com.sunmoon.platform.domain.order.OrderService;
import com.sunmoon.platform.domain.order.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    private static final Instant WHEN = Instant.parse("2026-09-09T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    private static Order order(OrderStatus status, String acceptedBy) {
        return new Order(1L, "cust-1", new BigDecimal("42.50"), status, acceptedBy, WHEN, WHEN);
    }

    @Test
    void placesAnOrder() throws Exception {
        when(orderService.place(anyString(), any(BigDecimal.class)))
                .thenReturn(order(OrderStatus.PLACED, null));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"cust-1\",\"amount\":42.50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PLACED"));
    }

    @Test
    void rejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"\",\"amount\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsByStatusSoATerminalCanAskWhatItMayAccept() throws Exception {
        when(orderService.list(OrderStatus.PLACED)).thenReturn(List.of(order(OrderStatus.PLACED, null)));

        mockMvc.perform(get("/orders").param("status", "PLACED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PLACED"));
    }

    @Test
    void acceptingRecordsTheTerminal() throws Exception {
        when(orderService.moveTo(eq(1L), eq(OrderStatus.ACCEPTED), eq("pos-01")))
                .thenReturn(order(OrderStatus.ACCEPTED, "pos-01"));

        mockMvc.perform(put("/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\",\"deviceId\":\"pos-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedBy").value("pos-01"));
    }

    /** An illegal move is a conflict, not a silent success and not a 500. */
    @Test
    void answersAnIllegalTransitionWith409() throws Exception {
        when(orderService.moveTo(anyLong(), any(OrderStatus.class), nullable(String.class)))
                .thenThrow(new IllegalOrderTransitionException(1L, OrderStatus.PLACED, OrderStatus.COMPLETED));

        mockMvc.perform(put("/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void answersAMissingOrderWith404() throws Exception {
        when(orderService.moveTo(anyLong(), any(OrderStatus.class), nullable(String.class)))
                .thenThrow(new NoSuchElementException("No such order: 99"));

        mockMvc.perform(put("/orders/99/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isNotFound());
    }
}
