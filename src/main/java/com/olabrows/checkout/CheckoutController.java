package com.olabrows.checkout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);
    private final CheckoutService checkout;
    private final PaystackClient paystack;
    private final ObjectMapper mapper = new ObjectMapper();

    public CheckoutController(CheckoutService checkout, PaystackClient paystack) {
        this.checkout = checkout;
        this.paystack = paystack;
    }

    /** Public: the browser sends product Web IDs, quantities and delivery details. */
    @PostMapping("/api/checkout/initialize")
    public ResponseEntity<Map<String, Object>> initialize(@RequestBody CheckoutService.CheckoutRequest req) {
        try {
            return ResponseEntity.ok(checkout.initialize(req));
        } catch (CheckoutService.CheckoutException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Checkout could not start", e);
            return ResponseEntity.status(502).body(Map.of("error", "We could not start the payment. Please try again."));
        }
    }

    /** Public: the order-complete page asks whether its order was paid. */
    @GetMapping("/api/checkout/verify/{reference}")
    public ResponseEntity<Map<String, Object>> verify(@PathVariable String reference) {
        if (!CheckoutService.REFERENCE.matcher(reference).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "That order reference is not valid."));
        }
        try {
            return ResponseEntity.ok(checkout.publicView(checkout.confirm(reference)));
        } catch (CheckoutService.CheckoutException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Could not verify {}", reference, e);
            return ResponseEntity.status(502).body(Map.of("error", "We could not check your payment yet. Refresh in a moment."));
        }
    }

    /** Public, but only accepts messages signed by Paystack with our secret key. */
    @PostMapping("/api/payment/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String rawBody,
                                        @RequestHeader(value = "x-paystack-signature", required = false) String signature) {
        if (!paystack.isValidSignature(rawBody, signature)) {
            log.warn("Rejected webhook with an invalid signature");
            return ResponseEntity.status(401).build();
        }
        try {
            JsonNode event = mapper.readTree(rawBody);
            if ("charge.success".equals(event.path("event").asText())) {
                String reference = event.path("data").path("reference").asText("");
                if (CheckoutService.REFERENCE.matcher(reference).matches()) checkout.confirm(reference);
            }
        } catch (Exception e) {
            log.error("Webhook processing failed", e);
        }
        return ResponseEntity.ok().build();
    }
}
