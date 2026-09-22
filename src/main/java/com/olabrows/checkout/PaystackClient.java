package com.olabrows.checkout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;

/** Talks to Paystack. The secret key only ever lives here, loaded from server settings. */
@Component
public class PaystackClient {

    private static final String BASE = "https://api.paystack.co";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String secretKey;

    public PaystackClient(@Value("${paystack.secret.key}") String secretKey) {
        this.secretKey = secretKey;
    }

    /** Starts a payment and returns Paystack's "data" object (authorization_url, reference). */
    public JsonNode initialize(Map<String, Object> body) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "/transaction/initialize"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + secretKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
            return send(req);
        } catch (PaystackException e) {
            throw e;
        } catch (Exception e) {
            throw new PaystackException("Could not reach Paystack", e);
        }
    }

    /** Asks Paystack directly whether a payment really succeeded. */
    public JsonNode verify(String reference) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + "/transaction/verify/"
                    + URLEncoder.encode(reference, StandardCharsets.UTF_8)))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + secretKey)
                .GET()
                .build();
            return send(req);
        } catch (PaystackException e) {
            throw e;
        } catch (Exception e) {
            throw new PaystackException("Could not reach Paystack", e);
        }
    }

    private JsonNode send(HttpRequest req) throws Exception {
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode json = mapper.readTree(res.body());
        if (res.statusCode() >= 300 || !json.path("status").asBoolean(false)) {
            throw new PaystackException("Paystack returned " + res.statusCode() + ": " + json.path("message").asText(), null);
        }
        return json.path("data");
    }

    /** True only if the webhook body was signed by Paystack with our secret key. */
    public boolean isValidSignature(String rawBody, String signature) {
        if (rawBody == null || signature == null || signature.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            String expected = HexFormat.of().formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    public static class PaystackException extends RuntimeException {
        public PaystackException(String message, Throwable cause) { super(message, cause); }
    }
}
