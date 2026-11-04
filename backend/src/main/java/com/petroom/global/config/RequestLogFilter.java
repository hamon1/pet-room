package com.petroom.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청마다 ID를 붙여 로그(MDC)와 응답 헤더에 남기고, 처리 결과를 한 줄로 기록합니다.
 * 프록시가 보낸 X-Request-Id가 안전한 형식이면 이어서 씁니다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLogFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");
    private static final Logger log = LoggerFactory.getLogger("http");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String id = incoming != null && SAFE_ID.matcher(incoming).matches()
                ? incoming
                : UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put(MDC_KEY, id);
        response.setHeader(HEADER, id);
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            if (!request.getRequestURI().startsWith("/actuator/health")) {
                long ms = (System.nanoTime() - start) / 1_000_000;
                // 경로만 남기고 쿼리 문자열은 남기지 않습니다 (개인정보가 섞일 수 있음).
                log.info("{} {} {} {}ms", request.getMethod(), request.getRequestURI(), response.getStatus(), ms);
            }
            MDC.remove(MDC_KEY);
        }
    }
}
