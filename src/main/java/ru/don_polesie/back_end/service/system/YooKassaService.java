package ru.don_polesie.back_end.service.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.don_polesie.back_end.model.enums.OrderStatus;
import ru.don_polesie.back_end.exceptions.ObjectNotFoundException;
import ru.don_polesie.back_end.model.order.Order;
import ru.don_polesie.back_end.repository.OrderRepository;
import ru.don_polesie.back_end.utils.CidrUtils;


import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;



@Service
@Log4j2
public class YooKassaService {

    @Value("${security.YooKassa.shopId}")
    private String shopId;

    @Value("${security.YooKassa.secretKey}")
    private String secretKey;

    @Value("${yookassa.return-url}")
    private String returnUrl;

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final List<CidrUtils> allowedCidrs;
    private final OrderRepository orderRepository;

    @Autowired
    public YooKassaService(OrderRepository orderRepository) throws Exception {
        this.orderRepository = orderRepository;
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();

        // Инициализация whitelist IP
        String[] cidrs = new String[]{
                "185.71.76.0/27",
                "185.71.77.0/27",
                "77.75.153.0/25",
                "77.75.156.11/32",
                "77.75.156.35/32",
                "77.75.154.128/25",
                "2a02:5180::/32"
        };
        List<CidrUtils> list = new ArrayList<>();
        for (String cidr : cidrs) {
            list.add(new CidrUtils(cidr));
        }
        this.allowedCidrs = Collections.unmodifiableList(list);
    }

    @Transactional
    public JsonNode createPayment(Long orderId) throws Exception {
        var order = orderRepository
                .findById(orderId)
                .orElseThrow(() -> new ObjectNotFoundException(""));

        var payload = mapper.createObjectNode();
        var amount = mapper.createObjectNode();
        amount.put("value", order.getTotalAmount());
        amount.put("currency", "RUB");
        payload.set("amount", amount);

        var paymentMethodData = mapper.createObjectNode();
        paymentMethodData.put("type", "bank_card");
        payload.set("payment_method_data", paymentMethodData);

        payload.put("capture", false);

        var confirmation = mapper.createObjectNode();
        confirmation.put("type", "redirect");
        confirmation.put("return_url", returnUrl);
        payload.set("confirmation", confirmation);

        var metadata = mapper.createObjectNode();
        metadata.put("orderId", orderId);
        metadata.put("userId", 0);
        payload.set("metadata", metadata);

        String body = mapper.writeValueAsString(payload);

        String credentials = shopId + ":" + secretKey;
        String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.yookassa.ru/v3/payments"))
                .header("Authorization", "Basic " + basicAuth)
                .header("Idempotence-Key", UUID.randomUUID().toString())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());


        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode json = mapper.readTree(response.body());
            String paymentId = json.get("id").asText();
            order.setStatus(OrderStatus.PAYING);
            order.setPaymentId(paymentId);
            orderRepository.save(order);
            return json;
        } else {
            throw new RuntimeException("YooKassa createPayment error: " + response.statusCode() + " " + response.body());
        }
    }

    public JsonNode getPayment(Long orderId) throws Exception {
        // 1. Достаем заказ и его paymentId
        Order order = orderRepository.findById(Long.valueOf(orderId))
                .orElseThrow(() -> new ObjectNotFoundException("Order not found with id: " + orderId));

        String paymentId = order.getPaymentId();
        if (paymentId == null || paymentId.isBlank()) {
            throw new RuntimeException("PaymentId not found for order " + orderId);
        }

        // 2. Авторизация
        String credentials = shopId + ":" + secretKey;
        String basicAuth = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        // 3. GET /v3/payments/{paymentId}
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.yookassa.ru/v3/payments/" + paymentId))
                .header("Authorization", "Basic " + basicAuth)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode json = mapper.readTree(response.body());

            // При желании можно обновить статус заказа
            String status = json.get("status").asText();
            if ("succeeded".equals(status)) {
                order.setStatus(OrderStatus.PAID);
                orderRepository.save(order);
                log.info("Money for order {} successfully got", orderId);
            } else if ("waiting_for_capture".equals(status)){
                order.setStatus(OrderStatus.MONEY_RESERVAITED);
                orderRepository.save(order);
                log.info("User paid order {}", orderId);
            } else if ("canceled".equals(status)) {
                order.setStatus(OrderStatus.CANCELED);
                orderRepository.save(order);
                log.info("Something went wrong with order {}", orderId);
            }

            return json;
        } else {
            log.warn("YooKassa getPayment error: {}", response.statusCode() + " " + response.body());
            throw new RuntimeException(
                    "YooKassa getPayment error: " + response.statusCode() + " " + response.body()
            );
        }
    }

    @Transactional
    public void storeNotification(JsonNode notification) {
        var orderId = extractOrderId(notification);
        var order = orderRepository
                .findById(orderId)
                .orElseThrow(() -> new ObjectNotFoundException(""));

        String event = notification.get("event").asText();
        OrderStatus status = order.getStatus();

        // Остатки товара здесь не трогаем: источник истины по остаткам — 1С
        // (ProductImportJob перезаписывает amount), а quantity весовых — в граммах.
        switch (event) {
            case "payment.waiting_for_capture" -> {
                if (status == OrderStatus.PAYING) {
                    order.setStatus(OrderStatus.MONEY_RESERVAITED);
                }
            }
            case "payment.succeeded" -> {
                // капчер уже переводит заказ в READY_FOR_DELIVERY — не откатываем статус назад
                if (status == OrderStatus.PAYING || status == OrderStatus.MONEY_RESERVAITED) {
                    order.setStatus(OrderStatus.PAID);
                }
            }
            case "payment.canceled" -> {
                // отмена/истёкший холд актуальны, только пока деньги не списаны
                if (status == OrderStatus.PAYING || status == OrderStatus.MONEY_RESERVAITED) {
                    order.setStatus(OrderStatus.CANCELED);
                }
            }
            default -> log.info("Уведомление ЮKassa с событием {} по заказу {} проигнорировано", event, orderId);
        }
    }

    /**
     * Проверка нотификации по документации:
     * 1. IP-адрес в whitelist.
     * 2. Тело — тип notification, есть поле event и object.
     * 3. Статус объекта актуален (сравнение с повторным запросом).
     */

    public boolean verifyNotification(String remoteIp, JsonNode notification) {
        try {
            // 1. IP
            boolean ipAllowed = allowedCidrs.stream().anyMatch(c -> c.contains(remoteIp));
            if (!ipAllowed) {
                return false;
            }

            // 2. Структура
            if (!notification.has("type") || !notification.get("type").asText().equals("notification")) {
                return false;
            }
            if (!notification.has("event") || !notification.has("object")) {
                return false;
            }

            String event = notification.get("event").asText(); // e.g., payment.succeeded
            JsonNode object = notification.get("object");
            if (!object.has("id") || !object.has("status")) {
                return false;
            }
            String objectId = object.get("id").asText();
            String notifiedStatus = object.get("status").asText();

            // 3. Заказ — из metadata.orderId; object.id (id платежа ЮKassa)
            //    обязан совпадать с paymentId заказа
            Long orderId = extractOrderId(notification);
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null || !objectId.equals(order.getPaymentId())) {
                return false;
            }

            // 4. Сверяем заявленный статус со свежим из API ЮKassa
            JsonNode current = getPayment(orderId);
            if (current == null || !current.has("status")) {
                return false;
            }
            String currentStatus = current.get("status").asText();

            // Логика согласованности: для payment.waiting_for_capture допустимо, если текущий уже succeeded
            if (event.equals("payment.waiting_for_capture")) {
                if (!currentStatus.equals("waiting_for_capture") && !currentStatus.equals("succeeded")) {
                    return false;
                }
            } else {
                // для остальных — точное совпадение
                if (!currentStatus.equals(notifiedStatus)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            // при ошибке — отвергаем
            return false;
        }
    }

    private Long extractOrderId(JsonNode notification) {
        JsonNode node = notification.at("/object/metadata/orderId");
        if (node.isMissingNode() || node.asText().isBlank()) throw new ObjectNotFoundException("");
        try {
            return Long.parseLong(node.asText());
        } catch (NumberFormatException e) {
            throw new ObjectNotFoundException("");
        }
    }

    public void getMoney(Order order) throws InterruptedException, IOException {
        String paymentId = order.getPaymentId();
        String url = "https://api.yookassa.ru/v3/payments/" + paymentId + "/capture";

        // Формируем JSON-тело запроса
        String json = String.format(Locale.US,
                "{ \"amount\": { \"value\": \"%.2f\", \"currency\": \"RUB\" } }",
                order.getTotalAmount()
        );

        // Idempotence-Key (лучше уникальный для каждого запроса)
        String idempotenceKey = java.util.UUID.randomUUID().toString();

        // Basic Auth
        String auth = Base64.getEncoder().encodeToString((shopId + ":" + secretKey).getBytes());

        log.debug("Capture payment for order {}: url={}, idempotenceKey={}", order.getId(), url, idempotenceKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Basic " + auth)
                .header("Idempotence-Key", idempotenceKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            log.info("Оплата по заказу {} успешно захвачена", order.getId());
        } else {
            log.error("Ошибка при захвате оплаты по заказу {}: {} - {}", order.getId(), response.statusCode(), response.body());
            throw new RuntimeException("Ошибка при захвате оплаты: " + response.statusCode() + " - " + response.body());
        }
    }
}
