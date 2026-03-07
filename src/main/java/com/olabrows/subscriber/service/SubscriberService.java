package com.olabrows.subscriber.service;

import com.olabrows.subscriber.model.Subscriber;
import com.olabrows.subscriber.repository.SubscriberRepository;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import java.util.List;

@Service

public class SubscriberService {

    @Autowired
    private SubscriberRepository subscriberRepository;

    public List<Subscriber> getAllSubscribers() {
        return subscriberRepository.findByActiveTrue();
    }

    public Subscriber subscribe(Subscriber subscriber) {
        subscriberRepository.findByEmail(subscriber.getEmail())
                .ifPresent(s -> { throw new RuntimeException("Email already subscribed"); });
        return subscriberRepository.save(subscriber);
    }

    public void unsubscribe(String email) {
        Subscriber subscriber = subscriberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Subscriber not found"));
        subscriber.setActive(false);
        subscriberRepository.save(subscriber);
    }
}
