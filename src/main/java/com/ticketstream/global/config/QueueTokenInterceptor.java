package com.ticketstream.global.config;

import com.ticketstream.domain.queue.QueueService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueTokenInterceptor implements HandlerInterceptor {

    private final QueueService queueService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Preflight OPTIONS 요청은 검증 없이 통과 (CORS 허용)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("X-Queue-Token");
        if (token == null || token.isBlank()) {
            token = request.getParameter("token");
        }

        log.debug("[QueueTokenInterceptor] URI: {}, Method: {}, Token: {}",
                request.getRequestURI(), request.getMethod(), token);

        queueService.validateActiveToken(token);
        return true;
    }
}
