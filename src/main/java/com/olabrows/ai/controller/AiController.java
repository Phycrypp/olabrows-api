package com.olabrows.ai.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiController {

    @Value("${anthropic.api.key:NOT_SET}")
    private String anthropicApiKey;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> request) {
        try {
            String message = (String) request.get("message");
            List<Map<String, Object>> history = (List<Map<String, Object>>) request.getOrDefault("history", new ArrayList<>());

            List<Map<String, Object>> messages = new ArrayList<>();
            for (Map<String, Object> h : history) {
                String role = (String) h.get("role");
                String content = (String) h.get("content");
                messages.add(Map.of(
                    "role", role.equals("assistant") ? "assistant" : "user",
                    "content", content
                ));
            }
            messages.add(Map.of("role", "user", "content", message));

            Map<String, Object> claudeRequest = new HashMap<>();
            claudeRequest.put("model", "claude-sonnet-4-5");
            claudeRequest.put("max_tokens", 400);
            claudeRequest.put("system",
                "You are Olá, a warm and knowledgeable personal brow advisor for Browed by Olá — a luxury brow beauty brand. " +
                "You help customers find the perfect brow product for their skin tone, face shape, and occasion. " +
                "Our products: Olá Brow Pencil Dark Brown $14, Olá Brow Pencil Soft Black $18, Brow Definer Duo Medium Brown $28, " +
                "Precision Brow Gel Clear $16, Microblade Effect Pen Soft Black $22, Brow Care Oil 30ml $24. " +
                "Always be warm, encouraging, and beauty-positive. Keep responses concise. " +
                "End with a recommendation when possible. Use occasional emojis like 🌸 💕 ✨"
            );
            claudeRequest.put("messages", messages);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", anthropicApiKey);
            headers.set("anthropic-version", "2023-06-01");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(claudeRequest, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.anthropic.com/v1/messages", entity, Map.class
            );

            Map responseBody = response.getBody();
            List content2 = (List) responseBody.get("content");
            Map firstContent = (Map) content2.get(0);
            String reply = (String) firstContent.get("text");

            return ResponseEntity.ok(Map.of("success", true, "message", reply));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }
}
