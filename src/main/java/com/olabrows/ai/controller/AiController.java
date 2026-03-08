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

    @Value("${gemini.api.key:NOT_SET}")
    private String geminiApiKey;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> request) {
        try {
            if (geminiApiKey.equals("NOT_SET") || geminiApiKey.isEmpty()) {
                return ResponseEntity.ok(Map.of("success", false, "message", "Gemini API key not configured"));
            }

            String message = (String) request.get("message");
            List<Map<String, Object>> history = (List<Map<String, Object>>) request.getOrDefault("history", new ArrayList<>());

            List<Map<String, Object>> contents = new ArrayList<>();
            for (Map<String, Object> h : history) {
                String role = (String) h.get("role");
                String content = (String) h.get("content");
                contents.add(Map.of(
                    "role", role.equals("assistant") ? "model" : "user",
                    "parts", List.of(Map.of("text", content))
                ));
            }
            contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", message))));

            Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text",
                    "You are Olá, a warm and knowledgeable personal brow advisor for Browed by Olá — a luxury brow beauty brand. " +
                    "You help customers find the perfect brow product for their skin tone, face shape, and occasion. " +
                    "Our products: Olá Brow Pencil Dark Brown $14, Olá Brow Pencil Soft Black $18, Brow Definer Duo Medium Brown $28, " +
                    "Precision Brow Gel Clear $16, Microblade Effect Pen Soft Black $22, Brow Care Oil 30ml $24. " +
                    "Always be warm, encouraging, and beauty-positive. Keep responses concise. " +
                    "End with a recommendation when possible. Use occasional emojis like 🌸 💕 ✨"
                ))
            );

            Map<String, Object> geminiRequest = new HashMap<>();
            geminiRequest.put("system_instruction", systemInstruction);
            geminiRequest.put("contents", contents);
            geminiRequest.put("generationConfig", Map.of("maxOutputTokens", 400, "temperature", 0.8));

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=" + geminiApiKey;
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(geminiRequest, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            Map responseBody = response.getBody();
            List candidates = (List) responseBody.get("candidates");
            Map candidate = (Map) candidates.get(0);
            Map content2 = (Map) candidate.get("content");
            List parts = (List) content2.get("parts");
            Map part = (Map) parts.get(0);
            String reply = (String) part.get("text");

            return ResponseEntity.ok(Map.of("success", true, "message", reply));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }
}
