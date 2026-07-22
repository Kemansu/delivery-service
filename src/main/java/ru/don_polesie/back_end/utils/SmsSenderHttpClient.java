package ru.don_polesie.back_end.utils;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
public class SmsSenderHttpClient {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String token = dotenv.get("MTS_EXOLVE_API_TOKEN");
    private static final String senderNumber = dotenv.get("MTS_NUMBER");


    public static void sendSms(String recipientNumber, String text) {
        try {
            // Правильное экранирование всех специальных символов JSON
            String escapedText = text
                    .replace("\\", "\\\\")  // сначала экранируем обратные слеши
                    .replace("\"", "\\\"")  // затем кавычки
                    .replace("\b", "\\b")   // backspace
                    .replace("\f", "\\f")   // form feed
                    .replace("\n", "\\n")   // новая строка
                    .replace("\r", "\\r")   // carriage return
                    .replace("\t", "\\t");  // табуляция

            String jsonBody = String.format("""
                {
                "number": "%s",
                "destination": "%s",
                "text": "%s"
                }
                """, senderNumber, recipientNumber, escapedText);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            String authHeader = "Bearer " + token;

            // Текст НЕ логируем: в нём код входа. Номер маскируем.
            log.info("SMS request sending to {}", maskPhone(recipientNumber));


            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.exolve.ru/messaging/v1/SendSMS"))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Логируем только статус, не тело (в ответе бывает эхо текста/номера)
            log.info("SMS response status: {}", response.statusCode());

            if (response.statusCode() != 200) {
                throw new RuntimeException("SMS API returned error status " + response.statusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to send SMS", e);
        }
    }

    /** Маскирует номер для логов: 79991234567 → 7999***4567 */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) return "***";
        return phone.substring(0, 4) + "***" + phone.substring(phone.length() - 4);
    }
}
