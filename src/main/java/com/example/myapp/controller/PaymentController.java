package com.example.myapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import com.example.myapp.entity.Order;
import com.example.myapp.entity.OrderStatus;
import com.example.myapp.entity.Payment;
import com.example.myapp.entity.ShippingMethod;
import com.example.myapp.repository.OrderRepository;
import com.example.myapp.repository.PaymentRepository;
import com.example.myapp.repository.ProductRepository;
import com.example.myapp.repository.RefundRecordRepository;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.json.JSONObject;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentController(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    @Value("${paytabs.profile_id}")
    private String profileId;

    @Value("${paytabs.integration_key}")
    private String integrationKey;

    @Value("${paytabs.instance}")
    private String instance;

    @Value("${paytabs.api_url}")
    private String apiUrl;

    @RequestMapping("/initiatePayment")
    @ResponseBody
    public ResponseEntity<?> initiatePayment(
            @RequestParam("orderId") String orderId,
            @RequestParam("amount") String amount,
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("shippingMethod") String shippingMethod,
            @RequestParam("address") String address) {
        try {
            UUID orderUUID = UUID.fromString(orderId);
            Optional<Order> optionalOrder = orderRepository.findById(orderUUID);

            if (optionalOrder.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"Order not found\"}");
            }

            // Update order fields
            Order order = optionalOrder.get();
            order.setName(name);
            order.setEmail(email);
            order.setAddress(address);
            try {
                ShippingMethod method = ShippingMethod.valueOf(shippingMethod.toUpperCase());
                order.setShippingMethod(method);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\": \"Invalid shipping method\"}");
            }
            orderRepository.save(order);

            JSONObject body = createPaymentRequestBody(orderId, amount, name, email, address);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", integrationKey);

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject jsonResponse = new JSONObject(response.getBody());
                String hppUrl = jsonResponse.getString("redirect_url");
                return ResponseEntity.ok(hppUrl);
            } else {
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("error", "Payment initiation failed.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse.toString());
            }
        } catch (Exception e) {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("error", "Error occurred while initiating payment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse.toString());
        }
    }

    // redirect URL handle redirectParams returned from PayTabs API
    @RequestMapping("/redirect")
    public String paymentReturn(@RequestParam Map<String, String> redirectParams, Model model) {
        // Pass the payment response status from PayTabs to the view for conditional redirection
        model.addAttribute("responseStatus", redirectParams.get("respStatus"));
        return "payment_redirect";
    }

    // callback URL handle callbackParams returned from PayTabs API
    @RequestMapping("/callback")
    @ResponseBody
    public String paymentCallback(@RequestBody Map<String, Object> callbackParams) {
        System.out.println("START callback");
        callbackParams.forEach((key, value) -> System.out.println(key + ": " + value));
        System.out.println("END paymentCallback");

        try {
            String cartId = (String) callbackParams.get("cart_id");
            Map<String, Object> paymentInfo = (Map<String, Object>) callbackParams.get("payment_info");
            Map<String, Object> paymentResult = (Map<String, Object>) callbackParams.get("payment_result");

            if (cartId == null) {
                return "payment_error: Missing cart_id";
            }

            UUID orderId = UUID.fromString(cartId);

            Optional<Order> optionalOrder = orderRepository.findById(orderId);
            if (optionalOrder.isEmpty()) {
                System.out.println("paymentCallback error: Order not found");
                return "payment_error: Order not found";
            }

            Order order = optionalOrder.get();

            if (paymentResult != null && "A".equals(paymentResult.get("response_status"))) {
                // Update order status to COMPLETED
                order.setStatus(OrderStatus.COMPLETED);
                orderRepository.save(order);

                // Create Payment record
                Payment payment = new Payment();
                payment.setOrder(order);
                payment.setPaymentDate(LocalDate.now());
                payment.setPaymentMethod((String) paymentInfo.get("payment_method"));
                payment.setOrder(order);
                paymentRepository.save(payment);
                
                System.out.println("paymentCallback: payment_success");
                return "payment_success";
            }
            System.out.println("paymentCallback error: Payment not authorized");
            return "payment_error: Payment not authorized";

        } catch (Exception e) {
            System.out.println("paymentCallback error: " + e.getMessage());
            return "payment_error: " + e.getMessage();
        }
    }

    @RequestMapping("/success")
    public String paymentSuccess() {
        return "payment_success";
    }

    @RequestMapping("/failure")
    public String paymentCancel() {
        return "payment_failure";
    }

    private JSONObject createPaymentRequestBody(String orderId, String amount, String name, String email,
            String address) {
        JSONObject body = new JSONObject();
        body.put("profile_id", profileId);
        body.put("tran_type", "sale");
        body.put("tran_class", "ecom");
        body.put("cart_id", orderId);
        body.put("cart_description", "Order #" + orderId);
        body.put("cart_currency", "EGP");
        body.put("cart_amount", amount);
        body.put("hide_shipping", true);
        body.put("framed", true);
        // returnURL and callbackURL don't work with localhost server, need a deployed server
        // body.put("return", "http://localhost:8080/payment/redirect");
        // body.put("callback", "http://localhost:8080/payment/callback");
        body.put("return", "https://5d43-116-109-30-201.ngrok-free.app/payment/redirect");
        body.put("callback", "https://5d43-116-109-30-201.ngrok-free.app/payment/callback");

        // Set customer details ta avoid re-entering billing details
        JSONObject customerDetails = new JSONObject();
        customerDetails.put("name", name);
        customerDetails.put("email", email);
        customerDetails.put("street1", address);
        customerDetails.put("city", "dubai");
        customerDetails.put("state", "du");
        customerDetails.put("country", "AE");
        customerDetails.put("zip", "12345");
        body.put("customer_details", customerDetails);

        return body;
    }

}