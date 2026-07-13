package com.secretshare.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitInterceptorTest {

    @Test
    void ignoresForwardedHeaderUnlessProxyTrustIsEnabled() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(false, 100);

        for (int requestNumber = 0; requestNumber < 30; requestNumber++) {
            assertThat(view(interceptor, "192.0.2.1", "198.51.100." + requestNumber).allowed())
                    .isTrue();
        }

        Result limited = view(interceptor, "192.0.2.1", "203.0.113.10");
        assertThat(limited.allowed()).isFalse();
        assertThat(limited.status()).isEqualTo(429);
    }

    @Test
    void usesForwardedClientIpWhenProxyTrustIsEnabled() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(true, 100);

        for (int requestNumber = 0; requestNumber < 31; requestNumber++) {
            assertThat(view(interceptor, "192.0.2.1", "198.51.100." + requestNumber).allowed())
                    .isTrue();
        }
    }

    @Test
    void rejectsNewClientsWhenTrackingCapacityIsReached() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(false, 2);

        assertThat(view(interceptor, "192.0.2.1", null).allowed()).isTrue();
        assertThat(view(interceptor, "192.0.2.2", null).allowed()).isTrue();

        Result limited = view(interceptor, "192.0.2.3", null);
        assertThat(limited.allowed()).isFalse();
        assertThat(limited.status()).isEqualTo(429);
    }

    private static Result view(
            RateLimitInterceptor interceptor,
            String remoteAddress,
            String forwardedFor) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/secrets/token");
        request.setRemoteAddr(remoteAddress);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();

        return new Result(interceptor.preHandle(request, response, new Object()), response.getStatus());
    }

    private record Result(boolean allowed, int status) {}
}
