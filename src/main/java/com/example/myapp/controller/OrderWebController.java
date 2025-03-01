package com.example.myapp.controller;

import com.example.myapp.entity.Order;
import com.example.myapp.entity.OrderItem;
import com.example.myapp.entity.OrderStatus;
import com.example.myapp.entity.Product;
import com.example.myapp.entity.ShippingMethod;
import com.example.myapp.repository.OrderRepository;
import com.example.myapp.repository.ProductRepository;
import com.example.myapp.repository.RefundRecordRepository;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Controller
@RequestMapping("/orders")
public class OrderWebController {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final RefundRecordRepository refundRecordRepository;

    public OrderWebController(OrderRepository orderRepository, ProductRepository productRepository,
            RefundRecordRepository refundRecordRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.refundRecordRepository = refundRecordRepository;
    }

    @GetMapping
    public String listOrders(Model model) {
        List<Order> orders = orderRepository.findAll();
        model.addAttribute("orders", orders);
        return "orders";
    }

    @GetMapping("/checkout")
    public String showCheckoutPage(@RequestParam Map<String, String> params, Model model) {
        List<OrderItem> items = new ArrayList<>();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            UUID productId = UUID.fromString(entry.getKey());
            int quantity = Integer.parseInt(entry.getValue());

            productRepository.findById(productId).ifPresent(product -> {
                OrderItem item = new OrderItem();
                item.setProduct(product);
                item.setQuantity(quantity);
                item.setPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
                items.add(item);
            });
        }

        model.addAttribute("items", items);
        model.addAttribute("shippingMethods", ShippingMethod.values());
        return "checkout";
    }

    @PostMapping("/checkout")
    @Transactional
    public String processCheckout(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String address,
            @RequestParam ShippingMethod shippingMethod,
            @RequestParam Map<String, String> parameters) {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);
        order.setShippingMethod(shippingMethod);

        AtomicReference<BigDecimal> totalAmount = new AtomicReference<>(BigDecimal.ZERO);
        Set<OrderItem> items = new HashSet<>();

        // Filter out non-UUID parameters (e.g., name, email, address ...)
        parameters.entrySet().stream()
                .filter(entry -> entry.getKey().matches("^[0-9a-fA-F-]{36}$")) // Match valid UUID format
                .forEach(entry -> {
                    UUID productId = UUID.fromString(entry.getKey());
                    int quantity = Integer.parseInt(entry.getValue());

                    productRepository.findById(productId).ifPresent(product -> {
                        OrderItem item = new OrderItem();
                        item.setOrder(order);
                        item.setProduct(product);
                        item.setQuantity(quantity);
                        item.setPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
                        items.add(item);
                        totalAmount.updateAndGet(current -> current.add(item.getPrice()));
                    });
                });
                
        order.setItems(items);
        order.setTotalAmount(totalAmount.get());
        orderRepository.save(order);

        return "redirect:/orders/" + order.getId();
    }

    @GetMapping("/{id}")
    public String orderDetails(@PathVariable UUID id, Model model) {
        return orderRepository.findById(id)
                .map(order -> {
                    model.addAttribute("order", order);
                    model.addAttribute("refunds", refundRecordRepository.findByOrderId(order.getId()));
                    return "order-details";
                })
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));
    }
}
