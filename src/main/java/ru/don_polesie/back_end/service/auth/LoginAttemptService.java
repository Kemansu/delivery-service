package ru.don_polesie.back_end.service.auth;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Блокировка перебора пароля по конкретному аккаунту (в дополнение к лимиту по
 * IP в RateLimitFilter). После MAX_ATTEMPTS подряд неудачных входов логин
 * блокируется на LOCK_DURATION; удачный вход счётчик сбрасывает.
 *
 * Состояние в памяти процесса: бэкенд одиночный (одна реплика), при рестарте
 * счётчики обнуляются — для защиты от перебора этого достаточно.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private static final class Attempt {
        int count;
        Instant lockedUntil;
    }

    private final ConcurrentMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    private String key(String login) {
        return login == null ? "" : login.trim().toLowerCase();
    }

    /** Заблокирован ли сейчас вход для этого логина. */
    public boolean isBlocked(String login) {
        Attempt a = attempts.get(key(login));
        if (a == null || a.lockedUntil == null) {
            return false;
        }
        if (a.lockedUntil.isAfter(Instant.now())) {
            return true;
        }
        // Срок блокировки истёк — сбрасываем
        attempts.remove(key(login));
        return false;
    }

    /** Сколько секунд осталось до конца блокировки (0 — не заблокирован). */
    public long secondsUntilUnlock(String login) {
        Attempt a = attempts.get(key(login));
        if (a == null || a.lockedUntil == null) {
            return 0;
        }
        long secs = Duration.between(Instant.now(), a.lockedUntil).getSeconds();
        return Math.max(0, secs);
    }

    /** Удачный вход — сбрасываем счётчик. */
    public void loginSucceeded(String login) {
        attempts.remove(key(login));
    }

    /** Неудачный вход — инкремент, при достижении порога ставим блокировку. */
    public void loginFailed(String login) {
        attempts.compute(key(login), (k, a) -> {
            if (a == null) {
                a = new Attempt();
            }
            a.count++;
            if (a.count >= MAX_ATTEMPTS) {
                a.lockedUntil = Instant.now().plus(LOCK_DURATION);
                a.count = 0;
            }
            return a;
        });
    }
}
