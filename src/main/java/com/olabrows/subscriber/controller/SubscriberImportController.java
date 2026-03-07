package com.olabrows.subscriber.controller;

import com.olabrows.subscriber.model.Subscriber;
import com.olabrows.subscriber.repository.SubscriberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscribers")
@CrossOrigin(origins = "*")
public class SubscriberImportController {

    @Autowired
    private SubscriberRepository subscriberRepository;

    @PostMapping("/import")
    public ResponseEntity<?> importSubscribers(@RequestParam("file") MultipartFile file) {
        List<String> imported = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 0) continue;

                String email = parts[0].trim().toLowerCase();
                String name = parts.length > 1 ? parts[1].trim() : "";

                if (email.isEmpty() || !email.contains("@")) continue;

                if (subscriberRepository.findByEmail(email).isPresent()) {
                    skipped.add(email);
                    continue;
                }

                Subscriber s = new Subscriber();
                s.setEmail(email);
                s.setName(name);
                s.setActive(true);
                
                subscriberRepository.save(s);
                imported.add(email);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

        return ResponseEntity.ok(Map.of(
            "imported", imported.size(),
            "skipped", skipped.size(),
            "emails", imported
        ));
    }
}
