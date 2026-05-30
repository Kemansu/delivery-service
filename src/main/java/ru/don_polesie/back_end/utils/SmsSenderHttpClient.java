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

            log.info("SMS request sending - PhoneNumber: {}, Text: {}",
                    recipientNumber, text);


            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.exolve.ru/messaging/v1/SendSMS"))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("SMS response body - Body: {}",
                    response.body());

            if (response.statusCode() != 200) {
                throw new RuntimeException("API returned error: " + response.body());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to send SMS", e);
        }
    }

}
