package com.olabrows.checkout;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olabrows.order.model.Order;
import com.olabrows.order.repository.OrderRepository;
import com.olabrows.product.model.Product;
import com.olabrows.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String REF_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    public static final Pattern REFERENCE = Pattern.compile("^OLA-[A-Z0-9]{10}$");

    private final SecureRandom random = new SecureRandom();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ProductRepository products;
    private final OrderRepository orders;
    private final PaystackClient paystack;
    private final String publicUrl;

    public CheckoutService(ProductRepository products, OrderRepository orders, PaystackClient paystack,
                           @Value("${app.public.url}") String publicUrl) {
        this.products = products;
        this.orders = orders;
        this.paystack = paystack;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
    }

    public record Item(String slug, Integer qty) {}
    public record Customer(String name, String email, String phone, String address,
                           String city, String state, String country) {}
    public record CheckoutRequest(List<Item> items, Customer customer) {}

    /** Customer-facing problem, safe to show as-is. */
    public static class CheckoutException extends RuntimeException {
        public CheckoutException(String message) { super(message); }
    }

    /**
     * Creates a pending order using prices from the database (never from the browser)
     * and starts a Paystack payment for exactly that amount.
     */
    @Transactional
    public Map<String, Object> initialize(CheckoutRequest req) {
        if (req == null || req.items() == null || req.items().isEmpty()) throw new CheckoutException("Your cart is empty.");
        if (req.items().size() > 20) throw new CheckoutException("Your cart has too many items.");
        Customer c = req.customer();
        if (c == null) throw new CheckoutException("Please fill in your delivery details.");

        String name = clean(c.name(), 120);
        String email = clean(c.email(), 160).toLowerCase();
        String phone = clean(c.phone(), 30);
        String address = clean(c.address(), 200);
        String city = clean(c.city(), 80);
        String state = clean(c.state(), 80);
        String country = clean(c.country(), 60);
        if (name.isEmpty() || address.isEmpty() || city.isEmpty()) throw new CheckoutException("Please fill in your name and delivery address.");
        if (!EMAIL.matcher(email).matches()) throw new CheckoutException("Please enter a valid email address.");
        if (phone.isEmpty()) throw new CheckoutException("Please enter a phone number for delivery.");
        if (country.isEmpty()) country = "Nigeria";

        // Combine repeated lines, then look up each product's real price and stock.
        Map<String, Integer> qtyBySlug = new LinkedHashMap<>();
        for (Item i : req.items()) {
            if (i == null || i.slug() == null || i.qty() == null || i.qty() < 1 || i.qty() > 10)
                throw new CheckoutException("One of the items in your cart is not valid.");
            qtyBySlug.merge(i.slug().trim().toLowerCase(), i.qty(), Integer::sum);
        }

        BigDecimal total = BigDecimal.ZERO;
        int totalQty = 0;
        List<Map<String, Object>> lines = new ArrayList<>();
        List<String> summary = new ArrayList<>();
        for (Map.Entry<String, Integer> e : qtyBySlug.entrySet()) {
            Product p = products.findBySlug(e.getKey())
                .filter(x -> !Boolean.FALSE.equals(x.getActive()))
                .orElseThrow(() -> new CheckoutException("One of the items in your cart is no longer available."));
            int qty = e.getValue();
            if (qty > 10) throw new CheckoutException("You can order up to 10 of each product.");
            int stock = p.getStock() == null ? 0 : p.getStock();
            if (stock < qty) throw new CheckoutException(stock == 0
                ? p.getName() + " is sold out."
                : "Only " + stock + " left of " + p.getName() + ".");
            if (p.getPrice() == null || p.getPrice().signum() <= 0) throw new CheckoutException(p.getName() + " is not available right now.");

            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(qty)));
            totalQty += qty;
            String variant = p.getVariant() == null ? "" : p.getVariant();
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("slug", p.getSlug());
            line.put("name", p.getName());
            line.put("variant", variant);
            line.put("qty", qty);
            line.put("unitPrice", p.getPrice());
            lines.add(line);
            summary.add(p.getName() + (variant.isBlank() ? "" : " (" + variant + ")") + " x" + qty);
        }
        long amountKobo = toKobo(total);

        Order order = new Order();
        order.setReference(newReference());
        order.setCustomerName(name);
        order.setCustomerEmail(email);
        order.setCustomerPhone(phone);
        order.setShippingAddress(state.isEmpty() ? address : address + ", " + state);
        order.setCity(city);
        order.setCountry(country);
        order.setProductName(truncate(String.join(", ", summary), 250));
        order.setQuantity(totalQty);
        order.setTotalAmount(total);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setNotes("Awaiting payment");
        try {
            order.setItemsJson(mapper.writeValueAsString(lines));
        } catch (Exception e) {
            throw new IllegalStateException("Could not record order items", e);
        }
        order = orders.save(order);

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("amount", amountKobo);
        body.put("currency", "NGN");
        body.put("channels", List.of("card", "bank_transfer", "ussd", "bank"));
        body.put("channels", List.of("card", "bank_transfer", "ussd", "bank"));
        body.put("reference", order.getReference());
        body.put("callback_url", publicUrl + "/order-complete.html");
        body.put("metadata", Map.of("order_id", order.getId()));
        JsonNode data = paystack.initialize(body);

        return Map.of("authorizationUrl", data.path("authorization_url").asText(),
                      "reference", order.getReference());
    }

    /**
     * Checks with Paystack whether this order was paid, and marks it confirmed exactly once.
     * The order row is locked while checking, so the browser and the webhook can't both
     * confirm it and reduce stock twice.
     */
    @Transactional
    public Order confirm(String reference) {
        Order order = orders.findWithLockByReference(reference)
            .orElseThrow(() -> new CheckoutException("We could not find that order."));
        if (order.getStatus() != Order.OrderStatus.PENDING) return order;

        JsonNode data = paystack.verify(reference);
        String status = data.path("status").asText();
        if (!"success".equals(status)) {
            if ("failed".equals(status) || "abandoned".equals(status)) order.setNotes("Payment " + status);
            return orders.save(order);
        }
        long expected = toKobo(order.getTotalAmount());
        if (data.path("amount").asLong() != expected || !"NGN".equals(data.path("currency").asText())) {
            log.warn("Payment for {} did not match: expected {} kobo NGN, got {} {}", reference, expected,
                data.path("amount").asLong(), data.path("currency").asText());
            order.setNotes("PAYMENT MISMATCH - check Paystack before shipping");
            return orders.save(order);
        }

        order.setStatus(Order.OrderStatus.CONFIRMED);
        order.setPaidAt(LocalDateTime.now());
        order.setNotes("Paid via Paystack");
        reduceStock(order);
        log.info("Order {} paid", reference);
        return orders.save(order);
    }

    /** What the order-complete page may show: no email, phone or address. */
    public Map<String, Object> publicView(Order o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reference", o.getReference());
        m.put("status", o.getStatus().name());
        m.put("paid", o.getStatus() != Order.OrderStatus.PENDING && o.getStatus() != Order.OrderStatus.CANCELLED);
        m.put("total", o.getTotalAmount());
        m.put("items", o.getProductName());
        String name = o.getCustomerName() == null ? "" : o.getCustomerName().trim();
        m.put("firstName", name.isEmpty() ? "" : name.split("\\s+")[0]);
        return m;
    }

    private void reduceStock(Order order) {
        try {
            List<Map<String, Object>> lines = mapper.readValue(order.getItemsJson(), new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> line : lines) {
                String slug = String.valueOf(line.get("slug"));
                int qty = ((Number) line.get("qty")).intValue();
                products.findBySlug(slug).ifPresent(p -> {
                    int stock = p.getStock() == null ? 0 : p.getStock();
                    if (stock < qty) order.setNotes("Paid, but stock ran short for " + p.getName() + " - check before shipping");
                    p.setStock(Math.max(0, stock - qty));
                    products.save(p);
                });
            }
        } catch (Exception e) {
            log.error("Could not update stock for order {}", order.getReference(), e);
            order.setNotes("Paid, but stock was not updated automatically - adjust it in the admin panel");
        }
    }

    private String newReference() {
        StringBuilder sb = new StringBuilder("OLA-");
        for (int i = 0; i < 10; i++) sb.append(REF_CHARS.charAt(random.nextInt(REF_CHARS.length())));
        return sb.toString();
    }

    private static long toKobo(BigDecimal naira) {
        return naira.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private static String clean(String value, int max) {
        if (value == null) return "";
        String v = value.replaceAll("[\\p{Cntrl}]", " ").trim();
        return truncate(v, max);
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
