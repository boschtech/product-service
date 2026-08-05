package com.boschtech.productservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
        filterChain = mock(FilterChain.class);
    }

    @Test
    void shouldAllowRequestWhenUnderLimit() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldReturn429WhenRateLimitExceeded() throws ServletException, IOException {
        // Exhaust all 100 tokens
        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilterInternal(req, res, filterChain);
        }

        // 101st request should be rate-limited
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(429, response.getStatus());
        assertEquals("60", response.getHeader("Retry-After"));
        assertTrue(response.getContentAsString().contains("Rate limit exceeded"));
    }

    @Test
    void shouldUseXForwardedForHeaderWhenPresent() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.50, 70.41.3.18");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldFallBackToRemoteAddrWhenXForwardedForIsBlank() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "   ");
        request.setRemoteAddr("192.168.1.2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRefillTokensAfterInterval() throws Exception {
        String clientIp = "10.0.0.99";

        // Exhaust all tokens
        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRemoteAddr(clientIp);
            filter.doFilterInternal(req, new MockHttpServletResponse(), filterChain);
        }

        // Verify exhausted
        MockHttpServletRequest blockedReq = new MockHttpServletRequest();
        blockedReq.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        filter.doFilterInternal(blockedReq, blockedRes, filterChain);
        assertEquals(429, blockedRes.getStatus());

        // Use reflection to simulate time passing on the bucket
        Field bucketsField = RateLimitFilter.class.getDeclaredField("buckets");
        bucketsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var buckets = (java.util.concurrent.ConcurrentHashMap<String, ?>) bucketsField.get(filter);
        Object bucket = buckets.get(clientIp);

        Field lastRefillField = bucket.getClass().getDeclaredField("lastRefillTime");
        lastRefillField.setAccessible(true);
        lastRefillField.set(bucket, System.currentTimeMillis() - 61_000);

        // Now request should succeed (tokens refilled)
        MockHttpServletRequest refreshedReq = new MockHttpServletRequest();
        refreshedReq.setRemoteAddr(clientIp);
        MockHttpServletResponse refreshedRes = new MockHttpServletResponse();
        filter.doFilterInternal(refreshedReq, refreshedRes, filterChain);
        assertEquals(200, refreshedRes.getStatus());
    }

    @Test
    void shouldDefaultToOneHundredRequestsPerMinute() {
        assertEquals(100, new RateLimitFilter().getRequestsPerMinute());
    }

    @Test
    void shouldEnforceCustomConfiguredLimit() throws ServletException, IOException {
        RateLimitFilter limitedFilter = new RateLimitFilter(3);
        String clientIp = "***********";

        assertEquals(3, limitedFilter.getRequestsPerMinute());

        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/products");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            limitedFilter.doFilterInternal(req, res, filterChain);
            assertEquals(200, res.getStatus(), "request " + (i + 1) + " should be allowed");
        }

        MockHttpServletRequest blocked = new MockHttpServletRequest("GET", "/api/products");
        blocked.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        limitedFilter.doFilterInternal(blocked, blockedRes, filterChain);

        assertEquals(429, blockedRes.getStatus());
        assertTrue(blockedRes.getContentAsString().contains("Rate limit exceeded"));
    }

    @Test
    void shouldNotRateLimitActuatorEndpoints() throws ServletException, IOException {
        RateLimitFilter limitedFilter = new RateLimitFilter(1);
        String clientIp = "***********";

        // Consume the single available token with an API request.
        MockHttpServletRequest apiRequest = new MockHttpServletRequest("GET", "/api/products");
        apiRequest.setRemoteAddr(clientIp);
        MockHttpServletResponse apiResponse = new MockHttpServletResponse();
        limitedFilter.doFilterInternal(apiRequest, apiResponse, filterChain);
        assertEquals(200, apiResponse.getStatus());

        // Health polling must never be throttled, even from an exhausted client IP.
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest healthRequest = new MockHttpServletRequest("GET", "/actuator/health");
            healthRequest.setRemoteAddr(clientIp);
            MockHttpServletResponse healthResponse = new MockHttpServletResponse();
            limitedFilter.doFilterInternal(healthRequest, healthResponse, filterChain);

            verify(filterChain).doFilter(healthRequest, healthResponse);
            assertEquals(200, healthResponse.getStatus());
        }

        // API traffic from the same IP is still limited.
        MockHttpServletRequest blocked = new MockHttpServletRequest("GET", "/api/products");
        blocked.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        limitedFilter.doFilterInternal(blocked, blockedRes, filterChain);
        assertEquals(429, blockedRes.getStatus());
    }

    @Test
    void shouldTrackDifferentClientsIndependently() throws ServletException, IOException {
        // Exhaust tokens for client A
        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRemoteAddr("10.0.0.1");
            filter.doFilterInternal(req, new MockHttpServletResponse(), filterChain);
        }

        // Client B should still be allowed
        MockHttpServletRequest requestB = new MockHttpServletRequest();
        requestB.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse responseB = new MockHttpServletResponse();
        filter.doFilterInternal(requestB, responseB, filterChain);

        assertEquals(200, responseB.getStatus());
    }
}
