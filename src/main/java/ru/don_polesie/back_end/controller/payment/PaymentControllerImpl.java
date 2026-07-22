package ru.don_polesie.back_end.controller.payment;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.don_polesie.back_end.service.system.YooKassaService;

import java.util.Map;
import java.util.Set;


@RestController
@RequiredArgsConstructor
@Slf4j
public class PaymentControllerImpl implements PaymentController {

    private final YooKassaService yooKassaServiceImpl;

    /** События, которые мы обрабатываем; прочие (refund.* и т.п.) подтверждаем без обработки,
     *  иначе ЮKassa будет ретраить их сутками. */
    private static final Set<String> HANDLED_EVENTS = Set.of(
            "payment.waiting_for_capture", "payment.succeeded", "payment.canceled");

    @Override
    public ResponseEntity<?> notifications(
            @RequestBody JsonNode body,
            HttpServletRequest servletRequest) {
        String remoteIp = extractClientIp(servletRequest);

        String event = body.has("event") ? body.get("event").asText() : null;
        if (event != null && !HANDLED_EVENTS.contains(event)) {
            log.info("Уведомление ЮKassa {} не обрабатывается, подтверждаем: ip={}", event, remoteIp);
            return ResponseEntity.ok(Map.of("status", "ignored"));
        }

        if (!yooKassaServiceImpl.verifyNotification(remoteIp, body)) {
            log.warn("Отказано в обработке уведомления: ip={} body={}", remoteIp, body);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid notification"));
        }

        yooKassaServiceImpl.storeNotification(body);
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    @Override
    public ResponseEntity<?> getPayment(Long id) throws Exception {
        JsonNode payment = yooKassaServiceImpl.getPayment(id);
        return ResponseEntity.ok(payment);
    }

    private String extractClientIp(HttpServletRequest request) {
        // X-Real-IP ставит nginx ($remote_addr) и перезаписывает клиентский —
        // подделать нельзя. Клиентский X-Forwarded-For (первый элемент) спуфится
        // и обходил бы IP-whitelist вебхука ЮKassa, поэтому его не берём.
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
