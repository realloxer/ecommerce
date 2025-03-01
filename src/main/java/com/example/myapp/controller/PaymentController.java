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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.Map;

import org.json.JSONObject;

@Controller
@RequestMapping("/payment")
public class PaymentController {

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
            @RequestParam("address") String address) {
        try {
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

    private JSONObject createPaymentRequestBody(String orderId, String amount, String name, String email,
            String address) {
        JSONObject body = new JSONObject();
        body.put("profile_id", profileId);
        body.put("tran_type", "sale");
        body.put("tran_class", "ecom");
        body.put("cart_id", "CART#" + orderId);
        body.put("cart_description", "Order #" + orderId);
        body.put("cart_currency", "EGP");
        body.put("cart_amount", amount);
        body.put("hide_shipping", true);
        body.put("framed", true);
        // Return Url does not work with localhost http server, need a deployed server
        // with https
        body.put("return", "https://5d43-116-109-30-201.ngrok-free.app/payment/return");

        // Set customer details ta avoid re-entering billing details
        JSONObject customerDetails = new JSONObject();
        customerDetails.put("name", name);
        customerDetails.put("email", email);
        customerDetails.put("street1", address);
        customerDetails.put("city", "cairo");
        customerDetails.put("state", "mamluk");
        customerDetails.put("country", "Egypt");
        customerDetails.put("zip", "12345");
        body.put("customer_details", customerDetails);

        return body;
    }

}