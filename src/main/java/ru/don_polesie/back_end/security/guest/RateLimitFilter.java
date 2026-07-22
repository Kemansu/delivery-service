package ru.don_polesie.back_end.security.guest;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Реальный IP клиента. Запросы приходят через nginx → Next-прокси, поэтому
     * getRemoteAddr() = адрес Next (127.0.0.1), общий для всех. nginx кладёт
     * настоящий адрес в X-Real-IP (proxy_set_header X-Real-IP $remote_addr) —
     * его нельзя подделать с клиента, nginx перезаписывает заголовок. На случай
     * его отсутствия — последний адрес X-Forwarded-For (добавлен nginx),
     * затем remoteAddr.
     */
    private String clientIp(HttpServletRequest req) {
        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            return parts[parts.length - 1].trim();
        }
        return req.getRemoteAddr();
    }

    /**
     * Вход и обновление токенов — самые лакомые для перебора эндпоинты, поэтому
     * лимит на них жёстче: 10 запросов в минуту на IP (перебор пароля упирается
     * в лимит; аккаунт дополнительно блокирует LoginAttemptService). Остальные
     * пути — прежние 60/мин на IP+URL+метод.
     */
    private Bucket resolveBucket(HttpServletRequest req) {
        String ip = clientIp(req);
        String uri = req.getRequestURI();

        if (uri.startsWith("/auth/")) {
            return buckets.computeIfAbsent("auth:" + ip, k -> Bucket.builder()
                    .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofMinutes(1)))
                    .build());
        }

        String key = ip + ":" + uri + ":" + req.getMethod();
        return buckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(limit -> limit.capacity(60).refillGreedy(60, Duration.ofMinutes(1)))
                .build());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (resolveBucket(req).tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"Too Many Requests\"}");
        }
    }
}
