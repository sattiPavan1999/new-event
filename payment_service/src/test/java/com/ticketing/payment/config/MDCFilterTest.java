package com.ticketing.payment.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MDCFilterTest {

    private final MDCFilter mdcFilter = new MDCFilter();

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @Test
    void setsTraceIdInMdcDuringRequest() throws IOException, ServletException {
        when(request.getHeader("X-Trace-Id")).thenReturn(null);

        mdcFilter.doFilter(request, response,
                (req, res) -> assertNotNull(MDC.get("traceId")));
    }

    @Test
    void usesHeaderTraceIdWhenPresent() throws IOException, ServletException {
        when(request.getHeader("X-Trace-Id")).thenReturn("test-trace-id");

        mdcFilter.doFilter(request, response,
                (req, res) -> assertEquals("test-trace-id", MDC.get("traceId")));
    }

    @Test
    void generatesRandomTraceIdWhenHeaderAbsent() throws IOException, ServletException {
        when(request.getHeader("X-Trace-Id")).thenReturn(null);

        mdcFilter.doFilter(request, response, (req, res) -> {
            String traceId = MDC.get("traceId");
            assertNotNull(traceId);
            assertFalse(traceId.isEmpty());
        });
    }

    @Test
    void clearsTraceIdFromMdcAfterRequest() throws IOException, ServletException {
        when(request.getHeader("X-Trace-Id")).thenReturn("to-be-cleared");

        mdcFilter.doFilter(request, response, (req, res) -> {});

        assertNull(MDC.get("traceId"));
    }

    @Test
    void generatesNewTraceIdForEmptyHeader() throws IOException, ServletException {
        when(request.getHeader("X-Trace-Id")).thenReturn("");

        mdcFilter.doFilter(request, response, (req, res) -> {
            String traceId = MDC.get("traceId");
            assertNotNull(traceId);
            assertFalse(traceId.isEmpty());
        });
    }
}
