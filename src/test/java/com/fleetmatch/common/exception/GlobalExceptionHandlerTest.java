package com.fleetmatch.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleOptimisticLockReturnsConflict() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/loads/123");

        ResponseEntity<ApiError> response = handler.handleOptimisticLock(
                new OptimisticLockingFailureException("Concurrent update"),
                request
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("CONCURRENT_UPDATE", response.getBody().getCode());
        assertEquals("/api/loads/123", response.getBody().getPath());
    }
}
